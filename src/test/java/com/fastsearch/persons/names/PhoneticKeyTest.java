package com.fastsearch.persons.names;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneticKeyTest {

    private static String fine(String value) {
        return PhoneticKey.of(value).fine();
    }

    private static String coarse(String value) {
        return PhoneticKey.of(value).coarse();
    }

    @Test
    void homophones_latins_exiges() {
        assertEquals(fine("Walid"), fine("Oualid"));
        assertEquals(fine("Walid"), fine("Ouelid"));
        assertEquals(fine("Walid"), fine("Walide"));
        assertEquals(fine("Ouahid"), fine("Wahid"));
        assertEquals(fine("Oussama"), fine("Oussamah"));
        assertEquals(fine("Oussama"), fine("Ousama"));
        assertEquals(fine("Mourad"), fine("Mourade"));
        assertEquals(fine("Mourad"), fine("Murad"));
        assertEquals(fine("Nacer"), fine("Nasser"));
        assertEquals(fine("Rachid"), fine("Rashid"));
        assertEquals(fine("Rachid"), fine("Rachide"));
        assertEquals(fine("Zineb"), fine("Zeinab"));
        assertEquals(fine("Zineb"), fine("Zaynab"));
        assertEquals(fine("Hocine"), fine("Houcine"));
        assertEquals(fine("Karim"), fine("Kerim"));
        assertEquals(fine("Mohamed"), fine("Mohammed"));
        assertEquals(fine("Abdelkader"), compact(PhoneticKey.ofName("Abd El Kader").fine()));
        assertEquals(fine("Yacine"), fine("Yassine"));
    }

    @Test
    void negatifs_cles_fines_differentes() {
        assertNotEquals(fine("Walid"), fine("Ould"));
        assertNotEquals(fine("Walid"), fine("Wahid"));
        assertNotEquals(fine("Karim"), fine("Kamil"));
        assertNotEquals(fine("Nacer"), fine("Nasim"));
    }

    @Test
    void table_de_cas_limites_ou() {
        assertEquals("walid", fine("Oualid"));
        assertEquals("wahid", fine("Ouahid"));
        assertEquals("bwalam", fine("Boualem"));
        assertEquals("mulud", fine("Mouloud"));
        assertEquals("muhamad", fine("Mouhamed"));
        assertEquals("uld", fine("Ould"));
        assertEquals("murad", fine("Mourad"));
        assertEquals("buzid", fine("Bouzid"));
        assertEquals("usama", fine("Oussama"));
        assertEquals("wasila", fine("Ouassila"));
        assertEquals("iusaf", fine("Youcef"));
        assertEquals("wld", coarse("Ould"));
    }

    @Test
    void collisions_grossieres_documentees() {
        assertEquals(coarse("Walid"), coarse("Ould"));
        assertEquals(coarse("Walid"), coarse("Wild"));
        assertEquals(coarse("Walid"), coarse("Wled"));
        assertNotEquals(fine("Walid"), fine("Wild"));
    }

    @Test
    void pont_arabe_latin_par_cle_grossiere() {
        assertEquals("wld", compact(PhoneticKey.ofName("وليد").coarse()));
        assertEquals("mhmd", compact(PhoneticKey.ofName("محمد").coarse()));
        assertEquals("bdkdr", compact(PhoneticKey.ofName("عبد القادر").coarse()));
        assertEquals("sn", compact(PhoneticKey.ofName("ياسين").coarse()));
        assertEquals("ftm", compact(PhoneticKey.ofName("فاطمة").coarse()));
        assertEquals("kdj", compact(PhoneticKey.ofName("خديجة").coarse()));
        assertEquals("blm", compact(PhoneticKey.ofName("بوعلام").coarse()));
        assertEquals("bdrhmn", compact(PhoneticKey.ofName("عبد الرحمان").coarse()));
    }

    private static String compact(String key) {
        return key.replace(" ", "");
    }

    @Test
    void voyelles_breves_absentes_de_l_arabe() {
        // Constat central : la clé fine n'est pas identique entre les deux scripts.
        assertNotEquals(fine("Walid"), fine("وليد"));
        assertNotEquals(fine("Mohamed"), fine("محمد"));
    }

    @Test
    void nom_compose_abdel() {
        assertEquals(fine("Abdelkader"), compact(PhoneticKey.ofName("Abd El Kader").fine()));
        assertTrue(PhoneticKey.ofName("BENALI Karim").fine().contains("karim"));
    }
}
