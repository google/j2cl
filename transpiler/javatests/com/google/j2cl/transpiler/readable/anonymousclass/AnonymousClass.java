package com.google.j2cl.transpiler.readable.anonymousclass;

interface AnonymousInterface {
  public String foo();
}

public class AnonymousClass {
  public void main() {
    AnonymousInterface instance =
        new AnonymousInterface() {
          public String foo() {
            return "a";
          }
        };
  }
}
