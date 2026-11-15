package com.fastsearch.persons.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** Configuration serveur, jamais fournie par le client. */
@ConfigurationProperties(prefix = "fastsearch")
public class FastSearchProperties {

    private String dictionaryPath = "";
    private final Index index = new Index();
    private final Search search = new Search();
    private final Sync sync = new Sync();
    private final Eval eval = new Eval();
    private final Demo demo = new Demo();
    private final Cors cors = new Cors();

    public String getDictionaryPath() {
        return dictionaryPath;
    }

    public void setDictionaryPath(String dictionaryPath) {
        this.dictionaryPath = dictionaryPath == null ? "" : dictionaryPath.trim();
    }

    public Index getIndex() {
        return index;
    }

    public Search getSearch() {
        return search;
    }

    public Sync getSync() {
        return sync;
    }

    public Eval getEval() {
        return eval;
    }

    public Demo getDemo() {
        return demo;
    }

    public Cors getCors() {
        return cors;
    }

    public static class Index {
        private String path = "./data/index";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path == null || path.isBlank() ? "./data/index" : path.trim();
        }
    }

    public static class Search {
        private int candidateLimit = 500;
        private int maxPageSize = 100;
        private boolean enableCoarse = true;

        public int getCandidateLimit() {
            return candidateLimit;
        }

        public void setCandidateLimit(int candidateLimit) {
            this.candidateLimit = candidateLimit;
        }

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }

        public boolean isEnableCoarse() {
            return enableCoarse;
        }

        public void setEnableCoarse(boolean enableCoarse) {
            this.enableCoarse = enableCoarse;
        }
    }

    public static class Sync {
        private int batchSize = 500;
        private String table = "person_source";
        private String idColumn = "id";
        private String lastNameColumn = "last_name";
        private String firstNameColumn = "first_name";
        private String birthDateColumn = "birth_date";
        private String birthPrecisionColumn = "birth_precision";
        private String birthFlagsColumn = "birth_flags";
        private String birthPlaceColumn = "birth_place";
        private String fatherNameColumn = "father_name";
        private String motherNameColumn = "mother_name";
        private String ownerGroupColumn = "owner_group";
        private String langColumn = "lang";
        private String updatedAtColumn = "updated_at";
        private String deletedColumn = "deleted";

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getIdColumn() {
            return idColumn;
        }

        public void setIdColumn(String idColumn) {
            this.idColumn = idColumn;
        }

        public String getLastNameColumn() {
            return lastNameColumn;
        }

        public void setLastNameColumn(String lastNameColumn) {
            this.lastNameColumn = lastNameColumn;
        }

        public String getFirstNameColumn() {
            return firstNameColumn;
        }

        public void setFirstNameColumn(String firstNameColumn) {
            this.firstNameColumn = firstNameColumn;
        }

        public String getBirthDateColumn() {
            return birthDateColumn;
        }

        public void setBirthDateColumn(String birthDateColumn) {
            this.birthDateColumn = birthDateColumn;
        }

        public String getBirthPrecisionColumn() {
            return birthPrecisionColumn;
        }

        public void setBirthPrecisionColumn(String birthPrecisionColumn) {
            this.birthPrecisionColumn = birthPrecisionColumn;
        }

        public String getBirthFlagsColumn() {
            return birthFlagsColumn;
        }

        public void setBirthFlagsColumn(String birthFlagsColumn) {
            this.birthFlagsColumn = birthFlagsColumn;
        }

        public String getBirthPlaceColumn() {
            return birthPlaceColumn;
        }

        public void setBirthPlaceColumn(String birthPlaceColumn) {
            this.birthPlaceColumn = birthPlaceColumn;
        }

        public String getFatherNameColumn() {
            return fatherNameColumn;
        }

        public void setFatherNameColumn(String fatherNameColumn) {
            this.fatherNameColumn = fatherNameColumn;
        }

        public String getMotherNameColumn() {
            return motherNameColumn;
        }

        public void setMotherNameColumn(String motherNameColumn) {
            this.motherNameColumn = motherNameColumn;
        }

        public String getOwnerGroupColumn() {
            return ownerGroupColumn;
        }

        public void setOwnerGroupColumn(String ownerGroupColumn) {
            this.ownerGroupColumn = ownerGroupColumn;
        }

        public String getLangColumn() {
            return langColumn;
        }

        public void setLangColumn(String langColumn) {
            this.langColumn = langColumn;
        }

        public String getUpdatedAtColumn() {
            return updatedAtColumn;
        }

        public void setUpdatedAtColumn(String updatedAtColumn) {
            this.updatedAtColumn = updatedAtColumn;
        }

        public String getDeletedColumn() {
            return deletedColumn;
        }

        public void setDeletedColumn(String deletedColumn) {
            this.deletedColumn = deletedColumn;
        }
    }

    public static class Eval {
        private long seed = 42;
        private int count = 40;
        private String openSearchUrl = "";
        private String openSearchIndex = "persons";

        public long getSeed() {
            return seed;
        }

        public void setSeed(long seed) {
            this.seed = seed;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getOpenSearchUrl() {
            return openSearchUrl;
        }

        public void setOpenSearchUrl(String openSearchUrl) {
            this.openSearchUrl = openSearchUrl == null ? "" : openSearchUrl.trim();
        }

        public String getOpenSearchIndex() {
            return openSearchIndex;
        }

        public void setOpenSearchIndex(String openSearchIndex) {
            this.openSearchIndex = openSearchIndex == null ? "persons" : openSearchIndex.trim();
        }
    }

    public static class Demo {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Origines autorisées pour l'interface web. En développement, Vite tourne
     * sur 5173 ; la prévisualisation sur 4173. Une origine explicite est
     * préférable à « * », surtout avec HTTP Basic.
     */
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:4173",
                "http://127.0.0.1:4173"
        ));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins == null
                    ? new ArrayList<>() : new ArrayList<>(allowedOrigins);
        }
    }
}
