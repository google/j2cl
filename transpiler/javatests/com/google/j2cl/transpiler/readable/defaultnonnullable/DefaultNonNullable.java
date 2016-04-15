package com.google.j2cl.transpiler.readable.defaultnonnullable;

import jsinterop.annotations.JsMethod;

import org.checkerframework.checker.nullness.compatqual.NullableType;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

// TODO(simionato): Update this example after there is support for @NotNull.
public class DefaultNonNullable {
  private String f1 = "Hello";
  @Nullable private String f2 = null;
  @org.checkerframework.checker.nullness.qual.Nullable private String f3 = null;
  private List<String> f4 = new ArrayList<>();
  @Nullable private List<String> f5 = new ArrayList<>();
  private List<@NullableType String> f6 = new ArrayList<>();
  @Nullable private List<@NullableType String> f7 = null;
  private String[] f8 = {};
  // Nonnullable array of nullable strings.
  @org.checkerframework.checker.nullness.qual.Nullable private String[] f9 = {};
  // Nullable array of non-nullable strings.
  private String @NullableType [] f10 = {};
  private Void f11 = null;

  public String m1(String a, List<Double> b) {
    return "";
  }

  @Nullable
  public String m2(@Nullable String a, List<@NullableType Double> b) {
    return null;
  }

  @JsMethod
  public void m3(String... args) {
  }
}
