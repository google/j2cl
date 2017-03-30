package com.google.j2cl.transpiler.regression.java8.package2;

/** An interface with a single default method m. */
public interface SimpleB {
  default String m() {
    return "SimpleB";
  }
}
