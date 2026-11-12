package com.fastsearch.persons.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArabicTextNormalizerTest {

    @Test
    void tashkeel_et_tatweel_sont_retires() {
        assertEquals("محمد", ArabicTextNormalizer.normalize("مُحَمَّد"));
        assertEquals("محمد", ArabicTextNormalizer.normalize("مـــحـــمـــد"));
    }

    @Test
    void alifs_et_ya_et_ta_marbuta_sont_unifies() {
        assertEquals("احمد", ArabicTextNormalizer.normalize("أحمد"));
        assertEquals("احمد", ArabicTextNormalizer.normalize("إحمد"));
        assertEquals("احمد", ArabicTextNormalizer.normalize("آحمد"));
        assertEquals("مصطفي", ArabicTextNormalizer.normalize("مصطفى"));
        assertEquals("فاطمه", ArabicTextNormalizer.normalize("فاطمة"));
        assertEquals("فاطمة", ArabicTextNormalizer.normalize("فاطمة", false));
    }

    @Test
    void chiffres_arabes_indiens_et_persans() {
        assertEquals("07/04/1992", ArabicTextNormalizer.normalize("٠٧/٠٤/١٩٩٢"));
        assertEquals("07/04/1992", ArabicTextNormalizer.normalize("۰۷/۰۴/۱۹۹۲"));
    }

    @Test
    void detection_arabe() {
        assertTrue(ArabicTextNormalizer.containsArabic("وليد"));
        assertTrue(!ArabicTextNormalizer.containsArabic("Walid"));
    }
}
