/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.transpiler.integration.instanceofs;

import java.io.Serializable;

/**
 * Test instanceof array.
 */
public class Main {
  public static void main(String... args) {
    testInstanceOf_class();
    testInstanceOf_interface();
    testInstanceOf_array();
    testInstanceOf_boxedTypes();
    testInstanceOf_string();
  }

  private static void testInstanceOf_class() {
    Object object = new SomeClass();
    assert object instanceof SomeClass;
    assert object instanceof Object;
    assert !(object instanceof String);
    assert "A String Literal" instanceof String;
    assert !(null instanceof Object);

    try {
      assert hasSideEffect() instanceof Object;
      assert false;
    } catch (IllegalArgumentException expected) {
    }

    try {
      assert hasSideEffect() instanceof ThreadLocal;
      assert false;
    } catch (IllegalArgumentException expected) {
    }
  }

  private static class SomeClass {}

  private static Object hasSideEffect() {
    throw new IllegalArgumentException();
  }

  private static void testInstanceOf_interface() {
    Object object = new Implementor();
    assert object instanceof ParentInterface;
    assert object instanceof ChildInterface;
    assert object instanceof GenericInterface;
    assert !(object instanceof Serializable);
  }

  public interface ParentInterface {}

  public interface ChildInterface extends ParentInterface {}

  public interface GenericInterface<T> {}

  private static class Implementor implements ChildInterface, GenericInterface<String> {}

  @SuppressWarnings("cast")
  private static void testInstanceOf_array() {
    Object object = new Object();
    assert object instanceof Object;
    assert !(object instanceof Object[]);
    assert !(object instanceof Object[][]);
    assert !(object instanceof String[]);
    assert !(object instanceof String[][]);
    assert !(object instanceof int[]);
    assert !(object instanceof Comparable);
    assert !(object instanceof Serializable);

    Object objects1d = new Object[1];
    assert objects1d instanceof Object;
    assert objects1d instanceof Object[];
    assert !(objects1d instanceof Object[][]);
    assert !(objects1d instanceof String[]);
    assert !(objects1d instanceof String[][]);
    assert !(objects1d instanceof int[]);
    assert !(objects1d instanceof Comparable);
    // TODO(b/117578563): Arrays should implement Serializable
    // assert objects1d instanceof Serializable;

    Object strings1d = new String[1];
    assert strings1d instanceof Object;
    assert strings1d instanceof Object[];
    assert !(strings1d instanceof Object[][]);
    assert strings1d instanceof String[];
    assert !(strings1d instanceof String[][]);
    assert !(strings1d instanceof int[]);
    assert !(strings1d instanceof Comparable);
    // TODO(b/117578563): Arrays should implement Serializable
    // assert strings1d instanceof Serializable;

    Object objects2d = new Object[1][1];
    assert objects2d instanceof Object;
    assert objects2d instanceof Object[];
    assert objects2d instanceof Object[][];
    assert !(objects2d instanceof String[]);
    assert !(objects2d instanceof String[][]);
    assert !(objects2d instanceof int[]);
    assert !(objects2d instanceof Comparable);
    // TODO(b/117578563): Arrays should implement Serializable
    // assert objects2d instanceof Serializable;

    Object strings2d = new String[1][1];
    assert strings2d instanceof Object;
    assert strings2d instanceof Object[];
    assert strings2d instanceof Object[][];
    assert !(strings2d instanceof String[]);
    assert strings2d instanceof String[][];
    assert !(strings2d instanceof int[]);
    assert !(strings2d instanceof Comparable);
    // TODO(b/117578563): Arrays should implement Serializable
    // assert strings2d instanceof Serializable;

    Object ints1d = new int[1];
    assert ints1d instanceof Object;
    assert !(ints1d instanceof Object[]);
    assert !(ints1d instanceof Object[][]);
    assert !(ints1d instanceof String[]);
    assert !(ints1d instanceof String[][]);
    assert ints1d instanceof int[];
    assert !(ints1d instanceof Comparable);
    // TODO(b/117578563): Arrays should implement Serializable
    // assert ints1d instanceof Serializable;

    Object ints2d = new int[1][];
    assert ints2d instanceof Object;
    assert ints2d instanceof Object[];
    assert !(ints2d instanceof Object[][]);
    assert !(ints2d instanceof String[]);
    assert !(ints2d instanceof String[][]);
    assert !(ints2d instanceof int[]);
    assert ints2d instanceof int[][];
    assert !(ints2d instanceof Comparable);
    // TODO(b/117578563): Arrays should implement Serializable
    //  assert ints2d instanceof Serializable;
  }

