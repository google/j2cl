package com.google.j2cl.transpiler.readable.stringconversion;

public class StringConversion {
  public void test() {
    // Two Null String instances.
    String s1 = null;
    String s2 = null;
    String s3 = s1 + s2; // two nullable strings
    s2 += s2; // nullable string compound assignment, plus a nullable string.
    s1 += "a"; // nullable string compound assignment, plus a string literal.
    // multiple nullable string instances concatenation, and plus a string literal.
    s3 = s1 + s1 + s2 + null + "a";
    // a string literal plus multiple nullable string instances.
    s3 = "a" + s1 + s1 + s2 + null;

    // Char + String
    String s4;
    char c1 = 'F';
    char c2 = 'o';
    s4 = c1 + c2 + "o";
  }
}
