package com.google.j2cl.transpiler.integration.genericnativetype;

public class BoundedJsArrays {
  private BoundedJsArrays() {}

  public static <V> BoundedJsArray<V> create(int bounds) {
    return BoundedJsArray.<V>create(bounds);
  }

  public static <V> BoundedJsArray<V> createWithSize(int bounds, int size) {
    return BoundedJsArray.<V>create(bounds, size);
  }
}
