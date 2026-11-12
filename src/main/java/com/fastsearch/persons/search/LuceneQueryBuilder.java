package com.fastsearch.persons.search;

import com.fastsearch.persons.analysis.NgramAnalyzer;
import com.fastsearch.persons.dates.DatePrecision;
import com.fastsearch.persons.dates.ParsedDate;
import com.fastsearch.persons.names.NameHypothesis;
import com.fastsearch.persons.names.NameNormalizer;
import com.fastsearch.persons.names.NameVariantGenerator;
import com.fastsearch.persons.names.PhoneticKey;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Construit une requête de candidats. Les poids décroissants sont :
 * exact > variantes/translittéré > phonétique fine > phonétique grossière >
 * flou sur clé > trigrammes. La clé grossière est un simple rappel, jamais
 * une preuve : le reranker plafonne la confiance.
 */
public final class LuceneQueryBuilder {

    private final NameVariantGenerator variants;
    private final boolean enableCoarse;
    private final NgramAnalyzer ngramAnalyzer = new NgramAnalyzer();

    public LuceneQueryBuilder(NameVariantGenerator variants, boolean enableCoarse) {
        this.variants = variants;
        this.enableCoarse = enableCoarse;
    }

    public Query build(ParsedSearchQuery parsed) {
        if (parsed == null || !parsed.valid()) {
            return null;
        }
        if (parsed.ownerGroups().isEmpty()) {
            return new MatchNoDocsQuery("aucun groupe autorisé");
        }
        BooleanQuery.Builder root = new BooleanQuery.Builder();
        boolean any = false;

        if (parsed.hasNames()) {
            BooleanQuery.Builder names = new BooleanQuery.Builder();
            for (NameHypothesis hypothesis : parsed.names()) {
                Query nameQuery = nameQuery(hypothesis);
                if (nameQuery != null) {
                    names.add(new BoostQuery(nameQuery, hypothesis.inverted() ? 0.95f : 1.0f),
                            BooleanClause.Occur.SHOULD);
                    any = true;
                }
            }
            if (any) {
                names.setMinimumNumberShouldMatch(1);
                root.add(names.build(), BooleanClause.Occur.SHOULD);
            }
        }
        if (parsed.hasDates()) {
            for (ParsedDate date : parsed.dates()) {
                root.add(dateQuery(date), BooleanClause.Occur.SHOULD);
                any = true;
            }
        }
        if (parsed.hasId()) {
            root.add(new BoostQuery(new TermQuery(new Term(IndexFields.SOURCE_ID, parsed.idQuery())), 12.0f),
                    BooleanClause.Occur.SHOULD);
            any = true;
        }
        if (!parsed.birthPlace().isBlank()) {
            root.add(new BoostQuery(new TermQuery(new Term(IndexFields.BIRTH_PLACE_EXACT,
                    NameNormalizer.normalizeToken(parsed.birthPlace()))), 0.6f), BooleanClause.Occur.SHOULD);
        }
        if (!any) {
            return null;
        }
        root.setMinimumNumberShouldMatch(1);
        BooleanQuery candidateQuery = root.build();

        BooleanQuery.Builder secured = new BooleanQuery.Builder();
        secured.add(candidateQuery, BooleanClause.Occur.MUST);
        Set<String> groups = parsed.ownerGroups();
        secured.add(new TermInSetQuery(IndexFields.OWNER_GROUP, bytesTerms(groups)),
                BooleanClause.Occur.FILTER);
        return secured.build();
    }

    private Query nameQuery(NameHypothesis hypothesis) {
        String fullName = hypothesis.fullName();
        List<String> tokens = NameNormalizer.tokensForKeys(fullName);
        if (tokens.isEmpty()) {
            return null;
        }
        BooleanQuery.Builder allTokens = new BooleanQuery.Builder();
        BooleanQuery.Builder anyToken = new BooleanQuery.Builder();
        for (String token : tokens) {
            Query tokenQuery = tokenQuery(token);
            allTokens.add(tokenQuery, BooleanClause.Occur.MUST);
            anyToken.add(tokenQuery, BooleanClause.Occur.SHOULD);
        }
        anyToken.setMinimumNumberShouldMatch(1);
        BooleanQuery all = allTokens.build();
        BooleanQuery any = anyToken.build();

        BooleanQuery.Builder name = new BooleanQuery.Builder();
        name.add(new BoostQuery(all, 6.0f), BooleanClause.Occur.SHOULD);
        name.add(new BoostQuery(any, 1.0f), BooleanClause.Occur.SHOULD);
        Query exact = exactQuery(hypothesis);
        if (exact != null) {
            name.add(new BoostQuery(exact, 8.0f), BooleanClause.Occur.SHOULD);
        }
        Set<String> fullVariants = variants.variantsForName(fullName);
        if (!fullVariants.isEmpty()) {
            name.add(new BoostQuery(new TermInSetQuery(IndexFields.NAME_VARIANTS,
                    bytesTerms(fullVariants)), 7.0f), BooleanClause.Occur.SHOULD);
        }
        Query ngram = ngramQuery(NameNormalizer.normalizedName(fullName));
        if (ngram != null) {
            name.add(new BoostQuery(ngram, 0.20f), BooleanClause.Occur.SHOULD);
        }
        name.setMinimumNumberShouldMatch(1);
        return name.build();
    }

