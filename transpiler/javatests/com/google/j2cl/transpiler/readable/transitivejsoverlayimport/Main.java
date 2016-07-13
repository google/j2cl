package com.google.j2cl.transpiler.readable.transitivejsoverlayimport;

public class Main {

  public static void main(String... args) {
    {
      Transitive transitive = null;
      transitive.doTransitiveInstanceMethod("arg1");
      transitive.getJsProperty();
    }

    {
      Immediate immediate = null;
      immediate.doTransitiveInstanceMethod("arg1");
      immediate.getJsProperty();
      immediate.doImmediateInstanceMethod();
    }

    {
      NonNativeUpper nonNativeUpper = null;
      nonNativeUpper.doTransitiveInstanceMethod("arg1");
      nonNativeUpper.getJsProperty();
      nonNativeUpper.doImmediateInstanceMethod();
      nonNativeUpper.doNonNativeUpperInstanceMethod();
    }

    {
      NonNativeLower nonNativeLower = null;
      nonNativeLower.doTransitiveInstanceMethod("arg1");
      nonNativeLower.getJsProperty();
      nonNativeLower.doImmediateInstanceMethod();
      nonNativeLower.doNonNativeUpperInstanceMethod();
      nonNativeLower.doNonNativeLowerInstanceMethod();
    }
  }
}
