package com.google.j2cl.transpiler.readable.instanceofboxedtype;

public class InstanceofBoxedType {
  @SuppressWarnings("unused")
  public void test() {
    Object b = new Integer(1);

    boolean a = b instanceof Byte;
    a = b instanceof Double;
    a = b instanceof Float;
    a = b instanceof Integer;
    a = b instanceof Long;
    a = b instanceof Short;
    a = b instanceof Number;
    a = b instanceof Character;
    a = b instanceof Boolean;
  }
}
