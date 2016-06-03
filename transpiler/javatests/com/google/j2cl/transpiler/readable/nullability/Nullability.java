package com.google.j2cl.transpiler.readable.nullability;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.transpiler.readable.nullability.subpackage.ClassInSubpackage;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;

import org.checkerframework.checker.nullness.compatqual.NullableType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Nullability {
  @Nonnull private String f1 = "Hello";
  private String f2 = null;
  @Nullable private String f3 = null;
  private @Nonnull List<@NonNull String> f4 = new ArrayList<>();
  private List<@NonNull String> f5 = new ArrayList<>();
  private @Nonnull List<String> f6 = new ArrayList<>();
  private List<String> f7 = null;
  private @NonNull String @NonNull [] f8 = {};
  // Nonnullable array of nullable strings.
  private String @NonNull [] f9 = {};
  // Nullable array of non-nullable strings.
  private @NonNull String[] f10 = {};
  // Conversion from generic type parameter.
  private List<String> f12 = Lists.newArrayList();

  @JsConstructor
  public Nullability(@Nonnull String a) {}

  @Nonnull
  public String m1(@Nonnull String a, @Nonnull List<@NonNull Double> b, String c) {
    return "";
  }

  public String m2(String a, @Nonnull List<@NullableType Double> b) {
    return "";
  }

  @JsMethod
  public String m3(@Nonnull String a, @NonNull String... args) {
    return null;
  }

  public void m4(@Nonnull MyFunction f) {}

  @NonNull
  public String ternary(boolean a, String s) {
    return a ? s : s;
  }

  // Cases in which calling other method and returning doesn't require casting.
  // TODO(simionato): This creates casting even thought it's not needed, add a visitor to do
  // flow analysis to tighten nullability on local variables to fix this.
  @Nonnull
  public String castingNotNeeded() {
    String a = "";
    List<@NonNull Double> b = new ArrayList<>(); // VariableDeclaration
    b = new ArrayList<>(); // Binary Expression
    String c = null;
    m1(a, b, c);
    this.f1 = a;
    if (true) {
      // Cast not needed since NonNullableClass.getString returns !string.
      return ClassInSubpackage.getNonNullString();
    }
    return a;
  }

  // Cases in which casting is needed, since we can't infer nullability or we are using a library
  // that doesn't have nullability annotations.
  @Nonnull
  public String castingNeeded(@Nullable String a, Foo<@NonNull String> foo, @Nonnull String b) {
    this.f4 = Lists.newArrayList();
    this.f4 = new StringList();
    this.f4 = (List) new StringList();
    String x = m3("");
    Preconditions.checkNotNull(x);
    // JS Compiler can't infer that after this point x is not null.
    new Nullability(x);
    m3(x, x, x);
    this.f1 = x;
    b = x;
    foo.bar(x);
    b = foo.toString();
    if (true) {
      return x;
    }
    @NonNull String[] myArray = "Hel-lo".split("-");
    m3(myArray[0]);
    // Cast needed since NullableClass.getString returns ?string.
    return ClassInSubpackage.getString();
  }

  public void functions(@Nullable MyFunction function) {
    // Will have to generate a cast here.
    m4(function);
  }

  static class Foo<T> {
    void bar(T t) {}

    @Nullable
    T baz() {
      return null;
    }

    @Override
    public String toString() {
      return "Foo";
    }
  }

  @JsFunction
  interface MyFunction {
    @Nonnull
    String x(@Nonnull String a);
  }

  public static class StringList extends ArrayList<String> {}

  // Should implement Comparator<string>
  public static class StringComparator implements Comparator<@NonNull String> {
    @Override
    public int compare(@Nonnull String a, @Nonnull String b) {
      return 0;
    }
  }

  // Should implement Comparator<?string>
  public static class NullableStringComparator implements Comparator<@NullableType String> {
    @Override
    public int compare(@Nullable String a, @Nullable String b) {
      return 0;
    }
  }
}
