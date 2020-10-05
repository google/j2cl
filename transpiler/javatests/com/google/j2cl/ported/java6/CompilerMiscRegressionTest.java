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
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.truth.MathUtil;
import com.google.j2cl.ported.java6.package1.Caller;
import com.google.j2cl.ported.java6.package1.ClassExposingM;
import com.google.j2cl.ported.java6.package1.SomeParent;
import com.google.j2cl.ported.java6.package1.SomeParentParent;
import com.google.j2cl.ported.java6.package1.SomeParentParentParent;
import com.google.j2cl.ported.java6.package1.SubClassExposingM;
import com.google.j2cl.ported.java6.package2.SomeSubClassInAnotherPackage;
import com.google.j2cl.ported.java6.package2.SomeSubSubClassInAnotherPackage;
import com.google.j2cl.ported.java6.package3.SomeInterface;
import com.google.j2cl.ported.java6.package3.SomePackageConfusedParent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests Miscelaneous fixes. */
@RunWith(JUnit4.class)
@SuppressWarnings({"BoxedPrimitiveConstructor", "MethodCanBeStatic"})
public class CompilerMiscRegressionTest {
  @Test
  public void testOverridingReturnType() {
    Map<String, String[]> map = new HashMap<>();
    map.put("one", new String[10]);

    map.get("one")[0] = "one";
    assertThat(map.get("one")[0]).isEqualTo("one");
  }

  private static float[] copy(float[] src, float[] dest) {
    System.arraycopy(src, 0, dest, 0, Math.min(src.length, dest.length));
    return dest;
  }

  private void throwE(String message) {
    throw new RuntimeException(message);
  }

  /** Test for issue 8243. */
  @Test
  public void testAddAllLargeNumberOfElements() {

    int dstLength = 10;
    // Some browser have a limit on the number of parameters a function can have and 130000 barely
    // exceeds Chrome limit (as of V34).
    // This limit also applies when functions are called through apply().
    int srcLength = 130000;
    List<String> original = new ArrayList<String>();
    for (int i = 0; i < dstLength; i++) {
      original.add("foo");
    }
    List<String> src = new ArrayList<String>();
    for (int i = 0; i < srcLength; i++) {
      src.add("bar");
    }

    original.addAll(src);
    final int totalLength = srcLength + dstLength;
    assertThat(original.size()).isEqualTo(totalLength);

    // Check the result sampling as iterating through large arrays seems costly in IE.
    for (int i = 0; i < totalLength; i += 1000) {
      if (i < dstLength) {
        assertThat(original.get(i)).isEqualTo("foo");
      } else {
        assertThat(original.get(i)).isEqualTo("bar");
      }
    }
  }

  /** Test for issue 7253. */
  @Test
  public void testNestedTryFollowedByTry() {
    try {
      throwE("1");
      fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      assertThat(e.getMessage()).isEqualTo("1");
      try {
        throwE("2");
        fail("Should have thrown RuntimeException");
      } catch (RuntimeException e2) {
        assertThat(e2.getMessage()).isEqualTo("2");
      }
    }
    try {
      throwE("3");
      fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      assertThat(e.getMessage()).isEqualTo("3");
    }
  }

  /** Test for issue 6638. */
  @Test
  public void testNewArrayInlining() {
    float[] src = new float[] {1, 1, 1};
    float[] dest = copy(src, new float[3]);

    assertEqualContents(src, dest);
  }

  /**
   * Tests complex overriding patterns involving package private methods.
   *
   * <p>Test for issue 8654.
   */
  @Test
  public void testOverride() {
    Caller aCaller = new Caller();
    assertThat(aCaller.callPackagePrivatem(new SomeParentParent())).isEqualTo("SomeParentParent");
    assertThat(aCaller.callPackagePrivatem(new SomeParent())).isEqualTo("SomeParent");
    assertThat(aCaller.callPackagePrivatem(new SomeSubClassInAnotherPackage()))
        .isEqualTo("SomeParent");

    assertThat(SomeSubClassInAnotherPackage.pleaseCallm(new SomeSubClassInAnotherPackage()))
        .isEqualTo("SomeSubClassInAnotherPackage");
    assertThat(SomeSubClassInAnotherPackage.pleaseCallm(new SomeSubSubClassInAnotherPackage()))
        .isEqualTo("SomeSubSubClassInAnotherPackage");

    assertThat(aCaller.callPackagePrivatem(new ClassExposingM())).isEqualTo("ClassExposingM");

    SomeInterface i = new ClassExposingM();
    assertThat(i.m()).isEqualTo("ClassExposingM");
    assertThat(new ClassExposingM().f()).isEqualTo("live at ClassExposingM");

    // Confirm that both calling m through SomeInterface and through SomeParentParentParent
    // dispatch to the right implementation.
    SomeInterface i1 = new SubClassExposingM();
    assertThat(i1.m()).isEqualTo("SubClassExposingM");

    assertThat(SomeParentParentParent.callSomeParentParentParentM(new SubClassExposingM()))
        .isEqualTo("SubClassExposingM");

    assertThat(SomeParentParentParent.callSomeParentParentParentM(new SomeParentParentParent()))
        .isEqualTo("SomeParentParentParent");
    assertThat(SomeParentParentParent.callSomeParentParentParentM(new SomePackageConfusedParent()))
        .isEqualTo("SomeParentParentParent");
    assertThat(SomeParentParentParent.callSomeParentParentParentM(new SomeParentParent()))
        .isEqualTo("SomeParentParent");
    assertThat(SomeParentParentParent.callSomeParentParentParentM(new SomeParent()))
        .isEqualTo("SomeParent");
    assertThat(
            SomeParentParentParent.callSomeParentParentParentM(new SomeSubClassInAnotherPackage()))
        .isEqualTo("SomeParent");
    assertThat(
            SomeParentParentParent.callSomeParentParentParentM(
                new SomeSubSubClassInAnotherPackage()))
        .isEqualTo("SomeParent");
  }

