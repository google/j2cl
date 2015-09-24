package com.google.j2cl.transpiler.readable.nobridgemethods;

public class TreeMap<K> extends Map<K> {
  static class InnerEntry<K> implements Entry<K> {}

  @Override
  public InnerEntry<K> getCeilingEntry(K key) {
    return new InnerEntry<K>();
  }
}
