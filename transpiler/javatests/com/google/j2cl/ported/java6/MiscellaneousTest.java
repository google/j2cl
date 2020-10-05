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
package com.google.j2cl.ported.java6;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Miscellaneous tests. */
@RunWith(JUnit4.class)
@SuppressWarnings({"MethodCanBeStatic", "ConstantField", "MissingDefault"})
public class MiscellaneousTest {

  interface I {}

  interface IBar extends I {}

  interface IFoo extends I {}

  static class PolyA implements IFoo {
    @Override
    public String toString() {
      return "A";
    }
  }

  static class PolyB implements IBar {
    @Override
    public String toString() {
      return "B";
    }
  }

  private static class HasClinit {
    public static int i = 1;

    private static HasClinit sInstance = new HasClinit();

    public static int sfoo() {
      return sInstance.foo();
    }

    private int foo() {
      //       this.toString();
      return 3;
    }
  }

  private static volatile boolean FALSE = false;

  private static volatile boolean TRUE = true;

  private static void assertAllCanStore(Object[] dest, Object[] src) {
    for (int i = 0; i < src.length; ++i) {
      dest[0] = src[i];
    }
  }

  private static void assertNoneCanStore(Object[] dest, Object[] src) {
    for (int i = 0; i < src.length; ++i) {
      try {
        dest[0] = src[i];
        fail();
      } catch (ArrayStoreException expected) {
      }
    }
  }

  @Test
  public void testArrayCasts() {
    {
      // thwart optimizer
      Object f1 = FALSE ? (Object) new PolyA() : (Object) new IFoo[1];
      if (expectClassMetadata()) {
        assertThat(f1.getClass().getName())
            .isEqualTo("[Lcom.google.j2cl.ported.java6.MiscellaneousTest$IFoo;");
      }
      assertThat(f1 instanceof PolyA[][]).isFalse();
      assertThat(f1 instanceof IFoo[][]).isFalse();
      assertThat(f1 instanceof PolyA[]).isFalse();
      assertThat(f1 instanceof IFoo[]).isTrue();
      assertThat(f1 instanceof PolyA).isFalse();
      assertThat(f1 instanceof IFoo).isFalse();
      assertThat(f1 instanceof Object[]).isTrue();
      assertThat(f1 instanceof Object[][]).isFalse();

      assertAllCanStore((Object[]) f1, new Object[] {new PolyA(), new IFoo() {}});
      assertNoneCanStore((Object[]) f1, new Object[] {new PolyB(), new Object(), new Object[0]});
    }

    {
      // thwart optimizer
      Object a1 = FALSE ? (Object) new PolyA() : (Object) new PolyA[1];
      if (expectClassMetadata()) {
        assertThat(a1.getClass().getName())
            .isEqualTo("[Lcom.google.j2cl.ported.java6.MiscellaneousTest$PolyA;");
      }
      assertThat(a1 instanceof PolyA[][]).isFalse();
      assertThat(a1 instanceof IFoo[][]).isFalse();
      assertThat(a1 instanceof PolyA[]).isTrue();
      assertThat(a1 instanceof IFoo[]).isTrue();
      assertThat(a1 instanceof PolyA).isFalse();
      assertThat(a1 instanceof IFoo).isFalse();
      assertThat(a1 instanceof Object[]).isTrue();
      assertThat(a1 instanceof Object[][]).isFalse();

      assertAllCanStore((Object[]) a1, new Object[] {new PolyA()});
      assertNoneCanStore(
          (Object[]) a1, new Object[] {new IFoo() {}, new PolyB(), new Object(), new Object[0]});
    }

    {
      // thwart optimizer
      Object f2 = FALSE ? (Object) new PolyA() : (Object) new IFoo[1][];
      if (expectClassMetadata()) {
        assertThat(f2.getClass().getName())
            .isEqualTo("[[Lcom.google.j2cl.ported.java6.MiscellaneousTest$IFoo;");
      }
      assertThat(f2 instanceof PolyA[][]).isFalse();
      assertThat(f2 instanceof IFoo[][]).isTrue();
      assertThat(f2 instanceof PolyA[]).isFalse();
      assertThat(f2 instanceof IFoo[]).isFalse();
      assertThat(f2 instanceof PolyA).isFalse();
      assertThat(f2 instanceof IFoo).isFalse();
      assertThat(f2 instanceof Object[]).isTrue();
      assertThat(f2 instanceof Object[][]).isTrue();

      assertAllCanStore((Object[]) f2, new Object[] {new PolyA[0], new IFoo[0]});
      assertNoneCanStore(
          (Object[]) f2, new Object[] {new IFoo() {}, new PolyB(), new Object(), new Object[0]});
    }

    {
      // thwart optimizer
      Object a2 = FALSE ? (Object) new PolyA() : (Object) new PolyA[1][];
      if (expectClassMetadata()) {
        assertThat(a2.getClass().getName())
            .isEqualTo("[[Lcom.google.j2cl.ported.java6.MiscellaneousTest$PolyA;");
      }
      assertThat(a2 instanceof PolyA[][]).isTrue();
      assertThat(a2 instanceof IFoo[][]).isTrue();
      assertThat(a2 instanceof PolyA[]).isFalse();
      assertThat(a2 instanceof IFoo[]).isFalse();
      assertThat(a2 instanceof PolyA).isFalse();
      assertThat(a2 instanceof IFoo).isFalse();
      assertThat(a2 instanceof Object[]).isTrue();
      assertThat(a2 instanceof Object[][]).isTrue();

      assertAllCanStore((Object[]) a2, new Object[] {new PolyA[0]});
      assertNoneCanStore(
          (Object[]) a2,
          new Object[] {new IFoo() {}, new PolyB(), new Object(), new Object[0], new IFoo[0]});
    }
  }

