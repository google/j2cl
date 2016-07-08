package com.google.j2cl.transpiler.readable.transitivejsoverlayimport;

public class Main {

  public static void main(String... args) {
    Immediate immediate = null;
    immediate.doImmediateInstanceMethod();
    immediate.doTransitiveInstanceMethod();
  }
}