  @SuppressWarnings("BoxedPrimitiveConstructor")
  private static void testInstanceOf_boxedTypes() {
    Object o = new Byte((byte) 1);
    assert o instanceof Byte;
    assert !(o instanceof Double);
    assert !(o instanceof Float);
    assert !(o instanceof Integer);
    assert !(o instanceof Long);
    assert !(o instanceof Short);
    assert o instanceof Number;
    assert !(o instanceof Character);
    assert !(o instanceof Boolean);
    assert o instanceof Comparable;
    assert o instanceof Serializable;

    o = new Double(1.0);
    assert !(o instanceof Byte);
    assert o instanceof Double;
    assert !(o instanceof Float);
    assert !(o instanceof Integer);
    assert !(o instanceof Long);
    assert !(o instanceof Short);
    assert o instanceof Number;
    assert !(o instanceof Character);
    assert !(o instanceof Boolean);
    assert o instanceof Comparable;
    // TODO(b/117578563): Double should implement Serializable
    // assert o instanceof Serializable;

    o = new Float(1.0f);
    assert !(o instanceof Byte);
    assert !(o instanceof Double);
    assert o instanceof Float;
    assert !(o instanceof Integer);
    assert !(o instanceof Long);
    assert !(o instanceof Short);
    assert o instanceof Number;
    assert !(o instanceof Character);
    assert !(o instanceof Boolean);
    assert o instanceof Comparable;
    assert o instanceof Serializable;

    o = new Integer(1);
    assert !(o instanceof Byte);
    assert !(o instanceof Double);
    assert !(o instanceof Float);
    assert o instanceof Integer;
    assert !(o instanceof Long);
    assert !(o instanceof Short);
    assert o instanceof Number;
    assert !(o instanceof Character);
    assert !(o instanceof Boolean);
    assert o instanceof Comparable;
    assert o instanceof Serializable;

    o = new Long(1L);
    assert !(o instanceof Byte);
    assert !(o instanceof Double);
    assert !(o instanceof Float);
    assert !(o instanceof Integer);
    assert o instanceof Long;
    assert !(o instanceof Short);
    assert o instanceof Number;
    assert !(o instanceof Character);
    assert !(o instanceof Boolean);
    assert o instanceof Comparable;
    assert o instanceof Serializable;

    o = new Short((short) 1);
    assert !(o instanceof Byte);
    assert !(o instanceof Double);
    assert !(o instanceof Float);
    assert !(o instanceof Integer);
    assert !(o instanceof Long);
    assert o instanceof Short;
    assert o instanceof Number;
    assert !(o instanceof Character);
    assert !(o instanceof Boolean);
    assert o instanceof Comparable;
    assert o instanceof Serializable;

    o = new Character('a');
    assert !(o instanceof Byte);
    assert !(o instanceof Double);
    assert !(o instanceof Float);
    assert !(o instanceof Integer);
    assert !(o instanceof Long);
    assert !(o instanceof Short);
    assert !(o instanceof Number);
    assert o instanceof Character;
    assert !(o instanceof Boolean);
    assert o instanceof Comparable;
    assert o instanceof Serializable;

    o = new Boolean(true);
    assert !(o instanceof Byte);
    assert !(o instanceof Double);
    assert !(o instanceof Float);
    assert !(o instanceof Integer);
    assert !(o instanceof Long);
    assert !(o instanceof Short);
    assert !(o instanceof Number);
    assert !(o instanceof Character);
    assert o instanceof Boolean;
    assert o instanceof Comparable;
    // TODO(b/117578563): Boolean should implement Serializable
    // assert o instanceof Serializable;

    o = new NumberSubclass();
    assert (o instanceof NumberSubclass);
    assert (o instanceof Number);
    assert o instanceof Serializable;

    assert (!(new Object() instanceof Void));
    assert (!(null instanceof Void));
  }

  private static class NumberSubclass extends Number {
    @Override
    public int intValue() {
      return 0;
    }

    @Override
    public long longValue() {
      return 0;
    }

    @Override
    public float floatValue() {
      return 0;
    }

    @Override
    public double doubleValue() {
      return 0;
    }
  }

  private static void testInstanceOf_string() {
    Object s = "A string";
    assert !(s instanceof Byte);
    assert !(s instanceof Double);
    assert !(s instanceof Float);
    assert !(s instanceof Integer);
    assert !(s instanceof Long);
    assert !(s instanceof Short);
    assert !(s instanceof Number);
    assert !(s instanceof Character);
    assert !(s instanceof Boolean);
    assert s instanceof String;
    assert s instanceof Comparable;
    // TODO(b/117578563): String should implement Serializable
    // assert s instanceof Serializable;
  }
}
