package com.fastsearch.persons.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

/** Applique {@link ArabicTextNormalizer} jeton par jeton. */
public final class ArabicNormalizationTokenFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public ArabicNormalizationTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }
        String normalized = ArabicTextNormalizer.normalize(termAtt.toString());
        termAtt.setEmpty().append(normalized);
        return true;
    }
}
