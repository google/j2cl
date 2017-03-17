package com.google.j2cl.transpiler.readable.defaultmethods;

public class DefaultMethods {
  interface List<T> {
    default void add(T elem) {}
    default void remove(T[] elems) {}
  }

  static class AConcreteList<T> implements List<T> {}

  static class StringList implements List<String> {}
}
