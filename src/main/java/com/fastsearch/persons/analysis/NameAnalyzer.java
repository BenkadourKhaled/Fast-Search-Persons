package com.fastsearch.persons.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Analyseur de noms : aucune racinisation, aucune suppression de préfixe.
 * Les noms propres ne sont pas du texte ordinaire ; un stemmer produirait
 * des rapprochements dangereux (par exemple entre patronymes distincts).
 */
public final class NameAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new StandardTokenizer();
        TokenStream stream = new LowerCaseFilter(tokenizer);
        stream = new ASCIIFoldingFilter(stream, false);
        stream = new ArabicNormalizationTokenFilter(stream);
        stream = new NameParticleFilter(stream);
        return new TokenStreamComponents(tokenizer, stream);
    }
}
