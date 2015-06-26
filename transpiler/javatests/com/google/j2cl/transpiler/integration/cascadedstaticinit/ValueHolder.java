package com.google.j2cl.transpiler.integration.cascadedstaticinit;

/**
 * Class that is used to hold two static values, and whose $clinit will be called by
 * its static getter/setter.
 */
public class ValueHolder {
  public static int bar = 5;
  public static int foo = bar * 5;
}
