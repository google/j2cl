package com.google.j2cl.transpiler.integration.packageprivatemethods;

import com.google.j2cl.transpiler.integration.packageprivatemethods.package1.Caller;

public class Main {
  public static void main(String[] args) {
    new Caller().test();
  }
}
