package com.google.j2cl.transpiler.readable.genericanddefaultmethods;

public interface InterfaceWithDefault {
  public default void foo(String value) {
    System.out.println("in InterfaceWithDefault");
  }
}
