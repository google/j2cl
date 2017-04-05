package com.google.j2cl.transpiler.readable.defaultmethods;

public class DefaultMethods {
  interface List<T> {
    default void add(T elem) {}
    default void remove(T[] elems) {}
    default List<T> sublist(int startIndex, int endIndex) {
      return null;
    }
  }

  static class AConcreteList<T> implements List<T> {}

  static class StringList implements List<String> {}
}
