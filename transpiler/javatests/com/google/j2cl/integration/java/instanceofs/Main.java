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
package instanceofs;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isWasm;

import java.io.Serializable;

/** Test instanceof array. */
@SuppressWarnings("BadInstanceof")
public class Main {
  public static void main(String... args) {
    testInstanceOf_class();
    testInstanceOf_interface();
    testInstanceOf_array();
    testInstanceOf_boxedTypes();
    testInstanceOf_string();
    testInstanceOf_sideEffects();
    testInstanceOf_markerInterfaces();
  }

  private static void testInstanceOf_class() {
    class SomeClass {}
    Object object = new SomeClass();
    assertTrue(object instanceof SomeClass);
    assertTrue(object instanceof Object);
    assertTrue(!(object instanceof String));
    assertTrue("A String Literal" instanceof String);
    assertTrue(!(null instanceof Object));
  }

  private static void testInstanceOf_sideEffects() {
    counter = 0;
    assertTrue(incrementCounter() instanceof Object);
    assertEquals(1, counter);
    assertFalse(incrementCounter() instanceof Number);
    assertEquals(2, counter);
    assertFalse(incrementCounter() instanceof Serializable);
    assertEquals(3, counter);
  }

  private static int counter = 0;

  private static Object incrementCounter() {
    counter++;
    return new Object();
  }

  private static void testInstanceOf_interface() {
    Object object = new Implementor();
    assertTrue(object instanceof ParentInterface);
    assertTrue(object instanceof ChildInterface);
    assertTrue(object instanceof GenericInterface);
    assertFalse(object instanceof Serializable);
    assertFalse(object instanceof Cloneable);

    // Serializable and Cloneable have custom isInstance implementations; make sure those
    // still behave correctly when classes implement the interface.
    object = new Serializable() {};
    assertTrue(object instanceof Serializable);

    object = new Cloneable() {};
    assertTrue(object instanceof Cloneable);
  }

  public interface ParentInterface {
    default void dummy() {}
    ;
  }

  public interface ChildInterface extends ParentInterface {
    default void dummy(long l) {}
    ;
  }

  public interface GenericInterface<T> {
    default void dummy(int i) {}
    ;
  }

  private static class Implementor implements ChildInterface, GenericInterface<String> {}

  @SuppressWarnings("cast")
  private static void testInstanceOf_array() {
    // TODO(b/184675805): Enable for Wasm when array metadata is fully implemented.
    if (isWasm()) {
      return;
    }
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

    Object intsLiteral = new int[] {1, 2, 3};
    assertTrue(intsLiteral instanceof Object);
    assertTrue(!(intsLiteral instanceof Object[]));
    assertTrue(!(intsLiteral instanceof Object[][]));
    assertTrue(!(intsLiteral instanceof String[]));
    assertTrue(!(intsLiteral instanceof String[][]));
    assertTrue(intsLiteral instanceof int[]);
    assertTrue(!(intsLiteral instanceof Comparable));
    assertTrue(intsLiteral instanceof Serializable);
    assertTrue(intsLiteral instanceof Cloneable);
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

  private static void testInstanceOf_markerInterfaces() {
    class A implements MarkerA {}
    class B implements MarkerB {}

    Object a = new A();
    assertTrue(a instanceof MarkerA);
    assertFalse(a instanceof MarkerB);

    Object b = new B();
    assertTrue(b instanceof MarkerB);
    assertFalse(b instanceof MarkerA);
  }

  interface MarkerA {}

  interface MarkerB {}
}
