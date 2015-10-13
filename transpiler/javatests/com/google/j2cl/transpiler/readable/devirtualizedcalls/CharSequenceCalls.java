package com.google.j2cl.transpiler.readable.devirtualizedcalls;

public class CharSequenceCalls {
  public void test(CharSequence cs) {
    cs.charAt(0);
  }

  public void main() {
    String s = "s";
    test(s);
  }
}
