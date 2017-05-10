package a;

import n.NativeType;

public abstract class A<T extends NativeType> {
  public T getType() {
    return null;
  }

  public static <T extends NativeType> A<T> newA() {
    return null;
  }
}
