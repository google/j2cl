package com.google.j2cl.transpiler.readable.bridgemethodcasts;

public class AbstractMap<K, V> implements Map<K, V> {
  @Override
  public V put(K key, V value) {
    return value;
  }
}
