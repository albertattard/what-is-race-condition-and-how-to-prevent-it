package com.javacreed.examples.concurrency.part2;

public class Data {

  private int value;

  public synchronized int getValue() {
    return value;
  }

  public synchronized void setValue(final int value) {
    this.value = value;
  }
}