    private Query tokenQuery(String token) {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        Set<String> tokenVariants = variants.variantsForToken(token);
        if (!tokenVariants.isEmpty()) {
            query.add(new BoostQuery(new TermInSetQuery(IndexFields.NAME_VARIANTS,
                    bytesTerms(tokenVariants)), 6.0f), BooleanClause.Occur.SHOULD);
        }
        PhoneticKey key = PhoneticKey.of(token);
        if (!key.fine().isBlank()) {
            query.add(new BoostQuery(new TermQuery(
                    new Term(IndexFields.NAME_PHONETIC_FINE, key.fine())), 4.0f),
                    BooleanClause.Occur.SHOULD);
            if (key.fine().length() >= 3) {
                query.add(new BoostQuery(new FuzzyQuery(
                        new Term(IndexFields.NAME_PHONETIC_FINE, key.fine()), 1, 0), 1.4f),
                        BooleanClause.Occur.SHOULD);
            }
        }
        if (enableCoarse && !key.coarse().isBlank()) {
            query.add(new BoostQuery(new TermQuery(
                    new Term(IndexFields.NAME_PHONETIC_COARSE, key.coarse())), 0.9f),
                    BooleanClause.Occur.SHOULD);
        }
        query.setMinimumNumberShouldMatch(1);
        return query.build();
    }

    private Query exactQuery(NameHypothesis hypothesis) {
        Set<String> values = new LinkedHashSet<>();
        String full = hypothesis.fullName();
        values.add(NameNormalizer.normalizedName(full));
        values.add(sorted(NameNormalizer.tokens(full)));
        values.add(sorted(NameNormalizer.tokensForKeys(full)));
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        for (String value : values) {
            if (!value.isBlank()) {
                query.add(new TermQuery(new Term(IndexFields.NAME_EXACT, value)), BooleanClause.Occur.SHOULD);
            }
        }
        query.setMinimumNumberShouldMatch(1);
        return query.build();
    }

    private Query dateQuery(ParsedDate date) {
        float boost = (float) (1.0 + date.weight());
        if (date.precision() == DatePrecision.JOUR) {
            return new BoostQuery(LongPoint.newExactQuery(IndexFields.BIRTH_EPOCH_DAY,
                    date.date().toEpochDay()), boost);
        }
        if (date.precision() == DatePrecision.MOIS) {
            BooleanQuery.Builder month = new BooleanQuery.Builder();
            month.add(IntPoint.newExactQuery(IndexFields.BIRTH_YEAR, date.date().getYear()),
                    BooleanClause.Occur.MUST);
            month.add(IntPoint.newExactQuery(IndexFields.BIRTH_MONTH, date.date().getMonthValue()),
                    BooleanClause.Occur.MUST);
            return new BoostQuery(month.build(), boost);
        }
        return new BoostQuery(IntPoint.newExactQuery(IndexFields.BIRTH_YEAR, date.date().getYear()), boost);
    }

    private Query ngramQuery(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        int count = 0;
        try (TokenStream stream = ngramAnalyzer.tokenStream(IndexFields.NAME_NGRAM, text)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                query.add(new TermQuery(new Term(IndexFields.NAME_NGRAM, term.toString())),
                        BooleanClause.Occur.SHOULD);
                count++;
            }
            stream.end();
        } catch (IOException e) {
            return null;
        }
        if (count == 0) {
            return null;
        }
        query.setMinimumNumberShouldMatch(1);
        return query.build();
    }

    private static List<BytesRef> bytesTerms(Set<String> values) {
        List<BytesRef> terms = new ArrayList<>(values.size());
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                terms.add(new BytesRef(value));
            }
        }
        return terms;
    }

    private static String sorted(List<String> tokens) {
        return tokens.stream()
                .map(NameNormalizer::normalizeToken)
                .filter(t -> !t.isBlank())
                .sorted()
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }
}
