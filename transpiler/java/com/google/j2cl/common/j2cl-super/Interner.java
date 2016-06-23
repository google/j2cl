package com.google.j2cl.common;

import java.util.HashMap;

/** J2cl's Interner implementation. */
public class Interner<T> {
  private HashMap<T, T> interned;

  public Interner() {
    this.interned = new HashMap<T, T>();
  }

  public T intern(T toIntern) {
    T canonical = interned.get(toIntern);
    if (canonical != null) {
      return canonical;
    } else {
      interned.put(toIntern, toIntern);
      return toIntern;
    }
  }
}
