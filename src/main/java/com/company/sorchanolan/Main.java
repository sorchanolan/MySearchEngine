package com.company.sorchanolan;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
  private String qrelsPath = "trec-qrels.txt";
  private String qrelsPathBoolean = "trec-qrels-boolean.txt";

  public static void main(String[] args) throws Exception {
    new Main();
  }

  public Main() throws Exception {
    CranfieldParser cranfieldParser = new CranfieldParser();
    cranfieldParser.parseRelevanceJudgements(qrelsPath, qrelsPathBoolean);

    List<Analyzer> analyzers = new ArrayList<>();
    analyzers.add(new StandardAnalyzer());
    analyzers.add(new WhitespaceAnalyzer());
    analyzers.add(new EnglishAnalyzer());
    analyzers.add(new StopAnalyzer());

    List<Similarity> similarities = new ArrayList<>();
    similarities.add(new ClassicSimilarity());
    similarities.add(new BM25Similarity());
    similarities.add(new BooleanSimilarity());

    int count = 0;
    for (Similarity similarity : similarities) {
      for (Analyzer analyzer : analyzers) {
        Path indexPath = Paths.get("index-" + count);
        String resultsPath = "trec-qrels-results-" + count + ".txt";
        createIndex(cranfieldParser.parseDocuments(), analyzer, similarity, indexPath);
        search(cranfieldParser.parseQueries(), analyzer, similarity, indexPath, resultsPath);
        count++;
      }
    }
  }

  private void createIndex(List<Document> documents, Analyzer analyzer, Similarity similarity, Path indexPath) throws Exception {
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
    config.setSimilarity(similarity);
    Directory directory = FSDirectory.open(indexPath);

    final IndexWriter writer = new IndexWriter(directory, config);
    for (Document document : documents) {
      writer.addDocument(document);
    }
    writer.close();
    directory.close();
  }

  private void search(List<Query> queries, Analyzer analyzer, Similarity similarity, Path indexPath, String resultsPath) throws Exception {
    IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
    IndexSearcher searcher = new IndexSearcher(reader);
    searcher.setSimilarity(similarity);

    MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
        new String[] {"text", "title", "author", "journal"},
        analyzer);

    PrintWriter writer = new PrintWriter(resultsPath, "UTF-8");

    for (int queryIndex = 1; queryIndex <= queries.size(); queryIndex++) {
      String currentQuery = queries.get(queryIndex-1).getQuery();
      currentQuery = QueryParser.escape(currentQuery);
      org.apache.lucene.search.Query query = queryParser.parse(currentQuery);
      TopDocs results = searcher.search(query, 1400);
      ScoreDoc[] hits = results.scoreDocs;

      for (int hitIndex = 0; hitIndex < hits.length; hitIndex++) {
        ScoreDoc hit = hits[hitIndex];
        writer.println(queryIndex + " 0 " + hit.doc+1 + " " + hitIndex + " " + hit.score + " 0 ");
      }
    }
    writer.close();
  }
}
