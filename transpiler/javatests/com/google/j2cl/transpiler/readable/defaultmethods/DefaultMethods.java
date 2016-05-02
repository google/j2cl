package com.google.j2cl.transpiler.readable.defaultmethods;

public class DefaultMethods {
  interface List<T> {
    default void add(T elem) {}
  }

  static class AConcreteList<T> implements List<T> {}

  static class StringList implements List<String> {}
}
