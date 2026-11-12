package com.fastsearch.persons.search;

import com.fastsearch.persons.names.NameDictionary;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Un seul IndexWriter, un SearcherManager. L'index est reconstructible et
 * n'est jamais la source de vérité.
 */
public final class IndexManager implements Closeable {

    private final Path indexPath;
    private final Directory directory;
    private final IndexWriter writer;
    private final SearcherManager searcherManager;
    private final DocumentMapper mapper;
    private final Analyzer analyzer;
    private boolean closed;

    public IndexManager(Path indexPath, NameDictionary dictionary) throws IOException {
        this.indexPath = indexPath.toAbsolutePath().normalize();
        Files.createDirectories(this.indexPath);
        this.directory = FSDirectory.open(this.indexPath);
        this.mapper = new DocumentMapper(dictionary);
        this.analyzer = mapper.analyzers().name;
        IndexWriterConfig config = new IndexWriterConfig(analyzer)
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
                .setRAMBufferSizeMB(64.0);
        this.writer = new IndexWriter(directory, config);
        this.searcherManager = new SearcherManager(writer, new SearcherFactory());
    }

    public synchronized void index(PersonRecord person) throws IOException {
        ensureOpen();
        writer.updateDocument(new Term(IndexFields.SOURCE_ID, person.id()), mapper.toDocument(person));
    }

    public synchronized int indexAll(Collection<PersonRecord> persons) throws IOException {
        ensureOpen();
        int count = 0;
        for (PersonRecord person : persons) {
            writer.updateDocument(new Term(IndexFields.SOURCE_ID, person.id()), mapper.toDocument(person));
            count++;
        }
        return count;
    }

    public synchronized void delete(String sourceId) throws IOException {
        ensureOpen();
        writer.deleteDocuments(new Term(IndexFields.SOURCE_ID, sourceId));
    }

    public synchronized void deleteAll() throws IOException {
        ensureOpen();
        writer.deleteAll();
    }

    public synchronized void commit() throws IOException {
        ensureOpen();
        writer.commit();
        searcherManager.maybeRefreshBlocking();
    }

    public synchronized void rollback() throws IOException {
        ensureOpen();
        writer.rollback();
    }

    public SearchCandidates search(Query query, int limit) throws IOException {
        ensureOpen();
        if (query == null) {
            return new SearchCandidates(List.of(), 0);
        }
        searcherManager.maybeRefreshBlocking();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            TopDocs topDocs = searcher.search(query, Math.max(1, limit));
            StoredFields stored = searcher.storedFields();
            List<Candidate> candidates = new ArrayList<>(topDocs.scoreDocs.length);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document document = stored.document(scoreDoc.doc);
                candidates.add(new Candidate(document, scoreDoc.score));
            }
            return new SearchCandidates(candidates, topDocs.totalHits.value);
        } finally {
            searcherManager.release(searcher);
        }
    }

    public IndexStats stats() throws IOException {
        ensureOpen();
        searcherManager.maybeRefreshBlocking();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            int maxDoc = searcher.getIndexReader().maxDoc();
            int numDocs = searcher.getIndexReader().numDocs();
            return new IndexStats(numDocs, maxDoc, Math.max(0, maxDoc - numDocs),
                    indexPath.toString(), true);
        } finally {
            searcherManager.release(searcher);
        }
    }

    public DocumentMapper mapper() {
        return mapper;
    }

    public Path indexPath() {
        return indexPath;
    }

    public boolean isOpen() {
        return !closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Index fermé");
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        try {
            searcherManager.close();
        } catch (IOException e) {
            failure = e;
        }
        try {
            writer.close();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            }
        }
        try {
            directory.close();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            }
        }
        try {
            mapper.analyzers().close();
        } catch (RuntimeException e) {
            if (failure == null) {
                failure = new IOException(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public record Candidate(Document document, float luceneScore) {
    }

    public record SearchCandidates(List<Candidate> candidates, long totalHits) {
        public SearchCandidates {
            candidates = List.copyOf(candidates);
        }
    }

    public record IndexStats(long numDocs, long maxDoc, long deletedDocs, String path, boolean open) {
    }
}
