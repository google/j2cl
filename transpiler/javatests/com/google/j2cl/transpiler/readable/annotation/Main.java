package com.google.j2cl.transpiler.readable.annotation;

public class Main {
  @interface Foo {
    int CONSTANT = 123;

    int bar() default CONSTANT;
  }

  class Bar {

    int baz() {
      Foo foo = null;
      foo.bar();
      return Foo.CONSTANT;
    }
  }
}
