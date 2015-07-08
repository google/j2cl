package com.google.j2cl.transpiler.integration.simplestarrayliteral;

public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    // This translates into a raw JS array with no custom Java initialization.
    Object[] values = new Object[] {"sam", "bob", "charlie"};

    // So of course you can insert a leaf value of a different but conforming type.
    values[0] = "jimmy";

    // But even so, you can not insert to an out of bounds index.
    try {
      values[100] = "ted";
      assert false : "An expected failure did not occur.";
    } catch (ArrayIndexOutOfBoundsException e) {
      // expected
    }

    // And it still has a functioning class literal.
    assert Object[].class == values.getClass();
  }
}
