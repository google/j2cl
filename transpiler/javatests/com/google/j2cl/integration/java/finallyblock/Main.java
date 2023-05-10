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
  public static void main(String[] args) {
    testFinally_basic();
    testFinally_fallThrough();
    testFinally_returnCancelledByOuterBreak();
    testFinally_innerReturnSuperSeededByOuterReturn();
    testFinally_innerReturnSuperByOuterContinue();
    testFinally_exceptionCancelledByOuterBreak();
    testFinally_exceptionSuperseededByOuterException();
  }

  private static void testFinally_basic() {
    assertTrue(basicFinallyMethod() == Main.value);
  }

  private static int value = 0;

  private static int basicFinallyMethod() {
    try {
      return 10;
    } finally {
      value = 10;
    }
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

  private static void testFinally_innerReturnSuperSeededByOuterReturn() {
    List<String> steps = new ArrayList<>();
    assertEquals("outerFinally", testFinally_innerReturnSuperSeededByOuterReturn(steps));
    assertEquals(new Object[] {"innerTry", "innerFinally", "outerFinally"}, steps.toArray());
  }

  private static String testFinally_innerReturnSuperSeededByOuterReturn(List<String> steps) {
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

  private static void testFinally_innerReturnSuperByOuterContinue() {
    List<String> steps = new ArrayList<>();
    assertEquals("topOfLoop", testFinally_innerReturnSuperByOuterContinue(steps));
    assertEquals(new Object[] {"innerTry", "innerFinally", "outerFinally"}, steps.toArray());
  }

  private static String testFinally_innerReturnSuperByOuterContinue(List<String> steps) {
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

  private static void testFinally_exceptionSuperseededByOuterException() {
    List<String> steps = new ArrayList<>();
    try {
      testFinally_exceptionSuperseededByOuterException(steps);
      fail();
    } catch (RuntimeException e) {
      assertEquals(new Object[] {"innerTry", "innerFinally", "outerFinally"}, steps.toArray());
      assertEquals("outerFinally", e.getMessage());
      // The original exceptions gets dropped and is not added to the suppressed ones.
      assertEquals(0, e.getSuppressed().length);
    }
  }

  private static String testFinally_exceptionSuperseededByOuterException(List<String> steps) {
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
}
 
