package com.google.j2cl.transpiler.readable.defaultnonnullable.nullablesubpackage;

import java.util.ArrayList;
import java.util.List;

// This class defaults to nullable.
public class NullableClass {
  public static String getString() {
    return null;
  }

  public static <T> List<T> newArrayList() {
    return new ArrayList<>();
  }
}
