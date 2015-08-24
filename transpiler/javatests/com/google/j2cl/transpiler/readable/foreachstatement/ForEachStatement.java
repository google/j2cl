package com.google.j2cl.transpiler.readable.foreachstatement;

public class ForEachStatement {
  public void test(Iterable<Throwable> iterable) {
    for (Throwable t : iterable) {
      t.toString();
    }

    for (Throwable t : new Throwable[10]) {
      t.toString();
    }
  }
}
