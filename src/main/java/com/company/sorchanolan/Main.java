package com.company.sorchanolan;

import org.apache.lucene.analysis.Analyzer;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    List<AnalyserObj> analyzers = new ArrayList<>();
    analyzers.add(new AnalyserObj(new StandardAnalyzer(), "Standard"));
    analyzers.add(new AnalyserObj(new WhitespaceAnalyzer(), "Whitespace"));
    analyzers.add(new AnalyserObj(new EnglishAnalyzer(), "English"));
    analyzers.add(new AnalyserObj(new StopAnalyzer(), "Stop"));

    List<SimilarityObj> similarities = new ArrayList<>();
    similarities.add(new SimilarityObj(new ClassicSimilarity(), "Classic"));
    similarities.add(new SimilarityObj(new BM25Similarity(), "BM25"));
    similarities.add(new SimilarityObj(new BooleanSimilarity(), "Boolean"));

    List<Results> resultsList = new ArrayList<>();

    for (SimilarityObj similarity : similarities) {
      for (AnalyserObj analyzer : analyzers) {
        Path indexPath = Paths.get("index-" + analyzer.getName() + "-" + similarity.getName());
        String resultsPath = "trec-qrels-results-" + analyzer.getName() + "-" + similarity.getName() + ".txt";
        createIndex(cranfieldParser.parseDocuments(), analyzer.getAnalyzer(), similarity.getSimilarity(), indexPath);
        search(cranfieldParser.parseQueries(), analyzer.getAnalyzer(), similarity.getSimilarity(), indexPath, resultsPath);

        Results results = new Results();
        results.setAnalyzer(analyzer.getName());
        results.setSimilarity(similarity.getName());
        if (!similarity.getName().equals("Boolean")) {
          resultsList.add(runTrecEval(qrelsPath, resultsPath, results));
        } else {
          resultsList.add(runTrecEval(qrelsPathBoolean, resultsPath, results));
        }
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

  private Results runTrecEval(String groundTruthPath, String resultsPath, Results results) throws Exception {
    String[] command = {"./trec_eval/trec_eval", groundTruthPath, resultsPath};
    ProcessBuilder processBuilder = new ProcessBuilder(command);

    Process process = processBuilder.start();
    InputStream is = process.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    String line;

    while ((line = br.readLine()) != null) {
      System.out.println(line);
      if (line.startsWith("map")) {
        results.setMap(Double.parseDouble(line.split("\\s+")[2]));
      } else if (line.startsWith("gm_map")) {
        results.setGm_map(Double.parseDouble(line.split("\\s+")[2]));
      } else if (line.startsWith("P_5")) {
        results.setP_5(Double.parseDouble(line.split("\\s+")[2]));
      } else if (line.startsWith("P_10")) {
        results.setP_10(Double.parseDouble(line.split("\\s+")[2]));
      } else if (line.startsWith("P_15")) {
        results.setP_15(Double.parseDouble(line.split("\\s+")[2]));
      }
    }

    process.waitFor();
    return results;
  }
}