  @Test
  public void testReferenceArrays() {
    Integer[] c = new Integer[] {1, 2};
    Integer[][] d = new Integer[][] {{1, 2}, {3, 4}};
    Integer[][][] e = new Integer[][][] {{{1, 2}, {3, 4}}, {{5, 6}, {7, 8}}};
    if (expectClassMetadata()) {
      assertThat(c.getClass().getName()).isEqualTo("[Ljava.lang.Integer;");
      assertThat(d.getClass().getName()).isEqualTo("[[Ljava.lang.Integer;");
      assertThat(d[1].getClass().getName()).isEqualTo("[Ljava.lang.Integer;");
      assertThat(e.getClass().getName()).isEqualTo("[[[Ljava.lang.Integer;");
      assertThat(e[1].getClass().getName()).isEqualTo("[[Ljava.lang.Integer;");
      assertThat(e[1][1].getClass().getName()).isEqualTo("[Ljava.lang.Integer;");
    }
    assertThat(c[1]).isEqualTo(2);
    assertThat(d[1][0]).isEqualTo(3);
    assertThat(e[1][1][1]).isEqualTo(8);

    int[][][] b = new int[3][2][1];
    b[2][1][0] = 1;
    b = new int[3][2][];
    b[2][1] = null;
    b = new int[3][][];
    b[2] = null;
  }

  @Test
  public void testPrimitiveArrays() {
    int[] c = new int[] {1, 2};
    int[][] d = new int[][] {{1, 2}, {3, 4}};
    int[][][] e = new int[][][] {{{1, 2}, {3, 4}}, {{5, 6}, {7, 8}}};
    if (expectClassMetadata()) {
      assertThat(c.getClass().getName()).isEqualTo("[I");
      assertThat(d.getClass().getName()).isEqualTo("[[I");
      assertThat(d[1].getClass().getName()).isEqualTo("[I");
      assertThat(e.getClass().getName()).isEqualTo("[[[I");
      assertThat(e[1].getClass().getName()).isEqualTo("[[I");
      assertThat(e[1][1].getClass().getName()).isEqualTo("[I");
    }
    assertThat(c[1]).isEqualTo(2);
    assertThat(d[1][0]).isEqualTo(3);
    assertThat(e[1][1][1]).isEqualTo(8);

    int[][][] b = new int[3][2][1];
    b[2][1][0] = 1;
    b = new int[3][2][];
    b[2][1] = null;
    b = new int[3][][];
    b[2] = null;

    assertThat(int[].class.getName()).isEqualTo("[I");
    assertThat(short[].class.getName()).isEqualTo("[S");
    assertThat(byte[].class.getName()).isEqualTo("[B");
    assertThat(char[].class.getName()).isEqualTo("[C");
    assertThat(long[].class.getName()).isEqualTo("[J");
    assertThat(float[].class.getName()).isEqualTo("[F");
    assertThat(double[].class.getName()).isEqualTo("[D");
    assertThat(boolean[].class.getName()).isEqualTo("[Z");
  }

  @Test
  public void testAssociativityCond() {
    int result = (TRUE ? TRUE : FALSE) ? 100 : 200;
    assertThat(result).isEqualTo(100);
  }

  @SuppressWarnings("cast")
  @Test
  public void testCasts() {
    Object o = FALSE ? (Object) new PolyA() : (Object) new PolyB();
    assertThat(o instanceof I).isTrue();
    assertThat(o instanceof IFoo).isFalse();
    assertThat(o instanceof IBar).isTrue();
    assertThat(o instanceof PolyA).isFalse();
    boolean b = o instanceof PolyB;
    assertThat(b).isTrue();
    assertThrows(
        ClassCastException.class,
        () -> {
          Object unused = (PolyA) o;
        });
  }

  @SuppressWarnings("StaticQualifiedUsingExpression")
  @Test
  public void testClinit() {
    ++HasClinit.i;
    HasClinit x = new HasClinit();
    ++x.i;
    new HasClinit().i++;
    HasClinit.i /= HasClinit.i;
    HasClinit.sfoo();
    HasClinit.i /= HasClinit.sfoo();
  }

  @Test
  public void testExceptions() {
    int i;
    for (i = 0; i < 5; ++i) {
      boolean hitOuter = false;
      boolean hitInner = false;
      try {
        try {
          switch (i) {
            case 0:
              throw new RuntimeException();
            case 1:
              throw new IndexOutOfBoundsException();
            case 2:
              throw new Exception();
            case 3:
              throw new StringIndexOutOfBoundsException();
            case 4:
              Object a = null;
              a.getClass(); // will throw JavaScript TypeError
          }
        } catch (StringIndexOutOfBoundsException e) {
          assertThat(i).isEqualTo(3);
        } finally {
          hitInner = true;
        }
      } catch (IndexOutOfBoundsException f) {
        assertThat(i).isEqualTo(1);
      } catch (NullPointerException npe) {
        assertThat(i).isEqualTo(4);
      } catch (RuntimeException g) {
        assertThat(i).isEqualTo(0);
      } catch (Throwable e) {
        assertThat(i).isEqualTo(2);
      } finally {
        assertThat(hitInner).isTrue();
        hitOuter = true;
      }
      assertThat(hitOuter).isTrue();
    }
    assertThat(i).isEqualTo(5);
  }

  @Override
  public String toString() {
    return "com.google.j2cl.ported.java6.MiscellaneousTest";
  }

  private boolean expectClassMetadata() {
    String name = Object.class.getName();

    if (name.equals("java.lang.Object")) {
      return true;
    } else if (name.startsWith("Class$")) {
      return false;
    }

    throw new RuntimeException("Unexpected class name " + name);
  }
}
