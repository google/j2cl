package com.google.j2cl.transpiler.readable.arrayofboxedtype;

public class ArrayOfBoxedType {
  @SuppressWarnings("unused")
  public void test() {
    // Boxed-type array creation.
    Object[] numberArray = new Number[2];
    Object[] byteArray = new Byte[2];
    Object[] doublerArray = new Double[2];
    Object[] floatArray = new Float[2];
    Object[] integerArray = new Integer[2];
    Object[] longArray = new Long[2];
    Object[] shortArray = new Short[2];
    Object[] characterArray = new Character[2];
    Object[] booleanArray = new Boolean[2];

    // cast to boxed-type array
    Number[] nArray = (Number[]) byteArray;
    Byte[] bArray = (Byte[]) byteArray;
    Double[] dArray = (Double[]) byteArray;
    Float[] fArray = (Float[]) byteArray;
    Integer[] iArray = (Integer[]) byteArray;
    Long[] lArray = (Long[]) byteArray;
    Short[] sArray = (Short[]) byteArray;
    Character[] cArray = (Character[]) byteArray;
    Boolean[] boolArray = (Boolean[]) byteArray;

    // array insertion.
    numberArray[1] = new Integer(1);
  }
}
