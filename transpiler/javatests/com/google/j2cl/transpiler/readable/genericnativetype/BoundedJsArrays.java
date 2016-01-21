package com.google.j2cl.transpiler.readable.genericnativetype;

public class BoundedJsArrays {
  private BoundedJsArrays() {}

  public static <V> BoundedJsArray<V> create() {
    return BoundedJsArray.<V>create();
  }

  public static <V> BoundedJsArray<V> createWithSize(int size) {
    return BoundedJsArray.<V>create(size);
  }
}