  enum MyEnum {
    A,
    B,
    C;

    public static final MyEnum[] VALUES = values();

    public int getPriority() {
      return VALUES.length - ordinal();
    }
  }

  /**
   * Tests that enum ordinalizer does not incorrectly optimize {@code MyEnum}.
   *
   * <p>Test for issue 8846.
   */
  @Test
  public void testMyEnum() {
    assertThat(MyEnum.B.getPriority()).isEqualTo(2);
  }

  enum OrderingProblem {
    A,
    B;

    public static OrderingProblem getPriority1() {
      if (new Integer(1).toString().isEmpty()) {
        return B;
      }
      return A;
    }
  }

  /**
   * Test for regression introduced in patch https://gwt-review.googlesource.com/#/c/9083; where
   * depending on the order in which references to the enum class were encountered, some instances
   * were not correctly replaced .
   */
  @Test
  public void testOrderingProblem() {
    assertThat(OrderingProblem.getPriority1().ordinal()).isEqualTo(OrderingProblem.A.ordinal());
  }

  private static final double MINUTES_IN_DAY = 24 * 60;

  public void assertStaticEvaluationRegression(int hour, int minute) {
    // Do not inline this method so that the problematic expression reaches JsStaticEval.
    double expected = hour * 60 + minute;
    expected /= MINUTES_IN_DAY;
    expected *= 100;
    assertThat((hour * 60 + minute) / MINUTES_IN_DAY * 100).isEqualTo(expected);
  }

  /** Test for issue 8934. */
  @Test
  public void testStaticEvaluationRegression() {
    // Perform two calls with different constant values to make sure the assertStaticEvaluation does
    // not get the constant parameters propagated and statically evaluated in the Java AST.
    assertStaticEvaluationRegression(10, 20);
    assertStaticEvaluationRegression(20, 10);
  }

  /**
   * Test for issue 8909.
   *
   * <p>DevMode does not conform to JS arithmetic semantics and this method tests exactly that.
   */
  @Test
  public void testStaticEvaluationSematics() {
    float num = getRoundedValue(1.005f);
    assertThat(MathUtil.equalWithinTolerance(1.00, (double) num, 0.001)).isTrue();
  }

  private float getRoundedValue(float parameter) {
    float local = parameter;
    local = local * 100f;
    return Math.round(local) / 100f;
  }

  /**
   * Test for issue 9153.
   *
   * <p>Typetightener used to incorrectly tighten method calls marked with STATIC_DISPATCH_ONLY.
   */
  @Test
  public void testIncorrectDispatch() {
    state = new int[1];
    new B().m();
    assertThat(state[0]).isEqualTo(1);
  }

  static int[] state;

  @JsType
  abstract static class A {
    public void m() {
      state[0] = 1;
    }
  }

  @JsType
  static class B extends A {
    @Override
    public void m() {
      super.m();
    }
  }

  private static void assertEqualContents(float[] expected, float[] actual) {

    assertWithMessage("Array length mismatch").that(actual.length).isEqualTo(expected.length);
    for (int i = 0; i < expected.length; i++) {
      assertWithMessage("Array mismatch at element " + i).that(actual[i]).isEqualTo(expected[i]);
    }
  }

  @JsMethod
  private static List<String> singletonFrom(int i, String... arguments) {
    // Make the second parameter varargs and pass it as a whole to trigger the arguments copying
    // preamble.
    return Arrays.asList(arguments).subList(i, i + 1);
  }

  @JsMethod
  private static List<String> argumentsParameterClasher(int arguments, String... others) {
    // Make the second parameter varargs and pass it as a whole to trigger the arguments copying
    // preamble.
    return Arrays.asList(others).subList(0, arguments);
  }

  @JsMethod
  @SuppressWarnings("unused")
  private static List<String> argumentsVariableClasher(int i, String... others) {
    // Make the second parameter varargs and pass it as a whole to trigger the arguments copying
    // preamble.
    {
      int arguments = 3;
    }
    return Arrays.asList(others).subList(0, i);
  }

  @Test
  public void testVarargsNamedArguments() {
    assertThat(singletonFrom(1, "Hello", "GoodBye").get(0)).isEqualTo("GoodBye");
    assertThat(argumentsParameterClasher(1, "Hello", "GoodBye").get(0)).isEqualTo("Hello");
    assertThat(argumentsVariableClasher(1, "Hello", "GoodBye").get(0)).isEqualTo("Hello");
  }

  interface ParameterizedFunctionalInterface<T, S> {}

  static class ClassUsingRawInterface {
    public ClassUsingRawInterface(Supplier<ParameterizedFunctionalInterface> supplier) {}
  }

  /**
   * Reproduces https://github.com/google/j2cl/issues/52.
   *
   * <p>The following code would trigger a IllegalArgumentException due to nested
   * JsDocCastExpressions being introduced by InsertErasureTypeSafetyCasts.
   */
  @Test
  public <R, S> void testCompilesWithoutNPE() {
    new ClassUsingRawInterface(() -> new ParameterizedFunctionalInterface<R, S>() {});
  }

  @Test
  public <T> void testCompilesWithoutNPE_b147690014() {
    acceptsSupplier(() -> new Object() {});
  }

  private static <T> void acceptsSupplier(Supplier<T> supplier) {}

  @Test
  public void testOverflow() {
    int i = 1 << 30;
    double u = 1.0 / (1 << 30) * ((i << 1) + 1 - (1 << 30));
    assertEquals(1.0000000009313226d, u, 0);
  }
}
