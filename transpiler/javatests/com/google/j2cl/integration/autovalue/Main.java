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
package com.google.j2cl.integration.autovalue;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

public class Main {

  public static void main(String... args) {
    testComposite();
    testUnreadValue();
    testExtending();
    testMemoized();
    testAbstractEquals();
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
    CompositeB parent = new AutoValue_CompositeB(11, true, "world", 101d, componentB);
    assertEquals(11, parent.getIntField());
    assertEquals(true, parent.getBooleanField());
    assertEquals("world", parent.getStringField());
    assertEquals(101d, parent.getDoubleField());
    assertEquals(componentB, parent.getComponentField());
    assertEquals(componentB.hashCode(), parent.getComponentField().hashCode());
    assertNotNull(parent.toString());
  }

  @AutoValue
  protected abstract static class TypeWithUnreadField {
    public abstract int getUnreadField();
  }

  private static void testUnreadValue() {
    // TODO(b/155944756): Fix the equals behavior for unread values.
    // assertNotEquals(
    //     new AutoValue_Main_TypeWithUnreadField(5), new AutoValue_Main_TypeWithUnreadField(6));
  }

  private static class BaseClass {
    int bar;
  }

  @AutoValue
  protected abstract static class ExtendingAutoValue extends BaseClass {
    public abstract int foo();
  }

  private static void testExtending() {
    ExtendingAutoValue o1 = new AutoValue_Main_ExtendingAutoValue(12);
    ExtendingAutoValue o2 = new AutoValue_Main_ExtendingAutoValue(12);
    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());

    // TODO(b/160369891): Fix the equals behavior with extra fields.
    // o1.bar = 2;
    // assertEquals(o1, o2);
    // assertEquals(o1.hashCode(), o2.hashCode());
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

    // TODO(b/160369891): Fix the equals behavior for extension values.
    // assertEquals(22, o2.memoized());
    // assertEquals(o1, o2);
    // assertEquals(o1.hashCode(), o2.hashCode());
  }

  @AutoValue
  abstract static class AbstractEquals {
    abstract String string();

    // TODO(b/188737593): Fix overriden abstract method handling.
    // @Override
    // public abstract boolean equals(Object o);
  }

  private static void testAbstractEquals() {
    AbstractEquals o1 = new AutoValue_Main_AbstractEquals("a");
    AbstractEquals o2 = new AutoValue_Main_AbstractEquals("a");
    assertEquals(o1, o2);
  }
}
