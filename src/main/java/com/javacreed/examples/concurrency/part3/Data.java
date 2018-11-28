package com.javacreed.examples.concurrency.part3;

public class Data {

  private int value;

  public synchronized void adjustBy(final int adjustment) {
    value += adjustment;
  }

  public synchronized int getValue() {
    return value;
  }

  public synchronized void setValue(final int value) {
    this.value = value;
  }
}
