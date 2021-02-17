package java.util.concurrent;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsType;

public class ThreadLocalRandom {

  private static final ThreadLocalRandom instance = new ThreadLocalRandom();

  public static ThreadLocalRandom current() {
    return instance;
  }

  public double nextDouble() {
    return NativeMath.random();
  }

  @JsType(isNative = true, name = "Math", namespace = GLOBAL)
  public static class NativeMath {
    public static native double random();
  }
}
