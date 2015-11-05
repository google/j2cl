package com.google.j2cl.transpiler.integration.depsdeep.foo;

import com.google.j2cl.transpiler.integration.depsdeep.bar.Bar;

public class Foo {
  public Bar bar = new Bar();

  public String getName() {
    return "Foo";
  }
}
