package com.javacreed.examples.concurrency.part1;

public class Example1 {

  public static void main(final String[] args) {
    final Data data = new Data();

    final int value = data.getValue();
    data.setValue(value + 1);
    System.out.println(data.getValue());
  }
}
