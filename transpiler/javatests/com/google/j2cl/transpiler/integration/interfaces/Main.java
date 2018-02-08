/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.integration.interfaces;

/** Test basic interface functionality. */
@SuppressWarnings("StaticQualifiedUsingExpression")
public class Main {
  public interface SomeInterface {
    int run();
  }

  private static class SomeClass implements SomeInterface {
    @Override
    public int run() {
      return 1;
    }
  }

  public static int run(SomeInterface someInterface) {
    return someInterface.run();
  }

  public static void testInterfaceDispatch() {
    SomeClass s = new SomeClass();
    assert run(s) == 1;
  }

  public interface InterfaceWithFields {
    public int A = 1;
    public static int B = 2;
  }

  public static void testInterfaceWithFields() {
    assert InterfaceWithFields.A == 1;
    assert InterfaceWithFields.B == 2;
    InterfaceWithFields i = new InterfaceWithFields() {};
    assert i.A == 1;
    assert i.B == 2;
  }

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
    @Override
    default int add(T elem) {
      assert this instanceof List;
      return LIST_ADD;
    }

    static int returnOne() {
      return 1;
    }
  }

  abstract static class AbstractCollection<T> implements Collection<T> {
    @Override
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
    @Override
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
    @Override
    public int add(T elem) {
      return AnotherListInterface.super.add(elem);
    }
  }

  public static void testDefaultMethods() {
    assert new ACollection<Object>().add(null) == COLLECTION_ADD;
    assert new AConcreteList<Object>().add(null) == ABSTRACT_COLLECTION_ADD;
    assert new SomeOtherCollection<Object>().add(null) == COLLECTION_ADD;
    assert new SomeOtherList<Object>().add(null) == LIST_ADD;
    assert new YetAnotherList<Object>().add(null) == LIST_ADD;
    assert new StringList().add(null) == LIST_ADD;
    assert new AnotherStringList().add(null) == ANOTHER_STRING_LIST_ADD;
    assert new AnotherCollection<Object>().add(null) == ANOTHER_LIST_INTERFACE_ADD;
  }

  public static void testStaticMethods() {
    assert List.returnOne() == 1;
  }

  interface InterfaceWithPrivateMethods {
    default int defaultMethod() {
      return privateInstanceMethod();
    }
    private int privateInstanceMethod() {
      return m();
    }
    private static int privateStaticMethod() {
      return ((InterfaceWithPrivateMethods) () -> 1).privateInstanceMethod() + 1;
    }

    int m();

    static int callPrivateStaticMethod() {
      return privateStaticMethod();
    }
  }
  public static void testPrivateMethods() {
    assert InterfaceWithPrivateMethods.callPrivateStaticMethod() == 2;
    assert ((InterfaceWithPrivateMethods) () -> 1).defaultMethod() == 1;
  }

  public static void main(String[] args) {
    testInterfaceDispatch();
    testInterfaceWithFields();
    testDefaultMethods();
    testStaticMethods();
    testPrivateMethods();
  }
}
