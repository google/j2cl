package com.google.j2cl.transpiler.integration.anonymousclasswithcaptures;

interface AnonymousInterface {
  public void foo();
}

public class Main {
  public static void main(String... args) {
    final Object[] instances = new Object[1];

    AnonymousInterface intf1 =
        new AnonymousInterface() {
          @Override
          public void foo() {
            instances[0] = this;
          }
        };

    assert instances[0] == null;

    intf1.foo();

    assert instances[0] instanceof AnonymousInterface;
    assert instances[0] == intf1;
  }
}
