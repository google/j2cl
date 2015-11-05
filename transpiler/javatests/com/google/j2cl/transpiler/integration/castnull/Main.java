package com.google.j2cl.transpiler.integration.castnull;

public class Main {

  public static void main(String... args) {
    Object a = null;
    String s = (String) a;
  }
}