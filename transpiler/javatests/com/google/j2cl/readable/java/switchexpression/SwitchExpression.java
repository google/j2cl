/*
 * Copyright 2024 Google Inc.
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
package switchexpression;

import javaemul.internal.annotations.Wasm;

public class SwitchExpression {
  @SuppressWarnings("unused")
  private static void testBasicSwitchExpressions() {
    int a = 0;
    // This it a switch expression featuring all the combinations in the spec per Java 14.
    // Note that the different branches of the switch yield different types that are also different
    // from the type that is assigned.
    long i =
        switch (3) {
          // Simple case with implicit yield.
          case 1 -> 5;
          // Case with 2 symbols that just throws.
          case 3, 4 -> throw new RuntimeException();
          // Case with a block and and explicit yield.
          default -> {
            Short j = (short) a++;
            yield j;
          }
        };
  }

  private static void testSwitchExpressionsWithComplexControlFlow() {
    int a = 0;
    long i =
        switch (3) {
          case 1 -> 5;
          case 3, 4 -> throw new RuntimeException();
          default -> {
            Short j = (short) a++;
            while (j < 3) {
              if (j == 2) {
                yield 2;
              }
            }
            yield j;
          }
        };
  }

  private static void testNestedSwitchExpressions() {
    int a = 0;
    long i =
        switch (3) {
          // Simple case with implicit yield.
          case 1 ->
              switch (5) {
                case 1 -> 10;
                default ->
                    switch (6) {
                      case 1 -> throw new RuntimeException();
                      default -> 5;
                    };
              };
          default -> a;
        };
  }

  enum Enum {
    A,
    B;
  }

  // TODO(b/395108282): Remove nop once the bug is fixed.
  @Wasm("nop")
  private static void testExhaustiveSwitchExpression() {
    long i =
        switch (Enum.A) {
          case A -> 0;
          case B -> 1;
        };
  }
}
