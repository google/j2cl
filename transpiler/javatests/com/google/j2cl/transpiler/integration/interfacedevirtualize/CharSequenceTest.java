package com.google.j2cl.transpiler.integration.interfacedevirtualize;

/**
 * Test CharSequence Interface on all devirtualized classes that implement it.
 */
public class CharSequenceTest {
  
  public static void test() {
    String s = "string";
    CharSequence cs = "string";

    assert s.length() == 6;
    assert cs.length() == 6;

    assert s.charAt(1) == 't';
    assert cs.charAt(1) == 't';

    assert s.subSequence(0, 2).equals("st");
    assert cs.subSequence(0, 2).equals("st");

    assert s.toString().equals("string");
    assert cs.toString().equals("string");
  }
}

