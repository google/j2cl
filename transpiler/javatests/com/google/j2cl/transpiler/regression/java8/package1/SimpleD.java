package com.google.j2cl.transpiler.regression.java8.package1;

import com.google.j2cl.transpiler.regression.java8.package2.SimpleB;

/**
 * A class overrides the package-private method of super class and the default interface method of
 * super interface.
 */
public class SimpleD extends SimpleA implements SimpleB {
  @Override
  public String m() {
    return super.m() + SimpleB.super.m();
  }
}
