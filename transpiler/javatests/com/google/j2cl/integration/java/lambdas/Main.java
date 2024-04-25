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
package lambdas;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.io.Serializable;

public class Main {
  public static void main(String... args) {
    Captures captures = new Captures();
    captures.testLambdaNoCapture();
    captures.testInstanceofLambda();
    captures.testLambdaCaptureField();
    captures.testLambdaCaptureLocal();
    captures.testLambdaCaptureFieldAndLocal();
    testSpecialLambdas();
    testSpecializedLambda();
    testVarargsLambdas();
    testVarKeywordInLambda();
    testSerializableLambda();
    testNestedLambdas();
    testArbitraryNesting();
    testIntersectionTypeLambdas();
    testInStaticContextWithInnerClasses();
  }

  private interface IntToIntFunction {
    int apply(int i);
  }

  private static class Captures {
    private int field = 100;

    private int test(IntToIntFunction f, int n) {
      return this.field + f.apply(n);
    }

    private void testLambdaNoCapture() {
      int result = test(i -> i + 1, 10);
      assertTrue(result == 111);
      result =
          test(
              i ->
                  new Object() {
                    int storedValue = i;

                    int addTwo() {
                      return this.storedValue + 2;
                    }
                  }.addTwo(),
              10);
      assertTrue(result == 112);
    }

    private void testInstanceofLambda() {
      IntToIntFunction f = i -> i + 1;
      assertTrue(f instanceof IntToIntFunction);
    }

    private void testLambdaCaptureField() {
      int result = test(i -> field + i + 1, 10);
      assertTrue(result == 211);

      class Local {
        int field = 10;

        class Inner {
          int getOuterField() {
            return Local.this.field;
          }
        }
      }

      assertEquals(10, new Local().new Inner().getOuterField());
    }

    private void testLambdaCaptureLocal() {
      int x = 1;
      int result = test(i -> x + i + 1, 10);
      assertTrue(result == 112);
    }

    private void testLambdaCaptureFieldAndLocal() {
      int x = 1;
      int result =
          test(
              i -> {
                int y = 1;
                return x + y + this.field + i + 1;
              },
              10);
      assertTrue(result == 213);
    }
  }

  interface Equals<T> {
    @Override
    boolean equals(Object object);

    default T get() {
      return null;
    }
  }

  interface SubEquals extends Equals<String> {
    @Override
    String get();
  }

  @SuppressWarnings({"SelfEquals", "EqualsIncompatibleType"})
  private static void testSpecialLambdas() {
    SubEquals getHello = () -> "Hello";

    assertTrue(getHello.equals(getHello));
    assertTrue(!getHello.equals("Hello"));
    assertTrue("Hello".equals(getHello.get()));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testSpecializedLambda() {
    Consumer<String> stringConsumer =
        s -> {
          Object unused = s.substring(1);
        };
    Consumer rawConsumer = stringConsumer;
    assertThrowsClassCastException(() -> rawConsumer.accept(new Object()), String.class);

    VarargsIntFunction<String> firstA = ns -> ns[0].indexOf("a");
    VarargsIntFunction rawVarargsFunction = firstA;

    assertThrowsClassCastException(
        () -> rawVarargsFunction.apply(new Object[] {"bbabb", "aabb"}), String[].class);
  }

  interface Consumer<T> {
    void accept(T t);
  }

  interface VarargsIntFunction<T> {
    int apply(T... t);
  }

  private static void testVarargsLambdas() {
    VarargsFunction<String> changeFirstElement =
        ss -> {
          ss[0] = ss[0] + " world";
          return ss;
        };

    String[] params = new String[] {"hello"};
    assertEquals(params, changeFirstElement.apply(params));
    assertEquals("hello world", params[0]);
  }

  interface VarargsFunction<T> {
    T[] apply(T... t);
  }

  private static void testVarKeywordInLambda() {
    IntToIntFunction f = (var i) -> i + 1;
    assertEquals(3, f.apply(2));
  }

  private static void testSerializableLambda() {
    Object lambda = (Consumer<Object> & Serializable) o -> {};
    assertTrue(lambda instanceof Serializable);
  }

  private static void testArbitraryNesting() {
    class A {
      public void a() {
        int[] x = new int[] {42};
        class B {
          public int b() {
            IntToIntFunction i =
                new IntToIntFunction() {

                  @Override
                  public int apply(int a) {
                    IntToIntFunction ii =
                        n -> {
                          return new IntToIntFunction() {
                            @Override
                            public int apply(int b) {
                              IntToIntFunction iii = m -> x[0] = x[0] + a + b + n + m;
                              return iii.apply(100);
                            }
                          }.apply(200);
                        };
                    return ii.apply(300);
                  }
                };
            return i.apply(400);
          }
        }
        int result = new B().b();
        assertTrue(result == 1042);
        assertTrue(x[0] == 1042);
      }
    }
    new A().a();
  }

  private static void testNestedLambdas() {
    int a = 10;
    IntToIntFunction i =
        m -> {
          int b = 20;
          IntToIntFunction ii = n -> a + b + m + n;
          return ii.apply(100);
        };
    assertTrue((i.apply(200) == 330));
  }

  private interface IdentityWithDefault<T> {
    T identityaccept(T t);

    default IdentityWithDefault<T> self() {
      return this;
    }
  }

  private interface InterfaceWithDefaultMethod {
    static final String MY_TEXT = "from Non Functional";

    default String defaultMethod() {
      return MY_TEXT;
    }
  }

  private static void testIntersectionTypeLambdas() {
    Object obj = (IdentityWithDefault<String> & InterfaceWithDefaultMethod) o -> o;
    assertTrue(obj instanceof IdentityWithDefault);
    assertTrue(obj instanceof InterfaceWithDefaultMethod);
    assertEquals(obj, ((IdentityWithDefault<String>) obj).self());
    assertEquals(
        InterfaceWithDefaultMethod.MY_TEXT, ((InterfaceWithDefaultMethod) obj).defaultMethod());
  }

  private static void testInStaticContextWithInnerClasses() {
    IntToIntFunction addTwo =
        i ->
            new Object() {
              int storedValue = i;

              int addTwo() {
                return this.storedValue + 2;
              }
            }.addTwo();
    assertEquals(5, addTwo.apply(3));
  }
}
