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
package arrayofboxedtype;

import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;

import com.google.j2cl.integration.testing.Asserts;

public class Main {
  public static void main(String[] args) {
    testArrayInsertion();
    testArrayCast();
  }

  private static void assertThrowsArrayStoreException(Object[] array, Object o) {
    Asserts.assertThrowsArrayStoreException(
        () -> {
          array[0] = o;
        });
  }

  private static void testArrayInsertion() {
    Boolean booleanValue = new Boolean(true);
    Character characterValue = new Character('a');
    Byte byteValue = new Byte((byte) 1);
    Short shortValue = new Short((short) 1);
    Integer integerValue = new Integer(1);
    Long longValue = new Long(1L);
    Double doubleValue = new Double(1.0);
    Float floatValue = new Float(1.0f);
    Object[] byteArray = new Byte[2];

    byteArray[0] = byteValue;
    assertThrowsArrayStoreException(byteArray, booleanValue);
    assertThrowsArrayStoreException(byteArray, characterValue);
    assertThrowsArrayStoreException(byteArray, doubleValue);
    assertThrowsArrayStoreException(byteArray, floatValue);
    assertThrowsArrayStoreException(byteArray, integerValue);
    assertThrowsArrayStoreException(byteArray, longValue);
    assertThrowsArrayStoreException(byteArray, shortValue);

    Object[] doubleArray = new Double[2];
    doubleArray[0] = doubleValue;
    assertThrowsArrayStoreException(doubleArray, booleanValue);
    assertThrowsArrayStoreException(doubleArray, characterValue);
    assertThrowsArrayStoreException(doubleArray, byteValue);
    assertThrowsArrayStoreException(doubleArray, floatValue);
    assertThrowsArrayStoreException(doubleArray, integerValue);
    assertThrowsArrayStoreException(doubleArray, longValue);
    assertThrowsArrayStoreException(doubleArray, shortValue);

    Object[] floatArray = new Float[2];
    floatArray[0] = floatValue;
    assertThrowsArrayStoreException(floatArray, booleanValue);
    assertThrowsArrayStoreException(floatArray, characterValue);
    assertThrowsArrayStoreException(floatArray, byteValue);
    assertThrowsArrayStoreException(floatArray, doubleValue);
    assertThrowsArrayStoreException(floatArray, integerValue);
    assertThrowsArrayStoreException(floatArray, longValue);
    assertThrowsArrayStoreException(floatArray, shortValue);

    Object[] integerArray = new Integer[2];
    integerArray[0] = integerValue;
    assertThrowsArrayStoreException(integerArray, booleanValue);
    assertThrowsArrayStoreException(integerArray, characterValue);
    assertThrowsArrayStoreException(integerArray, byteValue);
    assertThrowsArrayStoreException(integerArray, doubleValue);
    assertThrowsArrayStoreException(integerArray, floatValue);
    assertThrowsArrayStoreException(integerArray, longValue);
    assertThrowsArrayStoreException(integerArray, shortValue);

    Object[] longArray = new Long[2];
    longArray[0] = longValue;
    assertThrowsArrayStoreException(longArray, booleanValue);
    assertThrowsArrayStoreException(longArray, characterValue);
    assertThrowsArrayStoreException(longArray, byteValue);
    assertThrowsArrayStoreException(longArray, doubleValue);
    assertThrowsArrayStoreException(longArray, floatValue);
    assertThrowsArrayStoreException(longArray, integerValue);
    assertThrowsArrayStoreException(longArray, shortValue);

    Object[] shortArray = new Short[2];
    shortArray[0] = shortValue;
    assertThrowsArrayStoreException(shortArray, booleanValue);
    assertThrowsArrayStoreException(shortArray, characterValue);
    assertThrowsArrayStoreException(shortArray, byteValue);
    assertThrowsArrayStoreException(shortArray, doubleValue);
    assertThrowsArrayStoreException(shortArray, floatValue);
    assertThrowsArrayStoreException(shortArray, integerValue);
    assertThrowsArrayStoreException(shortArray, longValue);

    Object[] characterArray = new Character[2];
    characterArray[0] = characterValue;
    assertThrowsArrayStoreException(characterArray, booleanValue);
    assertThrowsArrayStoreException(characterArray, byteValue);
    assertThrowsArrayStoreException(characterArray, doubleValue);
    assertThrowsArrayStoreException(characterArray, floatValue);
    assertThrowsArrayStoreException(characterArray, integerValue);
    assertThrowsArrayStoreException(characterArray, longValue);
    assertThrowsArrayStoreException(characterArray, shortValue);

    Object[] booleanArray = new Boolean[2];
    booleanArray[0] = booleanValue;
    assertThrowsArrayStoreException(booleanArray, characterValue);
    assertThrowsArrayStoreException(booleanArray, byteValue);
    assertThrowsArrayStoreException(booleanArray, doubleValue);
    assertThrowsArrayStoreException(booleanArray, floatValue);
    assertThrowsArrayStoreException(booleanArray, integerValue);
    assertThrowsArrayStoreException(booleanArray, longValue);
    assertThrowsArrayStoreException(booleanArray, shortValue);

    Object[] numberArray = new Number[2];
    numberArray[0] = byteValue;
    numberArray[0] = doubleValue;
    numberArray[0] = floatValue;
    numberArray[0] = integerValue;
    numberArray[0] = longValue;
    numberArray[0] = shortValue;
    numberArray[0] = new SubNumber();
    assertThrowsArrayStoreException(numberArray, booleanValue);
    assertThrowsArrayStoreException(numberArray, characterValue);

    Object[] subnumberArray = new SubNumber[2];
    subnumberArray[0] = new SubNumber();
    assertThrowsArrayStoreException(subnumberArray, booleanValue);
    assertThrowsArrayStoreException(subnumberArray, characterValue);
    assertThrowsArrayStoreException(subnumberArray, byteValue);
    assertThrowsArrayStoreException(subnumberArray, doubleValue);
    assertThrowsArrayStoreException(subnumberArray, floatValue);
    assertThrowsArrayStoreException(subnumberArray, integerValue);
    assertThrowsArrayStoreException(subnumberArray, longValue);
    assertThrowsArrayStoreException(subnumberArray, shortValue);
  }

