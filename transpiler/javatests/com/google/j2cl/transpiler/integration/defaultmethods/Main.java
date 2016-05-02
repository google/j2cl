/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.integration.defaultmethods;

/**
 * Tests Java 8 default methods.
 */
public class Main {

  public static final int COLLECTION_ADD = 1;
  public static final int LIST_ADD = 2;
  public static final int ABSTRACT_COLLECTION_ADD = 3;
  public static final int ANOTHER_STRING_LIST_ADD = 4;
  public static final int ANOTHER_LIST_INTERFACE_ADD = 5;

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

  abstract static class AbstractCollection<T> implements Collection<T> {
    public int add(T elem) {
      assert this instanceof AbstractCollection;
      return ABSTRACT_COLLECTION_ADD;
    }
  }

  static class ACollection<T> implements Collection<T> {}

  abstract static class AbstractList<T> extends AbstractCollection<T> implements List<T> {}

  static class AConcreteList<T> extends AbstractList<T> {}

  static class SomeOtherCollection<T> implements Collection<T> {}

  static class SomeOtherList<T> extends SomeOtherCollection<T> implements List<T> {}

  // Should inherit List.add  even though the interface is not directly declared.
  static class YetAnotherList<T> extends SomeOtherList<T> implements Collection<T> {}

  static class StringList implements List<String> {}

  static class AnotherStringList implements List<String> {
    public int add(String elem) {
      return ANOTHER_STRING_LIST_ADD;
    }
  }

  interface AnotherListInterface<T> {
    default int add(T elem) {
      assert this instanceof AnotherListInterface;
      return ANOTHER_LIST_INTERFACE_ADD;
    }
  }

  static class AnotherCollection<T> implements List<T>, AnotherListInterface<T> {
    public int add(T elem) {
      return AnotherListInterface.super.add(elem);
    }
  }

  public static void main(String... args) {
    assert new ACollection<Object>().add(null) == COLLECTION_ADD;
    assert new AConcreteList<Object>().add(null) == ABSTRACT_COLLECTION_ADD;
    assert new SomeOtherCollection<Object>().add(null) == COLLECTION_ADD;
    assert new SomeOtherList<Object>().add(null) == LIST_ADD;
    assert new YetAnotherList<Object>().add(null) == LIST_ADD;
    assert new StringList().add(null) == LIST_ADD;
    assert new AnotherStringList().add(null) == ANOTHER_STRING_LIST_ADD;
    assert new AnotherCollection<Object>().add(null) == ANOTHER_LIST_INTERFACE_ADD;
  }
}
