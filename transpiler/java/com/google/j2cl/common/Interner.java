package com.google.j2cl.common;

import com.google.common.collect.Interners;

/** Wrapper for guava Strong Interner. */
public class Interner<T> {
  private final com.google.common.collect.Interner<T> interner;

  public Interner() {
    this.interner = Interners.newStrongInterner();
  }

  public T intern(T sample) {
    return this.interner.intern(sample);
  }
}
