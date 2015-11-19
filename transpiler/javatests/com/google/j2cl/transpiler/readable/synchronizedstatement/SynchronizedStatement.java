package com.google.j2cl.transpiler.readable.synchronizedstatement;

public class SynchronizedStatement {
  private int a;
  private int b;

  public void main() {
    synchronized (this) {
      a++;
      b--;
    }
  }
}
