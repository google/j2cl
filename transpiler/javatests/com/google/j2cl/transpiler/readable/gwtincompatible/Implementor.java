package com.google.j2cl.transpiler.readable.gwtincompatible;

import com.google.common.annotations.GwtIncompatible;

public class Implementor extends Interface.ClassWithAbstractMethod
    implements Interface, Interface.NestedInterface {
  @GwtIncompatible
  @Override
  public void incompatible() {}

  @GwtIncompatible
  @Override
  public void nestedIncompatible() {}

  @GwtIncompatible
  @Override
  public void incompatibleFromClass() {}

  public void compatibleMethod() {
    int a = 4; // Show that the source map line numbers are preserved correctly.
  }

  enum SomeEnum {
    COMPATIBLE {
      @Override
      void method() {}
    },
    @GwtIncompatible
    INCOMPATIBLE {
      @Override
      void method() {}
    };

    abstract void method();
  }
}
