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
package finallyblock;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import java.util.ArrayList;
import java.util.List;

/** Tests finally blocks. */
public class Main {
  public static void main(String... args) {
    testFinally_basic();
    testFinally_basic_inConstructor();
    testFinally_basic_inFieldInitializer();
    testFinally_evaluationOrder_returnExpression();
    testFinally_fallThrough();
    testFinally_returnCancelledByOuterBreak();
    testFinally_returnCancelledByOuterContinue();
    testFinally_returnSupersededByOuterReturn();
    testFinally_returnCancelledByBreak_inLambda();
    testFinally_exceptionSupersededByOuterException();
    testFinally_exceptionCancelledByOuterBreak();
    testFinally_exceptionCancelledByOuterBreak_inConstructor();
    testFinally_exceptionCancelledByOuterBreak_inInstanceInitializerBlock();
    testFinally_exceptionCancelledByOuterBreak_inStaticInitializerBlock();
  }

  private static void testFinally_basic() {
    assertEquals(10, basicFinallyMethod());
    assertEquals(42, value);
  }

  private static int value = 0;

  private static int basicFinallyMethod() {
    try {
      return 10;
    } finally {
      value = 42;
    }
  }

  private static void testFinally_basic_inConstructor() {
    value = 0;
    class Local {
      Local() {
        try {
          return;
        } finally {
          value = 54;
        }
      }
    }
    assertTrue(new Local() instanceof Local);
    assertEquals(54, value);
  }

  private static void testFinally_basic_inFieldInitializer() {
    List<String> steps = fieldWithTryFinally.get();
    assertEquals(new Object[] {"inTry", "inFinally"}, steps.toArray());
  }

  private static Supplier<List<String>> fieldWithTryFinally =
      () -> {
        List<String> steps = new ArrayList<>();
        try {
          steps.add("inTry");
          if (true) { // Fool javac to not complain about unreachable code.
            return steps;
          }
        } finally {
          steps.add("inFinally");
        }
        steps.add("afterTry");
        return null;
      };

  private static void testFinally_evaluationOrder_returnExpression() {
    List<String> steps = new ArrayList<>();
    testFinally_evaluationOrder_returnExpression(steps);
    assertEquals(
        new Object[] {"innerTry", "evaluatedReturnExpression", "innerFinally"}, steps.toArray());
  }

  private static boolean testFinally_evaluationOrder_returnExpression(List<String> steps) {
    try {
      steps.add("innerTry");
      if (true) { // Fool javac to not complain about unreachable code.
        return steps.add("evaluatedReturnExpression");
      }
    } finally {
      steps.add("innerFinally");
    }
    steps.add("atEnd");
    return false;
  }

  private static void testFinally_fallThrough() {
    List<String> steps = new ArrayList<>();
    testFinally_fallThrough(steps);
    assertEquals(
        new Object[] {"innerTry", "innerFinally", "endOuterTry", "outerFinally", "end"},
        steps.toArray());
  }

  private static void testFinally_fallThrough(List<String> steps) {
    try {
      try {
        steps.add("innerTry");
      } finally {
        steps.add("innerFinally");
      }
      steps.add("endOuterTry");
    } finally {
      steps.add("outerFinally");
    }
    steps.add("end");
  }

  private static void testFinally_returnCancelledByOuterBreak() {
    List<String> steps = new ArrayList<>();
    assertEquals("end", testFinally_returnCancelledByOuterBreak(steps));
    assertEquals(
        new Object[] {"innerTry", "innerFinally", "endOuterTry", "outerFinally", "end"},
        steps.toArray());
  }

  private static String testFinally_returnCancelledByOuterBreak(List<String> steps) {
    try {
      OUT:
      try {
        steps.add("innerTry");
        return "innerTry";
      } finally {
        steps.add("innerFinally");
        break OUT;
      }
      steps.add("endOuterTry");
    } finally {
      steps.add("outerFinally");
    }
    steps.add("end");
    return "end";
  }

  private static void testFinally_returnSupersededByOuterReturn() {
    List<String> steps = new ArrayList<>();
    assertEquals("outerFinally", testFinally_returnSupersededByOuterReturn(steps));
    assertEquals(new Object[] {"innerTry", "innerFinally", "outerFinally"}, steps.toArray());
  }

  private static String testFinally_returnSupersededByOuterReturn(List<String> steps) {
    try {
      OUT:
      try {
        steps.add("innerTry");
        if (true) { // Fool javac to not complain about unreachable code.
          return "innerTry";
        }
      } finally {
        steps.add("innerFinally");
      }
      steps.add("endOuterTry");
    } finally {
      steps.add("outerFinally");
      if (true) { // Fool javac to not complain about unreachable code.
        return "outerFinally";
      }
    }
    steps.add("end");
    return "end";
  }

  private static void testFinally_returnCancelledByOuterContinue() {
    List<String> steps = new ArrayList<>();
    assertEquals("topOfLoop", testFinally_returnCancelledByOuterContinue(steps));
    assertEquals(new Object[] {"innerTry", "innerFinally", "outerFinally"}, steps.toArray());
  }

