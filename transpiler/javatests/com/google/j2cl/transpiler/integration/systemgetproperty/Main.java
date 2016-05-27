package com.google.j2cl.transpiler.integration.systemgetproperty;

public class Main {

  public static void main(String[] args) {
    assert System.getProperty("goog.DEBUG").equals("true");
    assert System.getProperty("jre.bar", "bar").equals("bar");
  }
}
