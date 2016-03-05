package com.google.j2cl.transpiler.integration.interfacedevirtualize;

/**
 * Test CharSequence Interface on all devirtualized classes that implement it.
 */
public class CharSequenceTest {
  
  public static void test() {
    final String s = "string";
    CharSequence cs =
        new CharSequence() {
          @Override
          public int length() {
            return s.length();
          }

          @Override
          public char charAt(int i) {
            return s.charAt(i);
          }

          @Override
          public CharSequence subSequence(int i, int j) {
            return s.subSequence(i, j);
          }

          @Override
          public boolean equals(Object c) {
            return s.equals(c);
          }

          @Override
          public int hashCode() {
            return s.hashCode();
          }

          @Override
          public String toString() {
            return s.toString();
          }
        };

    assert s.length() == 6;
    assert cs.length() == 6;

    assert s.charAt(1) == 't';
    assert cs.charAt(1) == 't';

    assert s.subSequence(0, 2).equals("st");
    assert cs.subSequence(0, 2).equals("st");

    assert s.equals("string");
    assert cs.equals("string");

    assert s.hashCode() == "string".hashCode();
    assert cs.hashCode() == "string".hashCode();

    assert s.toString().equals("string");
    assert cs.toString().equals("string");

    assert s.getClass() == String.class;
    assert cs.getClass() != String.class;
  }
}

