package com.google.j2cl.transpiler.integration.fieldshadowing;

/** Test field shadowing and super field access. */
public class Main {

  interface IntSupplier {
    int get();
  }

  private static class Foo {
    int value = 1;
  }

  private static class SubFoo extends Foo {
    int value = 2;

    int value() {
      return value;
    }

    int superValue() {
      return super.value;
    }

    private class InnerSubFoo {
      public int value() {
        return value;
      }

      public int superValue() {
        return SubFoo.super.value;
      }
    }

    IntSupplier valueSupplier = () -> value;
    IntSupplier superValueSupplier = () -> super.value;
  }

  public static void main(String... args) {
    assert new SubFoo().value() == 2;
    assert new SubFoo().value == 2;
    assert new SubFoo().new InnerSubFoo().value() == 2;
    assert new SubFoo().valueSupplier.get() == 2;
    assert new SubFoo().value == 2;
    assert new SubFoo().superValue() == 1;
    assert new SubFoo().new InnerSubFoo().superValue() == 1;
    assert new SubFoo().superValueSupplier.get() == 1;
    assert new Foo().value == 1;
  }
}
