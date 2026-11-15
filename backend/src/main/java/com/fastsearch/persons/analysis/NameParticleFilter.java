package com.fastsearch.persons.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.FilteringTokenFilter;

import java.util.Set;

/**
 * Retire les particules des noms composés pour la recherche.
 *
 * <p>Choix : la particule est conservée dans les champs *_raw pour l'affichage,
 * mais exclue des clés de rapprochement. Cela évite que "Ben Ali" et "Ali"
 * soient artificiellement éloignés, tout en gardant une trace lisible.</p>
 */
public final class NameParticleFilter extends FilteringTokenFilter {

    public static final Set<String> PARTICLES = Set.of(
            "ben", "bin", "bent", "bint", "ould", "ouled", "ibn",
            "el", "al", "bel",
            "\u0628\u0646",       // بن
            "\u0628\u0646\u062A", // بنت
            "\u0648\u0644\u062F", // ولد
            "\u0627\u0628\u0646", // ابن
            "\u0627\u0644"        // ال
    );

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public NameParticleFilter(TokenStream input) {
        super(input);
    }

    @Override
    protected boolean accept() {
        return !PARTICLES.contains(termAtt.toString());
    }
}
