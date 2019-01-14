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
package com.google.j2cl.transpiler.integration.casttoboxedtype;

import static com.google.j2cl.transpiler.utils.Asserts.assertThrowsClassCastException;

public class Main {
  public static void main(String[] args) {
    Object b = new Byte((byte) 1);
    Byte unusedB = (Byte) b;
    Number unusedN = (Number) b;
    castToDoubleException(b);
    castToFloatException(b);
    castToIntegerException(b);
    castToLongException(b);
    castToShortException(b);
    castToCharacterException(b);
    castToBooleanException(b);

    Object d = new Double(1.0);
    Double unusedD = (Double) d;
    unusedN = (Number) d;
    castToByteException(d);
    castToFloatException(d);
    castToIntegerException(d);
    castToLongException(d);
    castToShortException(d);
    castToCharacterException(d);
    castToBooleanException(d);

    Object f = new Float(1.0f);
    Float unusedF = (Float) f;
    unusedN = (Number) f;
    castToByteException(f);
    castToDoubleException(f);
    castToIntegerException(f);
    castToLongException(f);
    castToShortException(f);
    castToCharacterException(f);
    castToBooleanException(f);

    Object i = new Integer(1);
    Integer unusedI = (Integer) i;
    unusedN = (Number) i;
    castToByteException(i);
    castToDoubleException(i);
    castToFloatException(i);
    castToLongException(i);
    castToShortException(i);
    castToCharacterException(i);
    castToBooleanException(i);

    Object l = new Long(1L);
    Long unusedL = (Long) l;
    unusedN = (Number) l;
    castToByteException(l);
    castToDoubleException(l);
    castToFloatException(l);
    castToIntegerException(l);
    castToShortException(l);
    castToCharacterException(l);
    castToBooleanException(l);

    Object s = new Short((short) 1);
    Short unusedS = (Short) s;
    unusedN = (Number) s;
    castToByteException(s);
    castToDoubleException(s);
    castToFloatException(s);
    castToIntegerException(s);
    castToLongException(s);
    castToCharacterException(s);
    castToBooleanException(s);

    Object c = new Character('a');
    Character unusedC = (Character) c;
    castToByteException(c);
    castToDoubleException(c);
    castToFloatException(c);
    castToIntegerException(c);
    castToLongException(c);
    castToShortException(c);
    castToNumberException(c);
    castToBooleanException(c);

    Object bool = new Boolean(true);
    Boolean unusedBool = (Boolean) bool;
    castToByteException(bool);
    castToDoubleException(bool);
    castToFloatException(bool);
    castToIntegerException(bool);
    castToLongException(bool);
    castToShortException(bool);
    castToNumberException(bool);
    castToCharacterException(bool);

    Object sn = new SubNumber();
    unusedN = (Number) sn;
  }

  private static void castToByteException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Byte b = (Byte) o;
        });
  }

  private static void castToDoubleException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Double d = (Double) o;
        });
  }

  private static void castToFloatException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Float f = (Float) o;
        });
  }

  private static void castToIntegerException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Integer i = (Integer) o;
        });
  }

  private static void castToLongException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Long l = (Long) o;
        });
  }

  private static void castToShortException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Short s = (Short) o;
        });
  }

  private static void castToCharacterException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Character c = (Character) o;
        });
  }

  private static void castToBooleanException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Boolean b = (Boolean) o;
        });
  }

  private static void castToNumberException(Object o) {
    assertThrowsClassCastException(
        () -> {
          Number n = (Number) o;
        });
  }
}
