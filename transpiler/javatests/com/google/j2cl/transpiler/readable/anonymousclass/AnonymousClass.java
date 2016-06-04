package com.google.j2cl.transpiler.readable.anonymousclass;

abstract class SomeClass {
  public abstract String foo();

  SomeClass(int i) {}
}

public class AnonymousClass {
  public void main() {
    SomeClass instance =
        new SomeClass(1) {
          public String foo() {
            return "a";
          }
        };
  }
}

interface SomeInterface {
  SomeClass implicitlyStatic =
      new SomeClass(1) {
        public String foo() {
          return "a";
        }
      };
}
