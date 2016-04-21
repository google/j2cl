package com.google.j2cl.transpiler.readable.stringliteral;

public class StringLiteral {
  private String someString = "This is a string literal";
  private static final String ESCAPE_CODES = "\b\f\n\r\t\"\'\\\u0000\u007F𐍆：";
  private String nonBmpChar = "𐍆";
  private String wideColon = "：";
  private static final String ESCAPE_CODES_COPY = ESCAPE_CODES;
}
