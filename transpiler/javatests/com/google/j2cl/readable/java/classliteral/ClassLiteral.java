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
package classliteral;

public class ClassLiteral {
  @SuppressWarnings("unused")
  public void main() {
    Class<Void> voidClass = void.class;

    Class<Boolean> primitiveBooleanClass = boolean.class;
    Class<boolean[]> primitiveBooleanArrayClass = boolean[].class;
    Class<boolean[][]> primitiveBooleanArrayArrayClass = boolean[][].class;
    Class<Boolean> booleanClass = Boolean.class;
    Class<Boolean[]> booleanArrayClass = Boolean[].class;
    Class<Boolean[][]> booleanArrayArrayClass = Boolean[][].class;

    Class<Character> primitiveCharClass = char.class;
    Class<char[]> primitiveCharArrayClass = char[].class;
    Class<char[][]> primitiveCharArrayArrayClass = char[][].class;
    Class<Character> characterClass = Character.class;
    Class<Character[]> characterArrayClass = Character[].class;
    Class<Character[][]> characterArrayArrayClass = Character[][].class;

    Class<Byte> primitiveByteClass = byte.class;
    Class<byte[]> primitiveByteArrayClass = byte[].class;
    Class<byte[][]> primitiveByteArrayArrayClass = byte[][].class;
    Class<Byte> byteClass = Byte.class;
    Class<Byte[]> byteArrayClass = Byte[].class;
    Class<Byte[][]> byteArrayArrayClass = Byte[][].class;

    Class<Short> primitiveShortClass = short.class;
    Class<short[]> primitiveShortArrayClass = short[].class;
    Class<short[][]> primitiveShortArrayArrayClass = short[][].class;
    Class<Short> shortClass = Short.class;
    Class<Short[]> shortArrayClass = Short[].class;
    Class<Short[][]> shortArrayArrayClass = Short[][].class;

    Class<Integer> primitiveIntClass = int.class;
    Class<int[]> primitiveIntArrayClass = int[].class;
    Class<int[][]> primitiveIntArrayArrayClass = int[][].class;
    Class<Integer> integerClass = Integer.class;
    Class<Integer[]> integerArrayClass = Integer[].class;
    Class<Integer[][]> integerArrayArrayClass = Integer[][].class;

    Class<Long> primitiveLongClass = long.class;
    Class<long[]> primitiveLongArrayClass = long[].class;
    Class<long[][]> primitiveLongArrayArrayClass = long[][].class;
    Class<Long> longClass = Long.class;
    Class<Long[]> longArrayClass = Long[].class;
    Class<Long[][]> longArraArrayClass = Long[][].class;

    Class<Float> primitiveFloatClass = float.class;
    Class<float[]> primitiveFloatArrayClass = float[].class;
    Class<float[][]> primitiveFloatArrayArrayClass = float[][].class;
    Class<Float> floatClass = Float.class;
    Class<Float[]> floatArrayClass = Float[].class;
    Class<Float[][]> floatArrayArrayClass = Float[][].class;

    Class<Double> primitiveDoubleClass = double.class;
    Class<double[]> primitiveDoubleArrayClass = double[].class;
    Class<double[][]> primitiveDoubleArrayArrayClass = double[][].class;
    Class<Double> doubleClass = Double.class;
    Class<Double[]> doubleArrayClass = Double[].class;
    Class<Double[][]> doubleArrayArrayClass = Double[][].class;

    Class<Object> objectClass = Object.class;
    Class<Object[]> objectArrayClass = Object[].class;
    Class<Object[][]> objectArrayArrayClass = Object[][].class;

    Class<ClassLiteral> nonObjectClass = ClassLiteral.class;
    Class<ClassLiteral[]> nonObjectArrayClass = ClassLiteral[].class;
    Class<ClassLiteral[][]> nonObjectArrayArrayClass = ClassLiteral[][].class;
  }
}
