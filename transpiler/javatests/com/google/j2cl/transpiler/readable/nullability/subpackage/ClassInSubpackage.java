package com.google.j2cl.transpiler.readable.nullability.subpackage;

import javax.annotation.Nonnull;

public class ClassInSubpackage {
  public static String getString() {
    return null;
  }

  @Nonnull
  public static String getNonNullString() {
    return "Hello";
  }
}
