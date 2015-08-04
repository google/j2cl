package com.google.j2cl.transpiler.readable.simpleenum;

public enum SimpleEnum {
  V1,
  V2
}

enum SimpleEnum2 {
  VALUE1(2),
  VALUE2(SimpleEnum.V1),
  VALUE3(5) {};

  int foo = SimpleEnum.V1.ordinal();

  SimpleEnum2(int foo) {
    this.foo = foo;
  }

  SimpleEnum2(SimpleEnum foo) {
    this(foo.ordinal());
  }
}
