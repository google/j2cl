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
package com.google.j2cl.transpiler.integration.castdevirtualizedtypes;

@SuppressWarnings("BoxedPrimitiveConstructor")
public class Main {
  public static void main(String[] args) {
    testObject();
    testNumber();
    testComparable();
    testCharSequence();
    testVoid();
  }

  private static void testObject() {
    Object unusedObject = null;

    // All these casts should succeed.
    unusedObject = (Object) "";
    unusedObject = (Object) new Double(0);
    unusedObject = (Object) new Boolean(false);
    unusedObject = (Object) new Object[] {};
  }

  private static void testNumber() {
    Number unusedNumber = null;

    // This casts should succeed.
    unusedNumber = (Number) new Double(0);
  }

  private static void testComparable() {
    Comparable<?> unusedComparable = null;

    // All these casts should succeed.
    unusedComparable = (Comparable) "";
    unusedComparable = (Comparable) new Double(0);
    unusedComparable = (Comparable) new Boolean(false);
  }

  private static void testCharSequence() {
    CharSequence unusedCharSequence = null;

    // This casts should succeed.
    unusedCharSequence = (CharSequence) "";
  }

  private static void testVoid() {
    Void unusedVoid = null;

    // This casts should succeed.
    unusedVoid = (Void) null;
  }
}
