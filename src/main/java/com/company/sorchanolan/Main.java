package com.company.sorchanolan;

import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

  public static void main(String[] args) throws Exception {
    new Main();
  }

  public Main() throws Exception {
    CranfieldParser cranfieldParser = new CranfieldParser();

    List<Document> documents = cranfieldParser.parseDocuments();
    List<Query> queries = cranfieldParser.parseQueries();
    List<RelevanceJudgement> relevanceJudgements = cranfieldParser.parseRelevanceJudgements();

    Path indexPath = Paths.get("index");

    createIndex(documents, indexPath);
    search(queries, indexPath);
  }

  public void createIndex(List<Document> documents, Path indexPath) throws Exception {
    Analyzer analyzer = englishAnalyser();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
    Directory directory = FSDirectory.open(indexPath);

    final IndexWriter writer = new IndexWriter(directory, config);
    for (Document document : documents) {
      writer.addDocument(document);
    }
    writer.close();
    directory.close();
  }

  public void search(List<Query> queries, Path indexPath) throws Exception {
    IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = englishAnalyser();

    MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
        new String[] {"text", "title", "author", "journal"},
        analyzer);

    PrintWriter writer = new PrintWriter("trec_eval.9.0/trec-qrels-results.txt", "UTF-8");

    for (int queryIndex = 1; queryIndex <= queries.size(); queryIndex++) {
      String currentQuery = queries.get(queryIndex-1).getQuery();
      currentQuery = QueryParser.escape(currentQuery);
      org.apache.lucene.search.Query query = queryParser.parse(currentQuery);
      TopDocs results = searcher.search(query, 1400);
      ScoreDoc[] hits = results.scoreDocs;
      int numTotalHits = Math.toIntExact(results.totalHits);
      for (int hitIndex = 0; hitIndex < hits.length; hitIndex++) {
        ScoreDoc hit = hits[hitIndex];
        writer.println(queryIndex + " 0 " + hit.doc+1 + " " + hitIndex + " " + hit.score + " 0 ");
      }
      System.out.println(numTotalHits + " total matching documents");
    }
    writer.close();
  }

  public Analyzer englishAnalyser() {
    return new StandardAnalyzer(EnglishAnalyzer.getDefaultStopSet());
  }
}
