package com.google.j2cl.transpiler.integration.privatecrosscall;

public class Main {

  static class Inner {
    static int caller() {
      return privateMethod();
    }
  }

  private static int privateMethod() {
    return 3;
  }

  public static void main(String[] args) {
    assert Inner.caller() == 3;
  }
}
