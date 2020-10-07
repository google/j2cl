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
package com.google.j2cl.integration.instanceofs;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

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
    assertTrue(object instanceof SomeClass);
    assertTrue(object instanceof Object);
    assertTrue(!(object instanceof String));
    assertTrue("A String Literal" instanceof String);
    assertTrue(!(null instanceof Object));

    try {
      assertTrue(hasSideEffect() instanceof Object);
      assertTrue(false);
    } catch (IllegalArgumentException expected) {
    }

    try {
      assertTrue(hasSideEffect() instanceof ThreadLocal);
      assertTrue(false);
    } catch (IllegalArgumentException expected) {
    }
  }

  private static class SomeClass {}

  private static Object hasSideEffect() {
    throw new IllegalArgumentException();
  }

  private static void testInstanceOf_interface() {
    Object object = new Implementor();
    assertTrue(object instanceof ParentInterface);
    assertTrue(object instanceof ChildInterface);
    assertTrue(object instanceof GenericInterface);
    assertTrue(!(object instanceof Serializable));
    assertTrue(!(object instanceof Cloneable));

    // Serializable and Cloneable have custom isInstance implementations; make sure those
    // still behave correctly when classes implement the interface.
    object = new Serializable() {};
    assertTrue(object instanceof Serializable);

    object = new Cloneable() {};
    assertTrue(object instanceof Cloneable);
  }

  public interface ParentInterface {}

  public interface ChildInterface extends ParentInterface {}

  public interface GenericInterface<T> {}

  private static class Implementor implements ChildInterface, GenericInterface<String> {}

  @SuppressWarnings("cast")
  private static void testInstanceOf_array() {
    Object object = new Object();
    assertTrue(object instanceof Object);
    assertTrue(!(object instanceof Object[]));
    assertTrue(!(object instanceof Object[][]));
    assertTrue(!(object instanceof String[]));
    assertTrue(!(object instanceof String[][]));
    assertTrue(!(object instanceof int[]));
    assertTrue(!(object instanceof Comparable));
    assertTrue(!(object instanceof Serializable));
    assertTrue(!(object instanceof Cloneable));

    Object objects1d = new Object[1];
    assertTrue(objects1d instanceof Object);
    assertTrue(objects1d instanceof Object[]);
    assertTrue(!(objects1d instanceof Object[][]));
    assertTrue(!(objects1d instanceof String[]));
    assertTrue(!(objects1d instanceof String[][]));
    assertTrue(!(objects1d instanceof int[]));
    assertTrue(!(objects1d instanceof Comparable));
    assertTrue(objects1d instanceof Serializable);
    assertTrue(objects1d instanceof Cloneable);

    Object strings1d = new String[1];
    assertTrue(strings1d instanceof Object);
    assertTrue(strings1d instanceof Object[]);
    assertTrue(!(strings1d instanceof Object[][]));
    assertTrue(strings1d instanceof String[]);
    assertTrue(!(strings1d instanceof String[][]));
    assertTrue(!(strings1d instanceof int[]));
    assertTrue(!(strings1d instanceof Comparable));
    assertTrue(strings1d instanceof Serializable);
    assertTrue(strings1d instanceof Cloneable);

    Object objects2d = new Object[1][1];
    assertTrue(objects2d instanceof Object);
    assertTrue(objects2d instanceof Object[]);
    assertTrue(objects2d instanceof Object[][]);
    assertTrue(!(objects2d instanceof String[]));
    assertTrue(!(objects2d instanceof String[][]));
    assertTrue(!(objects2d instanceof int[]));
    assertTrue(!(objects2d instanceof Comparable));
    assertTrue(objects2d instanceof Serializable);
    assertTrue(objects2d instanceof Cloneable);

    Object strings2d = new String[1][1];
    assertTrue(strings2d instanceof Object);
    assertTrue(strings2d instanceof Object[]);
    assertTrue(strings2d instanceof Object[][]);
    assertTrue(!(strings2d instanceof String[]));
    assertTrue(strings2d instanceof String[][]);
    assertTrue(!(strings2d instanceof int[]));
    assertTrue(!(strings2d instanceof Comparable));
    assertTrue(strings2d instanceof Serializable);
    assertTrue(strings2d instanceof Cloneable);

    Object ints1d = new int[1];
    assertTrue(ints1d instanceof Object);
    assertTrue(!(ints1d instanceof Object[]));
    assertTrue(!(ints1d instanceof Object[][]));
    assertTrue(!(ints1d instanceof String[]));
    assertTrue(!(ints1d instanceof String[][]));
    assertTrue(ints1d instanceof int[]);
    assertTrue(!(ints1d instanceof Comparable));
    assertTrue(ints1d instanceof Serializable);
    assertTrue(ints1d instanceof Cloneable);

    Object ints2d = new int[1][];
    assertTrue(ints2d instanceof Object);
    assertTrue(ints2d instanceof Object[]);
    assertTrue(!(ints2d instanceof Object[][]));
    assertTrue(!(ints2d instanceof String[]));
    assertTrue(!(ints2d instanceof String[][]));
    assertTrue(!(ints2d instanceof int[]));
    assertTrue(ints2d instanceof int[][]);
    assertTrue(!(ints2d instanceof Comparable));
    assertTrue(ints2d instanceof Serializable);
    assertTrue(ints2d instanceof Cloneable);
  }

  @SuppressWarnings("BoxedPrimitiveConstructor")
  private static void testInstanceOf_boxedTypes() {
    Object o = new Byte((byte) 1);
    assertTrue(o instanceof Byte);
    assertTrue(!(o instanceof Double));
    assertTrue(!(o instanceof Float));
    assertTrue(!(o instanceof Integer));
    assertTrue(!(o instanceof Long));
    assertTrue(!(o instanceof Short));
    assertTrue(o instanceof Number);
    assertTrue(!(o instanceof Character));
    assertTrue(!(o instanceof Boolean));
    assertTrue(o instanceof Comparable);
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    o = new Double(1.0);
    assertTrue(!(o instanceof Byte));
    assertTrue(o instanceof Double);
    assertTrue(!(o instanceof Float));
    assertTrue(!(o instanceof Integer));
    assertTrue(!(o instanceof Long));
    assertTrue(!(o instanceof Short));
    assertTrue(o instanceof Number);
    assertTrue(!(o instanceof Character));
    assertTrue(!(o instanceof Boolean));
    assertTrue(o instanceof Comparable);
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    o = new Float(1.0f);
    assertTrue(!(o instanceof Byte));
    assertTrue(!(o instanceof Double));
    assertTrue(o instanceof Float);
    assertTrue(!(o instanceof Integer));
    assertTrue(!(o instanceof Long));
    assertTrue(!(o instanceof Short));
    assertTrue(o instanceof Number);
    assertTrue(!(o instanceof Character));
    assertTrue(!(o instanceof Boolean));
    assertTrue(o instanceof Comparable);
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    o = new Integer(1);
    assertTrue(!(o instanceof Byte));
    assertTrue(!(o instanceof Double));
    assertTrue(!(o instanceof Float));
    assertTrue(o instanceof Integer);
    assertTrue(!(o instanceof Long));
    assertTrue(!(o instanceof Short));
    assertTrue(o instanceof Number);
    assertTrue(!(o instanceof Character));
    assertTrue(!(o instanceof Boolean));
    assertTrue(o instanceof Comparable);
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    o = new Long(1L);
    assertTrue(!(o instanceof Byte));
    assertTrue(!(o instanceof Double));
    assertTrue(!(o instanceof Float));
    assertTrue(!(o instanceof Integer));
    assertTrue(o instanceof Long);
    assertTrue(!(o instanceof Short));
    assertTrue(o instanceof Number);
    assertTrue(!(o instanceof Character));
    assertTrue(!(o instanceof Boolean));
    assertTrue(o instanceof Comparable);
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    o = new Short((short) 1);
    assertTrue(!(o instanceof Byte));
    assertTrue(!(o instanceof Double));
    assertTrue(!(o instanceof Float));
    assertTrue(!(o instanceof Integer));
    assertTrue(!(o instanceof Long));
    assertTrue(o instanceof Short);
    assertTrue(o instanceof Number);
    assertTrue(!(o instanceof Character));
    assertTrue(!(o instanceof Boolean));
    assertTrue(o instanceof Comparable);
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    o = new Character('a');
    assertTrue(!(o instanceof Byte));
    assertTrue(!(o instanceof Double));
    assertTrue(!(o instanceof Float));
    assertTrue(!(o instanceof Integer));
    assertTrue(!(o instanceof Long));
    assertTrue(!(o instanceof Short));
    assertTrue(!(o instanceof Number));
    assertTrue(o instanceof Character);
    assertTrue(!(o instanceof Boolean));
    assertTrue(o instanceof Comparable);
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    o = new Boolean(true);
    assertTrue(!(o instanceof Byte));
    assertTrue(!(o instanceof Double));
    assertTrue(!(o instanceof Float));
    assertTrue(!(o instanceof Integer));
    assertTrue(!(o instanceof Long));
    assertTrue(!(o instanceof Short));
    assertTrue(!(o instanceof Number));
    assertTrue(!(o instanceof Character));
    assertTrue(o instanceof Boolean);
    assertTrue(o instanceof Comparable);
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    o = new NumberSubclass();
    assertTrue((o instanceof NumberSubclass));
    assertTrue((o instanceof Number));
    assertTrue(o instanceof Serializable);
    assertTrue(!(o instanceof Cloneable));

    assertTrue((!(new Object() instanceof Void)));
    assertTrue((!(null instanceof Void)));
    assertTrue(!(null instanceof Cloneable));
    assertTrue(!(null instanceof Serializable));
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
    assertTrue(!(s instanceof Byte));
    assertTrue(!(s instanceof Double));
    assertTrue(!(s instanceof Float));
    assertTrue(!(s instanceof Integer));
    assertTrue(!(s instanceof Long));
    assertTrue(!(s instanceof Short));
    assertTrue(!(s instanceof Number));
    assertTrue(!(s instanceof Character));
    assertTrue(!(s instanceof Boolean));
    assertTrue(s instanceof String);
    assertTrue(s instanceof Comparable);
    assertTrue(s instanceof Serializable);
    assertTrue(!(s instanceof Cloneable));
  }
}
