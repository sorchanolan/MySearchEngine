package com.company.sorchanolan;

import lombok.Data;
import org.apache.lucene.analysis.Analyzer;

@Data
public class Results {
  private String analyzer;
  private String similarity;
  private double map;
  private double gm_map;
  private double p_5;
  private double p_10;
  private double p_15;
}
