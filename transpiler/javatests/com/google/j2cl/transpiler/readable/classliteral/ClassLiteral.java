package com.google.j2cl.transpiler.readable.classliteral;

public class ClassLiteral {
  @SuppressWarnings("unused")
  public void main() {
    Object o = ClassLiteral.class;
    o = ClassLiteral[][].class;
  }
}
