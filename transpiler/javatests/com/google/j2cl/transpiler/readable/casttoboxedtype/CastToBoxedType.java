package com.google.j2cl.transpiler.readable.casttoboxedtype;

public class CastToBoxedType {
  @SuppressWarnings("unused")
  public void test() {
    Object o = new Integer(1);
    Byte b = (Byte) o;
    Double d = (Double) o;
    Float f = (Float) o;
    Integer i = (Integer) o;
    Long l = (Long) o;
    Short s = (Short) o;
    Number n = (Number) o;
    Character c = (Character) o;
    Boolean bool = (Boolean) o;
  }
}
