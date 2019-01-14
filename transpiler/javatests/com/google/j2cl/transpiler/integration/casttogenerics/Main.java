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
package com.google.j2cl.transpiler.integration.casttogenerics;

import static com.google.j2cl.transpiler.utils.Asserts.fail;

public class Main<T, E extends Number> {
  @SuppressWarnings({"unused", "unchecked"})
  public void test() {
    Object o = new Integer(1);
    E e = (E) o; // cast to type varaible with bound, casting Integer instance to Number
    T t = (T) o; // cast to type varaible without bound, casting Integer instance to Object

    Object oo = new Error();
    try {
      E ee = (E) oo; // casting Error instance to Number, exception.
      fail("An expected failure did not occur.");
    } catch (ClassCastException exe) {
      // expected.
    }

    Object c = new Main<Number, Number>();
    Main<Error, Number> cc = (Main<Error, Number>) c; // cast to parameterized type.

    Object[] is = new Integer[1];
    Object[] os = new Object[1];
    E[] es = (E[]) is;
    T[] ts = (T[]) is;
    try {
      E[] ees = (E[]) os;
      fail("An expected failure did not occur.");
    } catch (ClassCastException exe) {
      // expected.
    }
    T[] tts = (T[]) os;
  }

  public static void main(String... args) {
    Main<Integer, Integer> m = new Main<Integer, Integer>();
    m.test();
  }
}
