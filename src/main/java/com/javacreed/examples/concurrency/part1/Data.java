package com.javacreed.examples.concurrency.part1;

public class Data {

  private int value;

  public synchronized int getValue() {
    return value;
  }

  public synchronized void setValue(final int value) {
    this.value = value;
  }
}
