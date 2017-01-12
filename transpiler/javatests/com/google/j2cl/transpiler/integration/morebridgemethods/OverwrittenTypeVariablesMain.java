package com.google.j2cl.transpiler.integration.morebridgemethods;

enum A {
  FIRST,
  SECOND;
}

interface MyFunction<F, T> {
  T apply(F input);
}

interface HashFunction<T> extends MyFunction<T, String> {}

class HashFunctions {
  public static final <T extends Enum<T>> HashFunction<T> enumHashFunction() {
    return new HashFunction<T>() {
      @Override
      public String apply(T input) {
        return "" + input.ordinal();
      }
    };
  }
}

public class OverwrittenTypeVariablesMain {
  public static void test() {
    // Might crash.
    HashFunctions.enumHashFunction();
  }
}
