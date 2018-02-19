package com.company.sorchanolan;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

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

    createIndex(documents);
  }

  public void createIndex(List<Document> documents) throws Exception {
    StandardAnalyzer analyzer = new StandardAnalyzer(EnglishAnalyzer.getDefaultStopSet());
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    Directory directory = FSDirectory.open(Paths.get("index"));

    final IndexWriter writer = new IndexWriter(directory, config);
    for (Document document : documents) {
      writer.addDocument(document);
    }
    writer.close();
    directory.close();
  }
}
