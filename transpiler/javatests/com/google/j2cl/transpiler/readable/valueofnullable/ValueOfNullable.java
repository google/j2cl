package com.google.j2cl.transpiler.readable.valueofnullable;

public class ValueOfNullable {
  String nullableString = "b";
  char someChar = 'a';
  /**
   * someChar will be boxed in output using String.valueOf(Character.valueOf(someChar)) and since
   * the return value is nullable the concatenation expression needs further special handling
   * (prefixing the expression with "" + ) to guard against null + null concatenation.
   */
  String result = someChar + nullableString;
}
