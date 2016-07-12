package com.google.j2cl.transpiler.readable.transitivejsoverlayimport;

public class Main {

  public static void main(String... args) {
    {
      Transitive transitive = null;
      transitive.doTransitiveInstanceMethod();
      transitive.getJsProperty();
    }

    {
      Immediate immediate = null;
      immediate.doTransitiveInstanceMethod();
      immediate.getJsProperty();
      immediate.doImmediateInstanceMethod();
    }

    {
      NonNativeUpper nonNativeUpper = null;
      nonNativeUpper.doTransitiveInstanceMethod();
      nonNativeUpper.getJsProperty();
      nonNativeUpper.doImmediateInstanceMethod();
      nonNativeUpper.doNonNativeUpperInstanceMethod();
    }

    {
      NonNativeLower nonNativeLower = null;
      nonNativeLower.doTransitiveInstanceMethod();
      nonNativeLower.getJsProperty();
      nonNativeLower.doImmediateInstanceMethod();
      nonNativeLower.doNonNativeUpperInstanceMethod();
      nonNativeLower.doNonNativeLowerInstanceMethod();
    }
  }
}
