package com.google.j2cl.transpiler.readable.gwtincompatible;

import com.google.common.annotations.GwtIncompatible;

interface Interface {
  @GwtIncompatible
  void incompatible();

  interface NestedInterface {
    @GwtIncompatible
    void nestedIncompatible();
  }

  abstract class ClassWithAbstractMethod {
    @GwtIncompatible
    public abstract void incompatibleFromClass();
  }
}
