package com.google.j2cl.transpiler.readable.genericequals;

public class GenericEquals<T> {

  private final T value;

  public GenericEquals(T value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof GenericEquals)) {
      return false;
    }
    GenericEquals<?> other = (GenericEquals<?>) obj;
    return value.equals(other.value);
  }
}
