package com.google.j2cl.transpiler.integration.packagecrosscall;

public class Main {

  static class Inner {
    static int caller() {
      return packageMethod();
    }
  }

  static int packageMethod() {
    return 3;
  }

  public static void main(String[] args) {
    assert Inner.caller() == 3;
  }
}
