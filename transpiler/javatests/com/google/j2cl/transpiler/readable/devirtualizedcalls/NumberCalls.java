package com.google.j2cl.transpiler.readable.devirtualizedcalls;

public class NumberCalls {
  public void main() {
    Number i = new Integer(1);
    i.byteValue();
    i.doubleValue();
    i.floatValue();
    i.intValue();
    i.longValue();
    i.shortValue();

    Integer ii = new Integer(1);
    ii.byteValue();
    ii.doubleValue();
    ii.floatValue();
    ii.intValue();
    ii.longValue();
    ii.shortValue();

    Number d = new Double(1.1);
    d.byteValue();
    d.doubleValue();
    d.floatValue();
    d.intValue();
    d.longValue();
    d.shortValue();

    Double dd = new Double(1.1);
    dd.byteValue();
    dd.doubleValue();
    dd.floatValue();
    dd.intValue();
    dd.longValue();
    dd.shortValue();

    Number s = new Short((short) 1);
    s.byteValue();
    s.doubleValue();
    s.floatValue();
    s.intValue();
    s.longValue();
    s.shortValue();

    Short ss = new Short((short) 1);
    ss.byteValue();
    ss.doubleValue();
    ss.floatValue();
    ss.intValue();
    ss.longValue();
    ss.shortValue();

    Number b = new Byte((byte) 1);
    b.byteValue();
    b.doubleValue();
    b.floatValue();
    b.intValue();
    b.longValue();
    b.shortValue();

    Byte bb = new Byte((byte) 1);
    bb.byteValue();
    bb.doubleValue();
    bb.floatValue();
    bb.intValue();
    bb.longValue();
    bb.shortValue();

    Number f = new Float(1.1f);
    f.byteValue();
    f.doubleValue();
    f.floatValue();
    f.intValue();
    f.longValue();
    f.shortValue();

    Float ff = new Float(1.1f);
    ff.byteValue();
    ff.doubleValue();
    ff.floatValue();
    ff.intValue();
    ff.longValue();
    ff.shortValue();

    Number l = new Long(1L);
    l.byteValue();
    l.doubleValue();
    l.floatValue();
    l.intValue();
    l.longValue();
    l.shortValue();

    Long ll = new Long(1L);
    ll.byteValue();
    ll.doubleValue();
    ll.floatValue();
    ll.intValue();
    ll.longValue();
    ll.shortValue();
  }
}
