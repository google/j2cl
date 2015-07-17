package com.google.j2cl.transpiler.integration.multipleanonymousclass;

/**
 * Test multiple anonymous classes.
 */
interface AnonymousInterface {
  AnonymousInterface getSelf();
}

public class Main {
  public static void main(String[] args) {
    AnonymousInterface intf1 =
        new AnonymousInterface() {
          @Override
          public AnonymousInterface getSelf() {
            return this;
          }
        };
    AnonymousInterface intf2 =
        new AnonymousInterface() {
          @Override
          public AnonymousInterface getSelf() {
            return this;
          }
        };

    assert intf1 == intf1.getSelf();
    assert intf2 == intf2.getSelf();
    assert intf1.getClass() != intf2.getClass();
    assert intf1
        .getClass()
        .getName()
        .equals("com.google.j2cl.transpiler.integration.multipleanonymousclass.Main$1");
    assert intf2
        .getClass()
        .getName()
        .equals("com.google.j2cl.transpiler.integration.multipleanonymousclass.Main$2");
  }
}
