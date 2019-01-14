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
package com.google.j2cl.transpiler.integration.supermethodcall;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/**
 * Test non-constructor super method calls.
 */
public class Main {
  public static void main(String[] args) {
    Child child = new Child();
    Parent parent = new Parent();
    GrandParent grandParent = new GrandParent();

    // Given the same inputs the functions return the same values, because the child calls are just
    // delegating to the super calls.
    assertTrue(child.getNameTwo() == parent.getNameTwo());
    assertTrue(child.getParentPassedValue(15) == parent.getParentPassedValue(15));
    assertTrue(
        child.getParentPassedValueWithChangingReturn(parent)
            == parent.getParentPassedValueWithChangingReturn(parent));
    assertTrue(child.getNameOne() == parent.getNameOne());
    assertTrue(
        child.getGrandParentPassedValueTimesTwo(15)
            == parent.getGrandParentPassedValueTimesTwo(15));
    assertTrue(
        child.getGrandParentPassedValueWithChangingReturn(parent)
            == parent.getGrandParentPassedValueWithChangingReturn(parent));
    assertTrue(child.getNameOne() == grandParent.getNameOne());
    assertTrue(
        child.getGrandParentPassedValueTimesTwo(15)
            == grandParent.getGrandParentPassedValueTimesTwo(15));
    assertTrue(
        child.getGrandParentPassedValueWithChangingReturn(grandParent)
            == grandParent.getGrandParentPassedValueWithChangingReturn(grandParent));

    // But with different inputs of course the results also differ.
    Parent unexpectedParentInstance = new Parent();
    assertTrue(child.getParentPassedValue(99999) != parent.getParentPassedValue(1));
    assertTrue(
        child.getParentPassedValueWithChangingReturn(unexpectedParentInstance)
            != parent.getParentPassedValueWithChangingReturn(parent));
    assertTrue(
        child.getGrandParentPassedValueTimesTwo(99999)
            != parent.getGrandParentPassedValueTimesTwo(1));
    assertTrue(
        child.getGrandParentPassedValueWithChangingReturn(unexpectedParentInstance)
            != parent.getGrandParentPassedValueWithChangingReturn(parent));
    assertTrue(
        child.getGrandParentPassedValueTimesTwo(99999)
            != grandParent.getGrandParentPassedValueTimesTwo(1));
    assertTrue(
        child.getGrandParentPassedValueWithChangingReturn(unexpectedParentInstance)
            != grandParent.getGrandParentPassedValueWithChangingReturn(grandParent));
  }
}
