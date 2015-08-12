package com.google.j2cl.transpiler.integration.numberobjectcalls;

public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    Byte b = new Byte((byte) 1);
    Double d = new Double(1.0);
    Float f = new Float(1.0f);
    Integer i = new Integer(1);
    Long l = new Long(1L);
    Short s = new Short((short) 1);

    // equals
    assert (b.equals(b));
    assert (b.equals(new Byte((byte) 1)));
    assert (d.equals(d));
    assert (d.equals(new Double(1.0)));
    assert (f.equals(f));
    assert (f.equals(new Float(1.0f)));
    assert (i.equals(i));
    assert (i.equals(new Integer(1)));
    assert (l.equals(l));
    assert (l.equals(new Long(1L)));
    assert (s.equals(s));
    assert (s.equals(new Short((short) 1)));
    assert (!l.equals(i)); // Long is transpiled fine.
    // assert (!b.equals(i)); // b.equals(i) returns true in the transpiled code.

    // hashCode
    assert (b.hashCode() == b.hashCode());
    assert (d.hashCode() == d.hashCode());
    assert (f.hashCode() == f.hashCode());
    assert (i.hashCode() == i.hashCode());
    assert (l.hashCode() == l.hashCode());
    assert (s.hashCode() == s.hashCode());
    assert (b.hashCode() == i.hashCode());
    assert (l.hashCode() == i.hashCode());
    assert (new Long(9223372036854775807L).hashCode() == -2147483648);

    // toString
    assert (b.toString().equals("1"));
    // assert (d.toString().equals("1.0")); // d.toString().equals("1")
    // assert (f.toString().equals("1.0")); // f.toString().equals("1")
    assert (i.toString().equals("1"));
    assert (l.toString().equals("1"));
    assert (s.toString().equals("1"));

    // getClass
    assert (b.getClass() instanceof Class);
    assert (d.getClass() instanceof Class);
    assert (f.getClass() instanceof Class);
    assert (i.getClass() instanceof Class);
    assert (l.getClass() instanceof Class);
    assert (s.getClass() instanceof Class);
    // getClass().getName() returns "double", and l.getClass().getName() returns "long"
    //    assert (b.getClass().getName().equals("java.lang.Byte"));
    //    assert (d.getClass().getName().equals("java.lang.Double"));
    //    assert (f.getClass().getName().equals("java.lang.Float"));
    //    assert (i.getClass().getName().equals("java.lang.Integer"));
    //    assert (l.getClass().getName().equals("java.lang.Long"));
    //    assert (s.getClass().getName().equals("java.lang.Short"));
  }
}
