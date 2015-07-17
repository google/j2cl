package com.google.j2cl.transpiler.integration.enums;

public class Main {

  static enum Foo {
    FOO,
    FOZ
  }

  static enum Bar {
    BAR(1),
    BAZ(Foo.FOO);

    int f;

    Bar(int i) {
      f = i;
    }

    Bar(Foo f) {
      this(f.ordinal());
    }

    int getF() {
      return f;
    }
  }

  public static void main(String[] args) {
    assert Foo.FOO.ordinal() == 0;
    assert Foo.FOO.name().equals("FOO");

    assert Foo.FOZ.ordinal() == 1;
    assert Foo.FOZ.name().equals("FOZ");

    assert Bar.BAR.ordinal() == 0;
    assert Bar.BAR.getF() == 1;
    assert Bar.BAR.name().equals("BAR");

    assert Bar.BAZ.ordinal() == 1;
    assert Bar.BAZ.getF() == 0;
    assert Bar.BAZ.name().equals("BAZ");
  }
}
