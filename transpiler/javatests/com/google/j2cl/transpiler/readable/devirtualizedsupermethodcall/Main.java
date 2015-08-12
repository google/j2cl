package com.google.j2cl.transpiler.readable.devirtualizedsupermethodcall;

public class Main {
  public void main() {
    FooCallsSuperObjectMethod fooCallsSuperObjectMethods = new FooCallsSuperObjectMethod();
    fooCallsSuperObjectMethods.hashCode();

    SubNumber sn = new SubNumber();
    sn.byteValue();
    sn.doubleValue();
    sn.floatValue();
    sn.intValue();
    sn.shortValue();
  }
}
