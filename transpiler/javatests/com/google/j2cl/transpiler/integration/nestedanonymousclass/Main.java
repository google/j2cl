package com.google.j2cl.transpiler.integration.nestedanonymousclass;

/**
 * Test nested anonymous class.
 */
interface AnonymousInterface {
  public void foo();
}

public class Main {
  public static void main(String... args) {
    AnonymousInterface intf1 =
        new AnonymousInterface() {
          @Override
          public void foo() {
            AnonymousInterface intf2 =
                new AnonymousInterface() {
                  @Override
                  public void foo() {}
                };
            assert intf2 instanceof AnonymousInterface;
            assert intf2
                .getClass()
                .getName()
                .equals("com.google.j2cl.transpiler.integration.nestedanonymousclass.Main$1$1");
          }
        };

    assert intf1 instanceof AnonymousInterface;
    assert intf1
        .getClass()
        .getName()
        .equals("com.google.j2cl.transpiler.integration.nestedanonymousclass.Main$1");
    intf1.foo();
  }
}
