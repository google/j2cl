package com.google.j2cl.transpiler.readable.defaultnonnullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.transpiler.readable.defaultnonnullable.nonnullablesubpackage.NonNullableClass;
import com.google.j2cl.transpiler.readable.defaultnonnullable.nullablesubpackage.NullableClass;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;

import org.checkerframework.checker.nullness.compatqual.NullableType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

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
  // Void is nullable by default.
  private Void f11 = null;
  // Conversion from generic type parameter.
  private List<String> f12 = NullableClass.newArrayList();

  @JsConstructor
  public DefaultNonNullable(String a) {}

  public String m1(String a, List<Double> b, @Nullable String c) {
    return "";
  }

  @Nullable
  public String m2(@Nullable String a, List<@NullableType Double> b) {
    return "";
  }

  @JsMethod
  @Nullable
  public String m3(String a, String... args) {
    return null;
  }

  public void m4(MyFunction f) {
  }

  // Cases in which calling other method and returning doesn't require casting.
  // TODO(simionato): This creates casting even thought it's not needed, add a visitor to do
  // flow analysis to tighten nullability on local variables to fix this.
  public String castingNotNeeded() {
    String a = "";
    List<Double> b = new ArrayList<>();
    String c = null;
    m1(a, b, c);
    this.f1 = a;
    if (true) {
      // Cast not needed since NonNullableClass.getString returns !string.
      return NonNullableClass.getString();
    }
    return a;
  }

  // Cases in which casting is needed, since we can't infer nullability or we are using a library
  // that doesn't have nullability annotations.
  public String castingNeeded(@Nullable String a, Foo<String> foo, String b) {
    this.f4 = Lists.newArrayList();
    this.f4 = new StringList();
    this.f4 = (List) new StringList();
    String x = m3("");
    Preconditions.checkNotNull(x);
    // JS Compiler can't infer that after this point x is not null.
    new DefaultNonNullable(x);
    m3(x, x, x);
    this.f1 = x;
    b = x;
    foo.bar(x);
    b = foo.toString();
    if (true) {
      return x;
    }
    // Cast needed since NullableClass.getString returns ?string.
    return NullableClass.getString();
  }

  public void functions(@Nullable MyFunction function) {
    // Will have to generate a cast here.
    m4(function);
  }

  static class Foo<T> {
    void bar(T t) {}

    @Override
    public String toString() {
      return "Foo";
    }
  }

  @JsFunction
  interface MyFunction {
    String x(String a);
  }

  public static class StringList extends ArrayList<@NullableType String> {}

  // Should implement Comparator<?string>
  public class StringComparator implements Comparator<String> {
    @Override
    public int compare(String a, String b) {
      return 0;
    }
  }

  // Should implement Comparator<string>
  public class NullableStringComparator implements Comparator<@NullableType String> {
    @Override
    public int compare(@Nullable String a, @Nullable String b) {
      return 0;
    }
  }
}
