package com.google.j2cl.transpiler.readable.bridgemethodcasts;

public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> {
  @Override
  public V put(K key, V value) {
    return value;
  }
}
