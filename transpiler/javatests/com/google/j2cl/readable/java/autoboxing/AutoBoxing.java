/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package autoboxing;

import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsMethod;

@SuppressWarnings({
  "IdentityBinaryExpression",
  "BoxedPrimitiveConstructor",
  "ReferenceEquality",
  "ShortCircuitBoolean"
})
public class AutoBoxing {
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

  public void takesFloatVarArgs(Float... elements) {}

  public double takesObjectAndReturnsPrimitiveDouble(@DoNotAutobox Object o) {
    return (double) o;
  }

  public double sumWithoutBoxing(@DoNotAutobox Object... numbers) {
    double sum = 0;
    for (Object number : numbers) {
      sum += (Double) number;
    }
    return sum;
  }

  @JsMethod
  public double sumWithoutBoxingJsVarargs(@DoNotAutobox Object... numbers) {
    double sum = 0;
    for (Object number : numbers) {
      sum += (Double) number;
    }
    return sum;
  }

  public double sumWithUnboxing(Iterable<Double> boxedDoubles) {
    double sum = 0;
    for (double d : boxedDoubles) {
      sum += d;
    }
    return sum;
  }

  public static final float COMPILE_TIME_CONSTANT = 1.1f;

  @SuppressWarnings("unused")
  public void testBoxing() {
    boolean bool = true;
    double d = 2.2;
    byte b = (byte) 1;
    float f = 1.1f;
    int i = 1;
    long l = 2L;
    short s = (short) 1;
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
    boxBool = true;
    boxD = 2.2;
    boxB = 1;
    boxF = 1.1f;
    boxI = 1;
    boxL = 2L;
    boxS = 1;
    boxC = 1;
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
    double unusedDouble = takesObjectAndReturnsPrimitiveDouble(4);
    unusedDouble = sumWithoutBoxing(1, 2.2, (byte) 1, (short) 1, (float) 2.2);
    unusedDouble = sumWithoutBoxingJsVarargs(1, 2.2, (byte) 1, (short) 1, (float) 2.2);
    takesFloatVarArgs(1.1f, (float) 'a', (float) 2.2);

    // auto-boxing by assignment to Object
    Object o;
    o = 2.2d;
    o = 1.1f;
    o = 1;
    o = 'a';

    // auto-boxing by assignment
    boxBool = boxBool && boxBool;
    boxD = boxD + boxD;
    boxI = boxI / boxI;
    boxL = boxL / boxL;
    boxBool = !boxBool;
    boxI = +boxI;
    boxI = -boxI;

    boxI <<= boxI;
    boxI <<= boxL;
    boxL <<= boxI;
    boxL <<= boxL;

    // auto-boxing by intersection cast.
    o = (Integer & Comparable<Integer>) 15;
  }

  @SuppressWarnings("unused")
  public void testUnboxing() {
    Boolean boxBool = new Boolean(true);
    Double boxD = new Double(2.2);
    Byte boxB = new Byte((byte) 1);
    Float boxF = new Float(1.1f);
    Integer boxI = new Integer(1);
    Long boxL = new Long(1L);
    Short boxS = new Short((short) 1);
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
    boxD = -boxD;
    boxI = -boxI;
    switch (boxI) {
      default:
    }

    i += boxI += i += boxI;

    i <<= boxI;
    i <<= boxL;
    l <<= boxI;
    l <<= boxL;
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

    Boolean boxB = null;
    boxB = !boxB;
    boxB = boxB && boxB;
    boxB = boxB ? boxB : boxB;
    if (boxB) {}
    boolean b = false;
    b |= boxB |= b |= boxB;
  }

  @SuppressWarnings("unused")
  public void testUnboxingEquality() {
    Boolean boxB = new Boolean(true);
    boolean b = false;

    assert boxB == boxB;
    assert boxB == b;

    assert b != b;
    assert b != boxB;

    Integer boxI = new Integer(1);
    int i = 1;

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

  public static <T extends Long> void testUnboxingFromTypeVariable() {
    T n = (T) (Long) 10L;
    // auto-unboxing from variable n.
    long l = n;
    assert l == 10L;

    n++;
    ++n;
    n = n++;
    n = ++n;

    class Local<T extends Long> {
      long toLong(T l) {
        // auto-unboxing from variable l.
        assert l.equals(11L);
        return l;
      }
    }
    l = new Local<>().toLong(11L);
    assert l == 11L;
  }

  public static <T extends Long & Comparable<Long>> void testUnboxingFromIntersectionType() {
    T n = (T) (Long) 10L;
    // auto-unboxing from variable n.
    long l = n;
    assert l == 10L;

    n++;
    ++n;
    n = n++;
    n = ++n;

    class Local<T extends Long & Comparable<Long>> {
      long toLong(T l) {
        // auto-unboxing from variable l.
        assert l.equals(11L);
        return l;
      }
    }
    // auto-boxing parameter.
    l = new Local<>().toLong(11L);
    assert l == 11L;

    int i = (Integer & Comparable<Integer>) 10;
  }

  public void testUnbox_withCast() {
    class Supplier<T> {
      T get() {
        return null;
      }
    }
    Supplier<Integer> supplier = new Supplier<>();
    int i = (int) supplier.get();
  }
}
