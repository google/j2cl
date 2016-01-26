package com.google.j2cl.transpiler.readable.overwrittentypevariables;

public class HashFunctions {
  public static final <T> HashFunction<T> hashFunction() {
    return new HashFunction<T>() {
      @Override
      public String apply(T input) {
        return "";
      }
    };
  }

  public static final <T extends Enum<T>> HashFunction<T> enumHashFunction() {
    return new HashFunction<T>() {
      @Override
      public String apply(T input) {
        return "" + input.ordinal();
      }
    };
  }
}
