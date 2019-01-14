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
package com.google.j2cl.transpiler.integration.arrayofboxedtype;

import static com.google.j2cl.transpiler.utils.Asserts.fail;

public class Main {
  public static void main(String[] args) {
    testArrayInsertion();
    testArrayCast();
  }

  private static void insertByteException(Object[] array) {
    try {
      array[0] = new Byte((byte) 1);
      fail("An expected failure did not occur.");
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void insertDoubleException(Object[] array) {
    try {
      array[0] = new Double(1.0);
      fail("An expected failure did not occur.");
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void insertFloatException(Object[] array) {
    try {
      array[0] = new Float(1.0f);
      fail("An expected failure did not occur.");
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void insertIntegerException(Object[] array) {
    try {
      array[0] = new Integer(1);
      fail("An expected failure did not occur.");
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void insertLongException(Object[] array) {
    try {
      array[0] = new Long(1L);
      fail("An expected failure did not occur.");
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void insertShortException(Object[] array) {
    try {
      array[0] = new Short((short) 1);
      fail("An expected failure did not occur.");
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void insertCharacterException(Object[] array) {
    try {
      array[0] = new Character('a');
      fail("An expected failure did not occur.");
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void insertBooleanException(Object[] array) {
    try {
      array[0] = new Boolean(true);
      fail("An expected failure did not occur.");
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void testArrayInsertion() {
    Object[] byteArray = new Byte[2];
    byteArray[0] = new Byte((byte) 1);
    insertBooleanException(byteArray);
    insertCharacterException(byteArray);
    insertDoubleException(byteArray);
    insertFloatException(byteArray);
    insertIntegerException(byteArray);
    insertLongException(byteArray);
    insertShortException(byteArray);

    Object[] doubleArray = new Double[2];
    doubleArray[0] = new Double(1.0);
    insertBooleanException(doubleArray);
    insertCharacterException(doubleArray);
    insertByteException(doubleArray);
    insertFloatException(doubleArray);
    insertIntegerException(doubleArray);
    insertLongException(doubleArray);
    insertShortException(doubleArray);

    Object[] floatArray = new Float[2];
    floatArray[0] = new Float(1.0f);
    insertBooleanException(floatArray);
    insertCharacterException(floatArray);
    insertByteException(floatArray);
    insertDoubleException(floatArray);
    insertIntegerException(floatArray);
    insertLongException(floatArray);
    insertShortException(floatArray);

    Object[] integerArray = new Integer[2];
    integerArray[0] = new Integer(1);
    insertBooleanException(integerArray);
    insertCharacterException(integerArray);
    insertByteException(integerArray);
    insertDoubleException(integerArray);
    insertFloatException(integerArray);
    insertLongException(integerArray);
    insertShortException(integerArray);

    Object[] longArray = new Long[2];
    longArray[0] = new Long(1L);
    insertBooleanException(longArray);
    insertCharacterException(longArray);
    insertByteException(longArray);
    insertDoubleException(longArray);
    insertFloatException(longArray);
    insertIntegerException(longArray);
    insertShortException(longArray);

    Object[] shortArray = new Short[2];
    shortArray[0] = new Short((short) 1);
    insertBooleanException(shortArray);
    insertCharacterException(shortArray);
    insertByteException(shortArray);
    insertDoubleException(shortArray);
    insertFloatException(shortArray);
    insertIntegerException(shortArray);
    insertLongException(shortArray);

    Object[] characterArray = new Character[2];
    characterArray[0] = new Character('a');
    insertBooleanException(characterArray);
    insertByteException(characterArray);
    insertDoubleException(characterArray);
    insertFloatException(characterArray);
    insertIntegerException(characterArray);
    insertLongException(characterArray);
    insertShortException(characterArray);

    Object[] booleanArray = new Boolean[2];
    booleanArray[0] = new Boolean(true);
    insertCharacterException(booleanArray);
    insertByteException(booleanArray);
    insertDoubleException(booleanArray);
    insertFloatException(booleanArray);
    insertIntegerException(booleanArray);
    insertLongException(booleanArray);
    insertShortException(booleanArray);

    Object[] numberArray = new Number[2];
    numberArray[0] = new Byte((byte) 1);
    numberArray[0] = new Double(1.0);
    numberArray[0] = new Float(1.0f);
    numberArray[0] = new Integer(1);
    numberArray[0] = new Long(1L);
    numberArray[0] = new Short((short) 1);
    numberArray[0] = new SubNumber();
    insertBooleanException(numberArray);
    insertCharacterException(numberArray);

    Object[] subnumberArray = new SubNumber[2];
    subnumberArray[0] = new SubNumber();
    insertBooleanException(subnumberArray);
    insertCharacterException(subnumberArray);
    insertByteException(subnumberArray);
    insertDoubleException(subnumberArray);
    insertFloatException(subnumberArray);
    insertIntegerException(subnumberArray);
    insertLongException(subnumberArray);
    insertShortException(subnumberArray);
  }

  @SuppressWarnings("unused")
  private static void castToByteException(Object[] array) {
    try {
      Byte[] byteArray = (Byte[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void castToDoubleException(Object[] array) {
    try {
      Double[] doubleArray = (Double[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void castToFloatException(Object[] array) {
    try {
      Float[] floatArray = (Float[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void castToIntegerException(Object[] array) {
    try {
      Integer[] integerArray = (Integer[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void castToLongException(Object[] array) {
    try {
      Long[] longArray = (Long[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void castToShortException(Object[] array) {
    try {
      Short[] shortArray = (Short[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void castToCharacterException(Object[] array) {
    try {
      Character[] characterArray = (Character[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void castToBooleanException(Object[] array) {
    try {
      Boolean[] booleanArray = (Boolean[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void castToNumberException(Object[] array) {
    try {
      Number[] numberArray = (Number[]) array;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  private static void testArrayCast() {
    Object[] byteArray = new Byte[2];
    Byte[] bArray = (Byte[]) byteArray;
    Number[] nArray = (Number[]) byteArray;
    castToDoubleException(byteArray);
    castToFloatException(byteArray);
    castToIntegerException(byteArray);
    castToLongException(byteArray);
    castToShortException(byteArray);
    castToCharacterException(byteArray);
    castToBooleanException(byteArray);

    Object[] doubleArray = new Double[2];
    Double[] dArray = (Double[]) doubleArray;
    nArray = (Number[]) doubleArray;
    castToByteException(doubleArray);
    castToFloatException(doubleArray);
    castToIntegerException(doubleArray);
    castToLongException(doubleArray);
    castToShortException(doubleArray);
    castToCharacterException(doubleArray);
    castToBooleanException(doubleArray);

    Object[] floatArray = new Float[2];
    Float[] fArray = (Float[]) floatArray;
    nArray = (Number[]) floatArray;
    castToByteException(floatArray);
    castToDoubleException(floatArray);
    castToIntegerException(floatArray);
    castToLongException(floatArray);
    castToShortException(floatArray);
    castToCharacterException(floatArray);
    castToBooleanException(floatArray);

    Object[] integerArray = new Integer[2];
    Integer[] iArray = (Integer[]) integerArray;
    nArray = (Number[]) integerArray;
    castToByteException(integerArray);
    castToDoubleException(integerArray);
    castToFloatException(integerArray);
    castToLongException(integerArray);
    castToShortException(integerArray);
    castToCharacterException(integerArray);
    castToBooleanException(integerArray);

    Object[] longArray = new Long[2];
    Long[] lArray = (Long[]) longArray;
    nArray = (Number[]) longArray;
    castToByteException(longArray);
    castToDoubleException(longArray);
    castToFloatException(longArray);
    castToIntegerException(longArray);
    castToShortException(longArray);
    castToCharacterException(longArray);
    castToBooleanException(longArray);

    Object[] shortArray = new Short[2];
    Short[] sArray = (Short[]) shortArray;
    nArray = (Number[]) shortArray;
    castToByteException(shortArray);
    castToDoubleException(shortArray);
    castToFloatException(shortArray);
    castToIntegerException(shortArray);
    castToLongException(shortArray);
    castToCharacterException(shortArray);
    castToBooleanException(shortArray);

    Object[] characterArray = new Character[2];
    Character[] cArray = (Character[]) characterArray;
    castToByteException(characterArray);
    castToDoubleException(characterArray);
    castToFloatException(characterArray);
    castToIntegerException(characterArray);
    castToLongException(characterArray);
    castToShortException(characterArray);
    castToBooleanException(characterArray);
    castToNumberException(characterArray);

    Object[] booleanArray = new Boolean[2];
    Boolean[] boolArray = (Boolean[]) booleanArray;
    castToByteException(booleanArray);
    castToDoubleException(booleanArray);
    castToFloatException(booleanArray);
    castToIntegerException(booleanArray);
    castToLongException(booleanArray);
    castToShortException(booleanArray);
    castToCharacterException(booleanArray);
    castToNumberException(booleanArray);

    Object[] numberArray = new Number[2];
    nArray = (Number[]) numberArray;
    castToByteException(numberArray);
    castToDoubleException(numberArray);
    castToFloatException(numberArray);
    castToIntegerException(numberArray);
    castToLongException(numberArray);
    castToShortException(numberArray);
    castToCharacterException(numberArray);
    castToBooleanException(numberArray);

    // test cast from SubNumber[] to Number[].
    Object[] subnumberArray = new SubNumber[2];
    nArray = (Number[]) subnumberArray;
    SubNumber[] sbArray = (SubNumber[]) subnumberArray;

    // test cast between double[] and Double[]
    Object o1 = new Double[2];
    Object o2 = new double[2];
    try {
      double[] d1 = (double[]) o1;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
    try {
      Double[] d2 = (Double[]) o2;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }

    // test cast between boolean[] and Boolean[]
    Object o3 = new Boolean[2];
    Object o4 = new boolean[2];
    try {
      boolean[] b3 = (boolean[]) o3;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
    try {
      Boolean[] b4 = (Boolean[]) o4;
      fail("An expected failure did not occur.");
    } catch (ClassCastException e) {
      // expected
    }
  }
}
