package com.google.j2cl.transpiler.integration.simpleautoboxing;

public class Main {
  public Byte box(byte b) {
    return new Byte(b);
  }

  public Double box(double d) {
    return new Double(d);
  }

  public Float box(float f) {
    return new Float(f);
  }

  public Integer box(int i) {
    return new Integer(i);
  }

  public Long box(long l) {
    return new Long(l);
  }

  public Short box(short s) {
    return new Short(s);
  }

  public Boolean box(boolean b) {
    return new Boolean(b);
  }

  public Character box(char a) {
    return new Character(a);
  }

  public byte unbox(Byte b) {
    return b.byteValue();
  }

  public double unbox(Double d) {
    return d.doubleValue();
  }

  public float unbox(Float f) {
    return f.floatValue();
  }

  public int unbox(Integer i) {
    return i.intValue();
  }

  public long unbox(Long l) {
    return l.longValue();
  }

  public short unbox(Short s) {
    return s.shortValue();
  }

  public boolean unbox(Boolean b) {
    return b.booleanValue();
  }

  public char unbox(Character c) {
    return c.charValue();
  }

  public void testBoxByParameter() {
    byte b = (byte) 100;
    double d = 1111.0;
    float f = 1111.0f;
    int i = 1111;
    long l = 1111L;
    short s = (short) 100;
    boolean bool = true;
    char c = 'a';
    assert (unbox(b) == b);
    assert (unbox(d) == d);
    assert (unbox(f) == f);
    assert (unbox(i) == i);
    assert (unbox(l) == l);
    assert (unbox(s) == s);
    assert (unbox(bool) == bool);
    assert (unbox(c) == c);
  }

  public void testBoxByAssignment() {
    byte b = (byte) 100;
    double d = 1111.0;
    float f = 1111.0f;
    int i = 1111;
    long l = 1111L;
    short s = (short) 100;
    boolean bool = true;
    char c = 'a';
    Byte boxB = b;
    Double boxD = d;
    Float boxF = f;
    Integer boxI = i;
    Long boxL = l;
    Short boxS = s;
    Boolean boxBool = bool;
    Character boxC = c;
    assert (boxB.byteValue() == b);
    assert (boxD.doubleValue() == d);
    assert (boxF.floatValue() == f);
    assert (boxI.intValue() == i);
    assert (boxL.longValue() == l);
    assert (boxS.shortValue() == s);
    assert (boxBool.booleanValue() == bool);
    assert (boxC.charValue() == c);
  }

  public void testUnboxByParameter() {
    Byte boxB = new Byte((byte) 100);
    Double boxD = new Double(1111.0);
    Float boxF = new Float(1111.0f);
    Integer boxI = new Integer(1111);
    Long boxL = new Long(1111L);
    Short boxS = new Short((short) 100);
    Boolean boxBool = new Boolean(true);
    Character boxC = new Character('c');
    assert (box(boxB).equals(boxB));
    assert (box(boxD).equals(boxD));
    assert (box(boxF).equals(boxF));
    assert (box(boxI).equals(boxI));
    assert (box(boxL).equals(boxL));
    assert (box(boxS).equals(boxS));
    assert (box(boxBool).equals(boxBool));
    assert (box(boxC).equals(boxC));
  }

  public void testUnboxByAssignment() {
    Byte boxB = new Byte((byte) 100);
    Double boxD = new Double(1111.0);
    Float boxF = new Float(1111.0f);
    Integer boxI = new Integer(1111);
    Long boxL = new Long(1111L);
    Short boxS = new Short((short) 100);
    Boolean boxBool = new Boolean(true);
    Character boxC = new Character('a');
    byte b = boxB;
    double d = boxD;
    float f = boxF;
    int i = boxI;
    long l = boxL;
    short s = boxS;
    boolean bool = boxBool;
    char c = boxC;
    assert (b == boxB.byteValue());
    assert (d == boxD.doubleValue());
    assert (f == boxF.floatValue());
    assert (i == boxI.intValue());
    assert (l == boxL.longValue());
    assert (s == boxS.shortValue());
    assert (bool == boxBool.booleanValue());
    assert (c == boxC.charValue());
  }

  public void testUnboxByOperator() {
    // non side effect prefix operations
    Integer i = new Integer(1111);
    i = +i;
    assert i.intValue() == 1111;
    i = -i;
    assert i.intValue() == -1111;
    i = ~i;
    assert i.intValue() == 1110;
    Boolean bool = new Boolean(true);
    bool = !bool;
    assert !bool.booleanValue();

    // non side effect binary operations
    Integer i1 = new Integer(100);
    Integer i2 = new Integer(200);
    int sumI = i1 + i2;
    Integer boxSumI = i1 + i2;
    assert (sumI == 300);
    assert (boxSumI.intValue() == 300);

    Long l1 = new Long(1000L);
    Long l2 = new Long(2000L);
    long sumL = l1 + l2;
    Long boxSumL = l1 + l2;
    assert (sumL == 3000L);
    assert (boxSumL.longValue() == sumL);

    Double d1 = new Double(1111.1);
    Double d2 = new Double(2222.2);
    double sumD = d1 + d2;
    Double boxSumD = d1 + d2;
    assert (boxSumD.doubleValue() == sumD);

    Boolean b1 = new Boolean(true);
    Boolean b2 = new Boolean(false);
    Boolean boxB = b1 && b2;
    boolean b = b1 || b2;
    assert (!boxB.booleanValue());
    assert (b);
  }

  private int foo = 0;
  public void testNull() {
    // Avoiding a "condition always evaluates to true" error in JSComp type checking.
    Object maybeNull = foo == 0 ? null : new Object();

    Boolean bool = null;
    Double d = null;
    Integer i = null;
    Long l = null;
    assert bool == maybeNull;
    assert d == maybeNull;
    assert i == maybeNull;
    assert l == maybeNull;
  }

  public static void main(String[] args) {
    Main m = new Main();
    m.testBoxByParameter();
    m.testBoxByAssignment();
    m.testUnboxByParameter();
    m.testUnboxByAssignment();
    m.testUnboxByOperator();
    m.testNull();
  }
}
