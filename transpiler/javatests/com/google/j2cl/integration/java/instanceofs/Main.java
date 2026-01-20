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
import static com.google.j2cl.integration.testing.TestUtils.isJ2Kt;
import static com.google.j2cl.integration.testing.TestUtils.isJ2KtNative;
import static com.google.j2cl.integration.testing.TestUtils.isWasm;

import java.io.Serializable;
import java.util.function.Supplier;

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
    testInstanceOf_patternVariable();
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
    assertTrue(incrementCounter() instanceof Comparable);
    assertEquals(3, counter);

    class LocalClass {
      int i = incrementCounter() instanceof String s ? s.length() : 0;
      boolean unused = incrementCounter() instanceof String unusedString;
    }
    assertEquals("Hello".length(), new LocalClass().i);
    assertEquals(5, counter);

    Supplier<Boolean> supplier = () -> incrementCounter() instanceof String unused;
    assertTrue(supplier.get());
    assertEquals(6, counter);
  }

  private static int counter = 0;

  private static Object incrementCounter() {
    counter++;
    return "Hello";
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

    // J2KT requires explicit clone() override.
    object =
        new Cloneable() {
          protected Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
          }
        };
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
    // TODO(b/423010387): instanceof X[] always returns true in J2KT for any reference array
    //  regardless of its type (here and below).
    if (!isJ2Kt()) {
      assertTrue(!(objects1d instanceof Object[][]));
      assertTrue(!(objects1d instanceof String[]));
      assertTrue(!(objects1d instanceof String[][]));
    }
    assertTrue(!(objects1d instanceof int[]));
    assertTrue(!(objects1d instanceof Comparable));
    // TODO(b/267597125): Kotlin Array is not Serializable/Cloneable (here and below)
    if (!isJ2KtNative()) {
      assertTrue(objects1d instanceof Serializable);
      assertTrue(objects1d instanceof Cloneable);
    }

    Object strings1d = new String[1];
    assertTrue(strings1d instanceof Object);
    assertTrue(strings1d instanceof Object[]);
    if (!isJ2Kt()) {
      assertTrue(!(strings1d instanceof Object[][]));
      assertTrue(strings1d instanceof String[]);
      assertTrue(!(strings1d instanceof String[][]));
    }
    assertTrue(!(strings1d instanceof int[]));
    assertTrue(!(strings1d instanceof Comparable));
    if (!isJ2KtNative()) {
      assertTrue(strings1d instanceof Serializable);
      assertTrue(strings1d instanceof Cloneable);
    }

    Object objects2d = new Object[1][1];
    assertTrue(objects2d instanceof Object);
    assertTrue(objects2d instanceof Object[]);
    if (!isJ2Kt()) {
      assertTrue(objects2d instanceof Object[][]);
      assertTrue(!(objects2d instanceof String[]));
      assertTrue(!(objects2d instanceof String[][]));
    }
    assertTrue(!(objects2d instanceof int[]));
    assertTrue(!(objects2d instanceof Comparable));
    if (!isJ2KtNative()) {
      assertTrue(objects2d instanceof Serializable);
      assertTrue(objects2d instanceof Cloneable);
    }

    Object strings2d = new String[1][1];
    assertTrue(strings2d instanceof Object);
    assertTrue(strings2d instanceof Object[]);
    if (!isJ2Kt()) {
      assertTrue(strings2d instanceof Object[][]);
      assertTrue(!(strings2d instanceof String[]));
      assertTrue(strings2d instanceof String[][]);
    }
    assertTrue(!(strings2d instanceof int[]));
    assertTrue(!(strings2d instanceof Comparable));
    if (!isJ2KtNative()) {
      assertTrue(strings2d instanceof Serializable);
      assertTrue(strings2d instanceof Cloneable);
    }

    Object ints1d = new int[1];
    assertTrue(ints1d instanceof Object);
    assertTrue(!(ints1d instanceof Object[]));
    assertTrue(!(ints1d instanceof Object[][]));
    assertTrue(!(ints1d instanceof String[]));
    assertTrue(!(ints1d instanceof String[][]));
    assertTrue(ints1d instanceof int[]);
    assertTrue(!(ints1d instanceof Comparable));
    if (!isJ2KtNative()) {
      assertTrue(ints1d instanceof Serializable);
      assertTrue(ints1d instanceof Cloneable);
    }

    Object ints2d = new int[1][];
    assertTrue(ints2d instanceof Object);
    assertTrue(ints2d instanceof Object[]);
    if (!isJ2Kt()) {
      assertTrue(!(ints2d instanceof Object[][]));
      assertTrue(!(ints2d instanceof String[]));
      assertTrue(!(ints2d instanceof String[][]));
    }
    assertTrue(!(ints2d instanceof int[]));
    assertTrue(ints2d instanceof int[][]);
    assertTrue(!(ints2d instanceof Comparable));
    if (!isJ2KtNative()) {
      assertTrue(ints2d instanceof Serializable);
      assertTrue(ints2d instanceof Cloneable);
    }

    Object intsLiteral = new int[] {1, 2, 3};
    assertTrue(intsLiteral instanceof Object);
    assertTrue(!(intsLiteral instanceof Object[]));
    assertTrue(!(intsLiteral instanceof Object[][]));
    assertTrue(!(intsLiteral instanceof String[]));
    assertTrue(!(intsLiteral instanceof String[][]));
    assertTrue(intsLiteral instanceof int[]);
    if (!isJ2KtNative()) {
      assertTrue(!(intsLiteral instanceof Comparable));
      assertTrue(intsLiteral instanceof Serializable);
      assertTrue(intsLiteral instanceof Cloneable);
    }
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
    // TODO(b/267597125): Boxed types are not Serializable (here and below)
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
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
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
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
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
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
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
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
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
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
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
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
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
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
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
    assertTrue(!(o instanceof Cloneable));

    o = new NumberSubclass();
    assertTrue((o instanceof NumberSubclass));
    assertTrue((o instanceof Number));
    if (!isJ2KtNative()) {
      assertTrue(o instanceof Serializable);
    }
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

    // TODO(b/267597125): String is not Serializable
    if (!isJ2KtNative()) {
      assertTrue(s instanceof Serializable);
    }
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

  private static void testInstanceOf_patternVariable() {
    String hello = "hello";
    Object o = hello;
    assertTrue(o instanceof String s && s.length() == hello.length());
    assertTrue(hello.length() == (o instanceof String s ? s.length() : 0));

    String bye = "bye";
    o = bye;
    if (!(o instanceof String s)) {
      throw new AssertionError();
    }
    // The variable s is in scope here.
    assertEquals(bye.length(), s.length());

    o = Integer.valueOf(1);
    while (o instanceof Number n) {
      assertEquals(1, n.intValue());
      break;
    }

    do {
      o = Integer.valueOf(2);
    } while (!(o instanceof Number n));
    assertEquals(2, n.intValue());
  }
}
