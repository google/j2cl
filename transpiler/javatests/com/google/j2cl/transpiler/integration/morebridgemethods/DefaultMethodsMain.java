package com.google.j2cl.transpiler.integration.morebridgemethods;

public class DefaultMethodsMain {

  public static final int COLLECTION_ADD = 1;
  public static final int LIST_ADD = 2;

  interface Collection<T> {
    default int add(T elem) {
      assert this instanceof Collection;
      return COLLECTION_ADD;
    }
  }

  interface List<T> extends Collection<T> {
    default int add(T elem) {
      assert this instanceof List;
      return LIST_ADD;
    }
  }

  static class SomeOtherCollection<T> implements Collection<T> {}

  static class SomeOtherList<T> extends SomeOtherCollection<T> implements List<T> {}

  // Should inherit List.add  even though the interface is not directly declared.
  static class YetAnotherList<T> extends SomeOtherList<T> implements Collection<T> {}

  public static void test() {
    assert new YetAnotherList<Object>().add(null) == LIST_ADD;
  }
}
