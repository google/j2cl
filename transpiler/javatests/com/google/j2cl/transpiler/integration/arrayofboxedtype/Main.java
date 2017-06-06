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

public class Main {
  public void insertByteException(Object[] array) {
    try {
      array[0] = new Byte((byte) 1);
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  public void insertDoubleException(Object[] array) {
    try {
      array[0] = new Double(1.0);
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  public void insertFloatException(Object[] array) {
    try {
      array[0] = new Float(1.0f);
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  public void insertIntegerException(Object[] array) {
    try {
      array[0] = new Integer(1);
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  public void insertLongException(Object[] array) {
    try {
      array[0] = new Long(1L);
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  public void insertShortException(Object[] array) {
    try {
      array[0] = new Short((short) 1);
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  public void insertCharacterException(Object[] array) {
    try {
      array[0] = new Character('a');
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  public void insertBooleanException(Object[] array) {
    try {
      array[0] = new Boolean(true);
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  public void testArrayInsertion() {
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
  public void castToByteException(Object[] array) {
    try {
      Byte[] byteArray = (Byte[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void castToDoubleException(Object[] array) {
    try {
      Double[] doubleArray = (Double[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void castToFloatException(Object[] array) {
    try {
      Float[] floatArray = (Float[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void castToIntegerException(Object[] array) {
    try {
      Integer[] integerArray = (Integer[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void castToLongException(Object[] array) {
    try {
      Long[] longArray = (Long[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void castToShortException(Object[] array) {
    try {
      Short[] shortArray = (Short[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void castToCharacterException(Object[] array) {
    try {
      Character[] characterArray = (Character[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void castToBooleanException(Object[] array) {
    try {
      Boolean[] booleanArray = (Boolean[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void castToNumberException(Object[] array) {
    try {
      Number[] numberArray = (Number[]) array;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  @SuppressWarnings("unused")
  public void testArrayCast() {
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
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
    try {
      Double[] d2 = (Double[]) o2;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }

    // test cast between boolean[] and Boolean[]
    Object o3 = new Boolean[2];
    Object o4 = new boolean[2];
    try {
      boolean[] b3 = (boolean[]) o3;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
    try {
      Boolean[] b4 = (Boolean[]) o4;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }

  public static void main(String[] args) {
    Main m = new Main();
    m.testArrayInsertion();
    m.testArrayCast();
  }
}
