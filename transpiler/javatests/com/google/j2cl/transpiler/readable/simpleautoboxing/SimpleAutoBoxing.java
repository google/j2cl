package com.google.j2cl.transpiler.readable.simpleautoboxing;

@SuppressWarnings({
  "IdentityBinaryExpression",
  "BoxedPrimitiveConstructor",
  "ReferenceEquality",
  "ShortCircuitBoolean"
})
public class SimpleAutoBoxing {
  public Boolean box(boolean b) {
    return b; // auto-boxing by return
  }

  public Double box(double d) {
    return d; // auto-boxing by return
  }

  public Byte box(byte b) {
    return b; // auto-boxing by return
  }

  public Float box(float f) {
    return f; // auto-boxing by return
  }

  public Integer box(int i) {
    return i; // auto-boxing by return
  }

  public Long box(long l) {
    return l; // auto-boxing by return
  }

  public Short box(short s) {
    return s; // auto-boxing by return
  }

  public Character box(char c) {
    return c; // auto-boxing by return
  }

  public boolean unbox(Boolean b) {
    return b; // auto-unboxing by return
  }

  public double unbox(Double d) {
    return d; // auto-unboxing by return
  }

  public byte unbox(Byte b) {
    return b; // auto-unboxing by return
  }

  public float unbox(Float f) {
    return f; // auto-unboxing by return
  }

  public int unbox(Integer i) {
    return i; // auto-unboxing by return
  }

  public long unbox(Long l) {
    return l; // auto-unboxing by return
  }

  public short unbox(Short s) {
    return s; // auto-unboxing by return
  }

  public char unbox(Character c) {
    return c; // auto-unboxing by return
  }

  public double takesAndReturnsPrimitiveDouble(double d) {
    return d;
  }

  public Void takesAndReturnsVoid(Void v) {
    return null;
  }

  @SuppressWarnings("unused")
  public void testNull() {
    Boolean b = null;
    Double d = null;
    Integer i = null;
    Long l = null;
  }

  @SuppressWarnings("unused")
  public void testBoxing() {
    boolean bool = true;
    double d = 1111.0;
    byte b = (byte) 100;
    float f = 1111.0f;
    int i = 1111;
    long l = 1111L;
    short s = (short) 100;
    char c = 'a';

    // auto-boxing by assignment
    Boolean boxBool = bool;
    Double boxD = d;
    Byte boxB = b;
    Float boxF = f;
    Integer boxI = i;
    Long boxL = l;
    Short boxS = s;
    Character boxC = c;

    // auto-boxing by assignment with literals
    Object o = 0;
    boxBool = true;
    boxD = 100.0;
    boxB = 0;
    boxF = 100.0f;
    boxI = 1000;
    boxL = 1000L;
    boxS = 0;
    boxC = 0;
    boxC = 'a';

    // auto-boxing by parameter
    bool = unbox(bool);
    d = unbox(d);
    b = unbox(b);
    f = unbox(f);
    i = unbox(i);
    l = unbox(l);
    s = unbox(s);
    c = unbox(c);

    // auto-boxing by assignment
    boxBool = boxBool && boxBool;
    boxD = boxD + boxD;
    boxF = boxF - boxF;
    boxI = boxI * boxI;
    boxL = boxL / boxL;
    boxBool = !boxBool;
    boxI = +boxI;
    boxI = -boxI;
    boxI = ~boxI;
  }

  @SuppressWarnings("unused")
  public void testUnboxing() {
    Boolean boxBool = new Boolean(true);
    Double boxD = new Double(1111.0);
    Byte boxB = new Byte((byte) 100);
    Float boxF = new Float(1111.0f);
    Integer boxI = new Integer(1111);
    Long boxL = new Long(1111L);
    Short boxS = new Short((short) 100);
    Character boxC = new Character('a');

    // auto-unboxing by assignment
    boolean bool = boxBool;
    double d = boxD;
    byte b = boxB;
    float f = boxF;
    int i = boxI;
    long l = boxL;
    short s = boxS;
    char c = boxC;

    // auto-unboxing by parameter
    boxBool = box(boxBool);
    boxD = box(boxD);
    boxB = box(boxB);
    boxF = box(boxF);
    boxI = box(boxI);
    boxL = box(boxL);
    boxS = box(boxS);
    boxC = box(boxC);

    // auto-unboxing and widening by assignment
    d = boxB;
    d = boxF;
    d = boxI;
    d = boxL;
    d = boxS;
    d = boxC;

    // auto-unboxing and widening by parameter
    takesAndReturnsPrimitiveDouble(boxB);
    takesAndReturnsPrimitiveDouble(boxF);
    takesAndReturnsPrimitiveDouble(boxI);
    takesAndReturnsPrimitiveDouble(boxL);
    takesAndReturnsPrimitiveDouble(boxS);
    takesAndReturnsPrimitiveDouble(boxC);

    Void v = takesAndReturnsVoid(takesAndReturnsVoid(null));

    // auto-unboxing by operator
    bool = boxBool && boxBool;
    d = boxD + boxD;
    f = boxF - boxF;
    i = boxI * boxI;
    l = boxL / boxL;
    bool = !boxBool;
    i = +boxI;
    i = -boxI;
    i = ~boxI;
  }

  @SuppressWarnings("unused")
  public void testUnboxingBoolean() {
    Boolean boxB1 = new Boolean(true);
    Boolean boxB2 = new Boolean(false);

    boolean br;
    boolean boxr;

    boxr = boxB1 == boxB2;
    br = boxB1 == boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 != boxB2;
    br = boxB1 != boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 ^ boxB2;
    br = boxB1 ^ boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 & boxB2;
    br = boxB1 & boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 | boxB2;
    br = boxB1 | boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 && boxB2;
    br = boxB1 && boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 || boxB2;
    br = boxB1 || boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 = boxB2;
    br = boxB1 = boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 &= boxB2;
    br = boxB1 &= boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 |= boxB2;
    br = boxB1 |= boxB2;
    assert boxr;
    assert br;

    boxr = boxB1 ^= boxB2;
    br = boxB1 ^= boxB2;
    assert boxr;
    assert br;
  }

  @SuppressWarnings("unused")
  public void testUnboxingEquality() {
    Boolean boxB = new Boolean(true);
    boolean b = false;

    assert boxB == boxB;
    assert boxB == b;

    assert b != b;
    assert b != boxB;

    Integer boxI = new Integer(100);
    int i = 101;

    assert boxI == boxI;
    assert boxI == i;

    assert i != i;
    assert i != boxI;
  }

  public void testAssertStatement() {
    Boolean boxB = new Boolean(true);
    boolean b = true;

    assert boxB;
    assert b;
  }
}