  private static String testFinally_returnCancelledByOuterContinue(List<String> steps) {
    for (int i = 0; i < 2; i++) {
      if (i == 1) {
        return "topOfLoop";
      }
      try {
        OUT:
        try {
          steps.add("innerTry");
          if (true) { // Fool javac to not complain about unreachable code.
            return "innerTry";
          }
        } finally {
          steps.add("innerFinally");
        }
        steps.add("endOuterTry");
      } finally {
        steps.add("outerFinally");
        if (true) { // Fool javac to not complain about unreachable code.
          continue;
        }
      }
      steps.add("bottomOfLoop");
    }
    steps.add("end");
    return "end";
  }

  private static void testFinally_returnCancelledByBreak_inLambda() {
    List<String> steps = new ArrayList<>();
    Supplier<String> functionWithFinally =
        () -> {
          OUT:
          try {
            steps.add("inTry");
            if (true) { // Fool javac to not complain about unreachable code.
              return "inTry";
            }
          } finally {
            steps.add("inFinally");
            break OUT;
          }
          steps.add("afterTry");
          return "atEnd";
        };

    steps.clear();
    assertEquals("atEnd", functionWithFinally.get());
    assertEquals(new Object[] {"inTry", "inFinally", "afterTry"}, steps.toArray());
  }

  private static void testFinally_exceptionSupersededByOuterException() {
    List<String> steps = new ArrayList<>();
    try {
      testFinally_exceptionSupersededByOuterException(steps);
      fail();
    } catch (RuntimeException e) {
      assertEquals(new Object[] {"innerTry", "innerFinally", "outerFinally"}, steps.toArray());
      assertEquals("outerFinally", e.getMessage());
      // The original exceptions gets dropped and is not added to the suppressed ones.
      assertEquals(0, e.getSuppressed().length);
    }
  }

  private static String testFinally_exceptionSupersededByOuterException(List<String> steps) {
    OUT:
    try {
      try {
        steps.add("innerTry");
        if (true) { // Fool javac to not complain about unreachable code.
          throw new RuntimeException("innerTry");
        }
      } finally {
        steps.add("innerFinally");
      }
      steps.add("endOuterTry");
    } finally {
      steps.add("outerFinally");
      if (true) { // Fool javac to not complain about unreachable code.
        throw new RuntimeException("outerFinally");
      }
    }
    steps.add("end");
    return "end";
  }

  private static void testFinally_exceptionCancelledByOuterBreak() {
    List<String> steps = new ArrayList<>();
    assertEquals("end", testFinally_exceptionCancelledByOuterBreak(steps));
    assertEquals(new Object[] {"innerTry", "innerFinally", "outerFinally", "end"}, steps.toArray());
  }

  private static String testFinally_exceptionCancelledByOuterBreak(List<String> steps) {
    OUT:
    try {
      try {
        steps.add("innerTry");
        if (true) { // Fool javac to not complain about unreachable code.
          throw new RuntimeException("innerTry");
        }
      } finally {
        steps.add("innerFinally");
      }
      steps.add("endOuterTry");
    } finally {
      steps.add("outerFinally");
      if (true) { // Fool javac to not complain about unreachable code.
        break OUT;
      }
    }
    steps.add("end");
    return "end";
  }

  private static void testFinally_exceptionCancelledByOuterBreak_inConstructor() {
    List<String> steps = new ArrayList<>();
    class Local {
      Local() {
        OUT:
        try {
          try {
            steps.add("beforeThrow");
            if (true) { // Fool javac to not complain about unreachable code.
              throw new RuntimeException();
            }
          } finally {
            steps.add("innerFinally");
          }
          steps.add("normalFlowAfterTry");

        } finally {
          steps.add("outerFinally");
          break OUT;
        }
        steps.add("atNormalExit");
      }
    }
    assertTrue(new Local() instanceof Local);
    assertEquals(
        new Object[] {"beforeThrow", "innerFinally", "outerFinally", "atNormalExit"},
        steps.toArray());
  }

  private static void testFinally_exceptionCancelledByOuterBreak_inInstanceInitializerBlock() {
    List<String> steps = new ArrayList<>();

    class Local {
      {
        OUT:
        try {
          try {
            steps.add("beforeThrow");
            if (true) { // Fool javac to not complain about unreachable code.
              throw new RuntimeException();
            }
          } finally {
            steps.add("innerFinally");
          }
          steps.add("normalFlowAfterFinally");
        } finally {
          steps.add("outerFinally");
          break OUT;
        }
        steps.add("atNormalExit");
      }
    }
    Local l = new Local();
    assertEquals(
        new Object[] {"beforeThrow", "innerFinally", "outerFinally", "atNormalExit"},
        steps.toArray());
  }

  private static void testFinally_exceptionCancelledByOuterBreak_inStaticInitializerBlock() {
    assertEquals(
        new Object[] {"beforeThrow", "innerFinally", "outerFinally", "atNormalExit"},
        FinallyInStaticInitializer.steps.toArray());
  }

  private static class FinallyInStaticInitializer {
    static List<String> steps = new ArrayList<>();

    static {
      OUT:
      try {
        try {
          steps.add("beforeThrow");
          if (true) { // Fool javac to not complain about unreachable code.
            throw new RuntimeException();
          }
        } finally {
          steps.add("innerFinally");
        }
        steps.add("normalFlowAfterFinally");
      } finally {
        steps.add("outerFinally");
        break OUT;
      }
      steps.add("atNormalExit");
    }
  }

  // Declare a local Supplier to avoid j2kt errors.
  interface Supplier<T> {
    T get();
  }
}
