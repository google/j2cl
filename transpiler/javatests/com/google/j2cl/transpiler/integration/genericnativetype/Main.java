package com.google.j2cl.transpiler.integration.genericnativetype;

public class Main {
  public static void main(String... args) {
    BoundedJsArrays.create(10);
    BoundedJsArrays.createWithSize(10, 10);
  }

  public static <V> BoundedJsArray<V> test(V v) {
    return BoundedJsArray.<V>create(10);
  }
}
