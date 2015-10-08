package com.google.j2cl.transpiler.readable.devirtualizedcalls;

/**
 * Test String devirtualization.
 */
public class StringCalls {
  public void main() {
    String literal = "String";
    String sub = literal.substring(1);
    String sub2 = literal.substring(2, 3);
    String trimmed = literal.trim();

    //Override calls
    String newString = literal.toString();
  }
}

