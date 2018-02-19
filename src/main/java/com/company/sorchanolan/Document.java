package com.company.sorchanolan;

import lombok.Data;

@Data
public class Document {
  private int index;
  private String title;
  private String author;
  private String journal;
  private String text;

  public Document(int index, String title, String author, String journal, String text) {
    this.index = index;
    this.title = title;
    this.author = author;
    this.journal = journal;
    this.text = text;
  }
}
