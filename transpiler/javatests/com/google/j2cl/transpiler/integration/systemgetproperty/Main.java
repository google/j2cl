package com.google.j2cl.transpiler.integration.systemgetproperty;

public class Main {

  public static void main(String[] args) {
    assert System.getProperty("foo") == null;
    assert System.getProperty("foo", "bar").equals("bar");
  }
}
