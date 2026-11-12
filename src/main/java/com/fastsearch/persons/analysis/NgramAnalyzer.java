package com.fastsearch.persons.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Trigrammes de caractères, utilisés avec un poids très faible pour absorber
 * une partie des erreurs OCR. Ce n'est jamais un signal d'identité.
 */
public final class NgramAnalyzer extends Analyzer {

    private final int minGram;
    private final int maxGram;

    public NgramAnalyzer() {
        this(3, 3);
    }

    public NgramAnalyzer(int minGram, int maxGram) {
        if (minGram < 2 || maxGram < minGram) {
            throw new IllegalArgumentException("Bornes de n-grammes invalides");
        }
        this.minGram = minGram;
        this.maxGram = maxGram;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new StandardTokenizer();
        TokenStream stream = new LowerCaseFilter(tokenizer);
        stream = new ASCIIFoldingFilter(stream, false);
        stream = new ArabicNormalizationTokenFilter(stream);
        stream = new NGramTokenFilter(stream, minGram, maxGram, false);
        return new TokenStreamComponents(tokenizer, stream);
    }
}
