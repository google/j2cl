package com.google.j2cl.transpiler.integration.protectedcrosscall;

public class Main {

  static class Inner {
    static int caller() {
      return protectedMethod();
    }
  }

  protected static int protectedMethod() {
    return 3;
  }

  public static void main(String[] args) {
    assert Inner.caller() == 3;
  }
}
