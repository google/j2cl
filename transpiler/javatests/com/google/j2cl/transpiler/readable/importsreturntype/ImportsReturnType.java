package com.google.j2cl.transpiler.readable.importsreturntype;

public class ImportsReturnType {
  public static class Entry {}

  public static class Set<S> {}

  public Set<Entry> get() {
    return null;
  }
}
