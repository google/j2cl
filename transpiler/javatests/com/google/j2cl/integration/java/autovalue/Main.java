/*
 * Copyright 2020 Google Inc.
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
package autovalue;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.TestUtils.isJavaScript;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {

  public static void main(String... args) {
    testComposite();
    testUnreadValue();
    testAutovalueWithCtor();
    testExtending();
    testMemoized();
    testAbstractEquals();
    testJsCollection();
    testUnusedType();
    testUnusedTypeExtending();
    testClinit();
    testJsInterop();
  }

  private static void testComposite() {
    // Note that we created two copies of each class to increase variety and avoid potential
    // optimizations that wouldn't applicable in the real life.

    ComponentA componentA = new AutoValue_ComponentA(1, false, "hello", 42d, new int[] {1, 2, 3});
    assertEquals(1, componentA.getIntField());
    assertEquals(false, componentA.getBooleanField());
    assertEquals(42d, componentA.getDoubleField());
    assertEquals("hello", componentA.getStringField());
    assertEquals(2, componentA.getArrayField()[1]);
    CompositeA compositeA = new AutoValue_CompositeA(10, true, "world", 100d, componentA);
    assertEquals(10, compositeA.getIntField());
    assertEquals(true, compositeA.getBooleanField());
    assertEquals("world", compositeA.getStringField());
    assertEquals(100d, compositeA.getDoubleField());
    assertEquals(componentA, compositeA.getComponentField());
    assertEquals(componentA.hashCode(), compositeA.getComponentField().hashCode());
    assertNotNull(compositeA.toString());

    ComponentB componentB = new AutoValue_ComponentB(2, false, "hello", 43d, new int[] {5, 6, 7});
    assertEquals(2, componentB.getIntField());
    assertEquals(false, componentB.getBooleanField());
    assertEquals(43d, componentB.getDoubleField());
    assertEquals("hello", componentB.getStringField());
    assertEquals(6, componentB.getArrayField()[1]);
    CompositeB compositeB = new AutoValue_CompositeB(11, true, "world", 101d, componentB);
    assertEquals(11, compositeB.getIntField());
    assertEquals(true, compositeB.getBooleanField());
    assertEquals("world", compositeB.getStringField());
    assertEquals(101d, compositeB.getDoubleField());
    assertEquals(componentB, compositeB.getComponentField());
    assertEquals(componentB.hashCode(), compositeB.getComponentField().hashCode());
    assertNotNull(compositeB.toString());
  }

  @AutoValue
  protected abstract static class TypeWithUnreadField {
    public abstract int getUnreadField();

    // Having constructors ensures that we didn't miss the right one while preserving fields.

    TypeWithUnreadField(int foo) {}

    TypeWithUnreadField() {}
  }

  private static void testUnreadValue() {
    assertNotEquals(
        new AutoValue_Main_TypeWithUnreadField(5), new AutoValue_Main_TypeWithUnreadField(6));
  }

  @AutoValue
  protected abstract static class AutoValueWithCtors {
    public abstract int foo();

    int bar;

    AutoValueWithCtors(int zoo) {
      // Have an extra constructor to make sure correct constructor is called.
    }

    AutoValueWithCtors() {
      bar = 42;
    }
  }

  private static void testAutovalueWithCtor() {
    AutoValueWithCtors o1 = new AutoValue_Main_AutoValueWithCtors(12);
    assertEquals(42, o1.bar); // Make sure the parent constructor is still called.
    AutoValueWithCtors o2 = new AutoValue_Main_AutoValueWithCtors(12);
    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());

    o1.bar = 2;
    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());
  }

  private static class BaseClass {
    int bar;

    BaseClass() {
      bar = 42;
    }
  }

  @AutoValue
  protected abstract static class ExtendingAutoValue extends BaseClass {
    public abstract int foo();
  }

  private static void testExtending() {
    ExtendingAutoValue o1 = new AutoValue_Main_ExtendingAutoValue(12);
    assertEquals(42, o1.bar); // Make sure the parent constructor is still called.
    ExtendingAutoValue o2 = new AutoValue_Main_ExtendingAutoValue(12);
    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());

    o1.bar = 2;
    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());
  }

  @AutoValue
  protected abstract static class TypeWithMemoization {
    public abstract int foo();

    @Memoized
    public int memoized() {
      return foo() * 2;
    }
  }

  private static void testMemoized() {
    TypeWithMemoization o1 = new AutoValue_Main_TypeWithMemoization(11);
    TypeWithMemoization o2 = new AutoValue_Main_TypeWithMemoization(11);
    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());

    assertEquals(22, o2.memoized());
    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());
  }

  @AutoValue
  abstract static class AbstractEquals {
    abstract String string();

    @Override
    @SuppressWarnings("EqualsHashCode")
    public abstract boolean equals(Object o);
  }

  private static void testAbstractEquals() {
    AbstractEquals o1 = new AutoValue_Main_AbstractEquals("a");
    AbstractEquals o2 = new AutoValue_Main_AbstractEquals("a");
    assertEquals(o1, o2);
  }

  @AutoValue
  abstract static class UsesJsCollection {
    abstract JsArray jsArray();
  }

  private static void testJsCollection() {
    // Treating object arrays as JavaScript native types is only supported in Closure.
    if (!isJavaScript()) {
      return;
    }
    UsesJsCollection o1 = new AutoValue_Main_UsesJsCollection((JsArray) (Object) new Object[] {1});
    UsesJsCollection o2 = new AutoValue_Main_UsesJsCollection((JsArray) (Object) new Object[] {1});
    assertNotEquals(o1, o2);
  }

  @AutoValue
  protected abstract static class Unused {
    public abstract int getIntField();

    public abstract boolean getBooleanField();

    public abstract String getStringField();

    public abstract Double getDoubleField();
  }

  private static void testUnusedType() {
    // Unused code to track/validate code removal with size tracking.
    boolean unusedResult = (new Object()) instanceof AutoValue_Main_Unused;
  }

  @AutoValue
  protected abstract static class UnusedExtending extends BaseClass {
    public int zoo;

    public abstract int foo();
  }

  private static void testUnusedTypeExtending() {
    // Unused code to track/validate code removal with size tracking.
    // In this particular case we make sure ValueType.mixin is utilized.
    boolean unusedResult = (new Object()) instanceof AutoValue_Main_UnusedExtending;
  }

  @AutoValue
  abstract static class AutoValueWithBuilderAndClinit {
    private static int field;

    private int getField() {
      return field;
    }

    static {
      field = 1;
    }

    @AutoValue.Builder
    abstract static class Builder {
      private int anotherField;

      // Declare a non empty constructor to prevent the builder from being optimized.
      Builder() {
        anotherField = 1;
      }

      public abstract AutoValueWithBuilderAndClinit build();

      static AutoValueWithBuilderAndClinit create() {
        return new AutoValue_Main_AutoValueWithBuilderAndClinit.Builder().build();
      }
    }
  }

  /**
   * Tests that clinit is called even if the @AutoValue class is optimized but the Builder is not.
   */
  private static void testClinit() {
    AutoValueWithBuilderAndClinit o = AutoValueWithBuilderAndClinit.Builder.create();
    assertEquals(1, o.getField());
  }

  @AutoValue
  @JsType
  abstract static class AutoValueJsType {
    protected AutoValueJsType() {}

    public abstract int getField();

    @JsMethod(name = "getField2")
    abstract int getWithJsMethod();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface Accessor {
    int getField();

    int getField2();
  }

  @Wasm("nop")
  private static void testJsInterop() {
    if (!isJavaScript()) {
      // Testing JsInterop.
      return;
    }

    Accessor autoValueJsType = (Accessor) (Object) new AutoValue_Main_AutoValueJsType(0, 1);

    assertEquals(0, autoValueJsType.getField());
    assertEquals(1, autoValueJsType.getField2());
  }
}
