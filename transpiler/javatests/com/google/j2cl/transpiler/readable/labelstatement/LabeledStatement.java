package com.google.j2cl.transpiler.readable.labelstatement;

public class LabeledStatement {
  public void test() {
    LABEL:
    for (; ; ) {
      break LABEL;
    }

    LABEL1:
    for (; ; ) {
      continue LABEL1;
    }
  }
}
