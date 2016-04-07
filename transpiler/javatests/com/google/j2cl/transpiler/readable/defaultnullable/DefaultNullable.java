package com.google.j2cl.transpiler.readable.defaultnullable;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DefaultNullable {
  private String a = "Hello";
  @Nullable private String b = null;
  private List<String> c = new ArrayList<>();
  @Nullable private List<String> d = new ArrayList<>();
  private List<@Nullable String> e = new ArrayList<>();
  @Nullable private List<@Nullable String> f = null;
}
