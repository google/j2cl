package com.google.j2cl.transpiler.regression.compiler;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
  ArraysTest.class,
  AutoboxTest.class,
  CompilerMiscRegressionTest.class,
  CompilerTest.class,
  EnhancedForLoopTest.class,
  EnumsTest.class,
  FieldInitializationOrderTest.class,
  GenericCastTest.class,
  InnerClassTest.class,
  InnerOuterSuperTest.class,
  MethodBindTest.class,
  MethodCallTest.class,
  MiscellaneousTest.class,
  NativeDevirtualizationTest.class,
  ObjectIdentityTest.class,
  VarargsTest.class
})
public class CompilerSuite {}
