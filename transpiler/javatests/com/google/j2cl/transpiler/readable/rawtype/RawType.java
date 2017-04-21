package com.google.j2cl.transpiler.readable.rawtype;

@SuppressWarnings("rawtypes")
class RawType {
  Comparable c;

  interface I<T extends RawType> {
    default void f(T t) {}
  }

  static class RawSubclass implements I {
    @Override
    public void f(RawType t) {}
  }
}
