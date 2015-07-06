package com.google.j2cl.transpiler.integration.arrayleafinsertion;

public class Main {
  public static void main(String... args) {
    testFullArray();
    testPartialArray();
    testEmptyArray();
  }

  private static void testFullArray() {
    Object[] array = new HasName[2];

    // You can insert a leaf value of a different but conforming type.
    array[0] = new Person();

    // You can NOT insert to an out of bounds index.
    try {
      array[100] = new Person();
      assert false : "An expected failure did not occur.";
    } catch (ArrayIndexOutOfBoundsException e) {
      // expected
    }

    // When inserting a leaf value the type must conform.
    try {
      array[0] = new Object();
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void testPartialArray() {
    // You can create a partially initialized array.
    Object[][] partialArray = new Object[1][];

    // When trying to insert into the uninitialized section you'll get an NPE.
    try {
      partialArray[0][0] = new Person();
      assert false : "An expected failure did not occur.";
    } catch (NullPointerException e) {
      // expected
    }

    // You can replace it with a fully initialized array.
    partialArray = new Object[1][1];

    // And then insert a leaf value that conforms to the strictest leaf type
    partialArray[0][0] = new Person();
  }

  private static void testEmptyArray() {
    // You can create a zero length array.
    Object[] emptyArray = new Object[0];

    // But any insertion attempt will be out of bounds.
    try {
      emptyArray[0] = new Person();
      assert false : "An expected failure did not occur.";
    } catch (ArrayIndexOutOfBoundsException e) {
      // expected
    }
  }
}
