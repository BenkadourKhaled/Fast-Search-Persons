package com.fastsearch.persons.search;

/** Noms de champs Lucene, centralisés pour éviter les fautes de frappe. */
public final class IndexFields {

    public static final String SOURCE_ID = "source_id";
    public static final String LAST_NAME_RAW = "last_name_raw";
    public static final String FIRST_NAME_RAW = "first_name_raw";
    public static final String FULL_NAME_RAW = "full_name_raw";
    public static final String FULL_NAME_TRANSLIT = "full_name_translit";
    public static final String NAME_EXACT = "name_exact";
    public static final String NAME_VARIANTS = "name_variants";
    public static final String NAME_PHONETIC_FINE = "name_phonetic_fine";
    public static final String NAME_PHONETIC_COARSE = "name_phonetic_coarse";
    public static final String NAME_NGRAM = "name_ngram";
    public static final String BIRTH_DATE = "birth_date";
    public static final String BIRTH_PRECISION = "birth_precision";
    public static final String BIRTH_FLAGS = "birth_flags";
    public static final String BIRTH_EPOCH_DAY = "birth_epoch_day";
    public static final String BIRTH_YEAR = "birth_year";
    public static final String BIRTH_MONTH = "birth_month";
    public static final String BIRTH_DAY = "birth_day";
    public static final String BIRTH_PLACE = "birth_place";
    public static final String BIRTH_PLACE_EXACT = "birth_place_exact";
    public static final String FATHER_NAME = "father_name";
    public static final String MOTHER_NAME = "mother_name";
    public static final String OWNER_GROUP = "owner_group";
    public static final String LANG = "lang";

    private IndexFields() {
    }
}
