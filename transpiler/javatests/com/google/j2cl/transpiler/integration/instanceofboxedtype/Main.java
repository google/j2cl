package com.google.j2cl.transpiler.integration.instanceofboxedtype;

@SuppressWarnings("BoxedPrimitiveConstructor")
public class Main {
  public static void main(String[] args) {
    Object b = new Byte((byte) 1);
    Object d = new Double(1.0);
    Object f = new Float(1.0f);
    Object i = new Integer(1);
    Object l = new Long(1L);
    Object s = new Short((short) 1);
    Object c = new Character('a');
    Object bool = new Boolean(true);
    Object sn = new SubNumber();

    assert b instanceof Byte;
    assert !(b instanceof Double);
    assert !(b instanceof Float);
    assert !(b instanceof Integer);
    assert !(b instanceof Long);
    assert !(b instanceof Short);
    assert b instanceof Number;
    assert !(b instanceof Character);
    assert !(b instanceof Boolean);

    assert !(d instanceof Byte);
    assert d instanceof Double;
    assert !(d instanceof Float);
    assert !(d instanceof Integer);
    assert !(d instanceof Long);
    assert !(d instanceof Short);
    assert d instanceof Number;
    assert !(d instanceof Character);
    assert !(d instanceof Boolean);

    assert !(f instanceof Byte);
    assert !(f instanceof Double);
    assert f instanceof Float;
    assert !(f instanceof Integer);
    assert !(f instanceof Long);
    assert !(f instanceof Short);
    assert f instanceof Number;
    assert !(f instanceof Character);
    assert !(f instanceof Boolean);

    assert !(i instanceof Byte);
    assert !(i instanceof Double);
    assert !(i instanceof Float);
    assert i instanceof Integer;
    assert !(i instanceof Long);
    assert !(i instanceof Short);
    assert i instanceof Number;
    assert !(i instanceof Character);
    assert !(i instanceof Boolean);

    assert !(l instanceof Byte);
    assert !(l instanceof Double);
    assert !(l instanceof Float);
    assert !(l instanceof Integer);
    assert l instanceof Long;
    assert !(l instanceof Short);
    assert l instanceof Number;
    assert !(l instanceof Character);
    assert !(l instanceof Boolean);

    assert !(s instanceof Byte);
    assert !(s instanceof Double);
    assert !(s instanceof Float);
    assert !(s instanceof Integer);
    assert !(s instanceof Long);
    assert s instanceof Short;
    assert s instanceof Number;
    assert !(s instanceof Character);
    assert !(s instanceof Boolean);

    assert !(c instanceof Byte);
    assert !(c instanceof Double);
    assert !(c instanceof Float);
    assert !(c instanceof Integer);
    assert !(c instanceof Long);
    assert !(c instanceof Short);
    assert !(c instanceof Number);
    assert c instanceof Character;
    assert !(c instanceof Boolean);

    assert !(bool instanceof Byte);
    assert !(bool instanceof Double);
    assert !(bool instanceof Float);
    assert !(bool instanceof Integer);
    assert !(bool instanceof Long);
    assert !(bool instanceof Short);
    assert !(bool instanceof Number);
    assert !(bool instanceof Character);
    assert bool instanceof Boolean;

    assert (sn instanceof SubNumber);
    assert (sn instanceof Number);

    assert (!(new Object() instanceof Void));
    assert (!(null instanceof Void));
  }
}
