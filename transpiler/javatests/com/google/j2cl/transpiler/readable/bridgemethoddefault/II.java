package com.google.j2cl.transpiler.readable.bridgemethoddefault;

interface II extends I<String> {
  @Override
  default void m(String s) {}
}