  private static void castToByteException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Byte[] unused = (Byte[]) array;
        });
  }

  private static void castToDoubleException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Double[] unused = (Double[]) array;
        });
  }

  private static void castToFloatException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Float[] unused = (Float[]) array;
        });
  }

  private static void castToIntegerException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Integer[] unused = (Integer[]) array;
        });
  }

  private static void castToLongException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Long[] unused = (Long[]) array;
        });
  }

  private static void castToShortException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Short[] unused = (Short[]) array;
        });
  }

  private static void castToCharacterException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Character[] unused = (Character[]) array;
        });
  }

  private static void castToBooleanException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Boolean[] unused = (Boolean[]) array;
        });
  }

  private static void castToNumberException(Object[] array) {
    assertThrowsClassCastException(
        () -> {
          Number[] unused = (Number[]) array;
        });
  }

  private static void testArrayCast() {
    Object[] byteArray = new Byte[2];
    Byte[] unusedByteArray = (Byte[]) byteArray;
    Number[] unusedNumberArray = (Number[]) byteArray;
    castToDoubleException(byteArray);
    castToFloatException(byteArray);
    castToIntegerException(byteArray);
    castToLongException(byteArray);
    castToShortException(byteArray);
    castToCharacterException(byteArray);
    castToBooleanException(byteArray);

    Object[] doubleArray = new Double[2];
    Double[] unusedDoubleArray = (Double[]) doubleArray;
    unusedNumberArray = (Number[]) doubleArray;
    castToByteException(doubleArray);
    castToFloatException(doubleArray);
    castToIntegerException(doubleArray);
    castToLongException(doubleArray);
    castToShortException(doubleArray);
    castToCharacterException(doubleArray);
    castToBooleanException(doubleArray);

    Object[] floatArray = new Float[2];
    Float[] unusedFloatArray = (Float[]) floatArray;
    unusedNumberArray = (Number[]) floatArray;
    castToByteException(floatArray);
    castToDoubleException(floatArray);
    castToIntegerException(floatArray);
    castToLongException(floatArray);
    castToShortException(floatArray);
    castToCharacterException(floatArray);
    castToBooleanException(floatArray);

    Object[] integerArray = new Integer[2];
    Integer[] unusedIntegerArray = (Integer[]) integerArray;
    unusedNumberArray = (Number[]) integerArray;
    castToByteException(integerArray);
    castToDoubleException(integerArray);
    castToFloatException(integerArray);
    castToLongException(integerArray);
    castToShortException(integerArray);
    castToCharacterException(integerArray);
    castToBooleanException(integerArray);

    Object[] longArray = new Long[2];
    Long[] unusedLongArray = (Long[]) longArray;
    unusedNumberArray = (Number[]) longArray;
    castToByteException(longArray);
    castToDoubleException(longArray);
    castToFloatException(longArray);
    castToIntegerException(longArray);
    castToShortException(longArray);
    castToCharacterException(longArray);
    castToBooleanException(longArray);

    Object[] shortArray = new Short[2];
    Short[] unusedShortArray = (Short[]) shortArray;
    unusedNumberArray = (Number[]) shortArray;
    castToByteException(shortArray);
    castToDoubleException(shortArray);
    castToFloatException(shortArray);
    castToIntegerException(shortArray);
    castToLongException(shortArray);
    castToCharacterException(shortArray);
    castToBooleanException(shortArray);

    Object[] characterArray = new Character[2];
    Character[] unusedCharacterArray = (Character[]) characterArray;
    castToByteException(characterArray);
    castToDoubleException(characterArray);
    castToFloatException(characterArray);
    castToIntegerException(characterArray);
    castToLongException(characterArray);
    castToShortException(characterArray);
    castToBooleanException(characterArray);
    castToNumberException(characterArray);

    Object[] booleanArray = new Boolean[2];
    Boolean[] unusedBooleanArray = (Boolean[]) booleanArray;
    castToByteException(booleanArray);
    castToDoubleException(booleanArray);
    castToFloatException(booleanArray);
    castToIntegerException(booleanArray);
    castToLongException(booleanArray);
    castToShortException(booleanArray);
    castToCharacterException(booleanArray);
    castToNumberException(booleanArray);

    Object[] numberArray = new Number[2];
    unusedNumberArray = (Number[]) numberArray;
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
    unusedNumberArray = (Number[]) subnumberArray;
    SubNumber[] unusedSbArray = (SubNumber[]) subnumberArray;

    // test cast between double[] and Double[]
    Object o1 = new Double[2];
    Object o2 = new double[2];
    assertThrowsClassCastException(
        () -> {
          double[] unused = (double[]) o1;
        });
    assertThrowsClassCastException(
        () -> {
          Double[] unused = (Double[]) o2;
        });

    // test cast between boolean[] and Boolean[]
    Object o3 = new Boolean[2];
    Object o4 = new boolean[2];
    assertThrowsClassCastException(
        () -> {
          boolean[] unused = (boolean[]) o3;
        });
    assertThrowsClassCastException(
        () -> {
          Boolean[] unused = (Boolean[]) o4;
        });
  }
}
