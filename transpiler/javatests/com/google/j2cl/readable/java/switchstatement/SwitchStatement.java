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
package switchstatement;

enum Numbers {
  ONE,
  TWO
}

public class SwitchStatement {
  public void main() {
    // String switch.
    switch ("one") {
      case "one":
      case "two":
        break;
      default:
        return;
    }

    // Char switch.
    switch ('1') {
      case '1':
      case '2':
        break;
      default:
        return;
    }

    // Int switch.
    switch (1) {
      case -2:
      case 1:
      case 2:
        break;
      default:
        return;
    }

    // Enum switch.
    switch (Numbers.ONE) {
      case ONE:
      case TWO:
        break;
      default:
        return;
    }
  }

  @SuppressWarnings("unused")
  private static void testSwitchVariableDeclarations() {
    switch (3) {
      case 1:
        int unassigned, unassigned2;
        int i = 0;
        int j = 2, b = j + 1;
        break;
      case 3:
        i = 3;
        assert i == 3;
        return;
    }

    switch (5) {
      case 5:
        int i = 1;
        break;
    }
    assert false;
  }

  private void testCaseExpressionTypes(char ch, int i, byte b, short s) {
    switch (ch) {
      case 'a':
        break;
      case 1:
        break;
      case (byte) 2:
        break;
      case (short) 3:
        break;
    }

    switch (i) {
      case 'a':
        break;
      case 1:
        break;
      case (byte) 2:
        break;
      case (short) 3:
        break;
    }

    switch (b) {
      case 'a':
        break;
      case 1:
        break;
      case (byte) 2:
        break;
      case (short) 3:
        break;
    }

    switch (s) {
      case 'a':
        break;
      case 1:
        break;
      case (byte) 2:
        break;
      case (short) 3:
        break;
    }
  }
  
  private void testBlocksInSwitchCase(int i) {
    switch (i) {
      case 1:
        foo(1);
        {
          foo(2);
        }
        foo(3);
        {
          foo(4);
          foo(5);
        }
        break;
    }
  }

  private void testLabelInSwitchCase(int i) {
    switch (i) {
      case 1:
        LABEL:
        do {
          break LABEL;
        } while (true);
    }
  }

  private void testNonFallThroughBreakCase(int i) {
    switch (i) {
      case 1:
        foo(1);
        break;
      default:
        foo(2);
        break;
    }
  }

  private void testNonFallThroughBreakOuterCase(int i) {
    OUTER_LABEL:
    do {
      switch (i) {
        case 1:
          foo(1);
          break OUTER_LABEL;
        default:
          foo(2);
          break;
      }
    } while (false);
  }

  private void testNonFallThroughContinueCase(int i) {
    do {
      switch (i) {
        case 3:
          foo(1);
          continue;
        default:
          foo(2);
          break;
      }
    } while (false);
  }

  private void testNonFallThroughReturnCase(int i) {
    switch (i) {
      case 4:
        foo(1);
        return;
      default:
        foo(2);
        break;
    }
  }

  private void testNonFallThroughThrowCase(int i) {
    switch (i) {
      case 5:
        foo(1);
        throw new RuntimeException();
      default:
        foo(2);
        break;
    }
  }

  private void testNonFallThroughIfCase(int i) {
    switch (i) {
      case 1:
        foo(1);
        if (false) {
          break;
        } else {
          break;
        }
      default:
        foo(2);
        break;
    }
  }

  private void testNonFallThroughBlockCase(int i) {
    switch (i) {
      case 1:
        {
          foo(1);
          break;
        }
      default:
        foo(2);
        break;
    }
  }

  private void testFallThroughCase(int i) {
    switch (i) {
      case 1:
        foo(1);
        // fall through
      default:
        foo(2);
        break;
    }
  }

  private void testFallThroughBreakInnerCase(int i) {
    switch (i) {
      case 1:
        foo(1);
        INNER_LABEL:
        do {
          break INNER_LABEL;
        } while (false);
        // fall through
      default:
        foo(2);
        break;
    }
  }

  private void testFallThroughContinueInnerCase(int i) {
    switch (i) {
      case 1:
        foo(1);
        INNER_LABEL:
        do {
          continue INNER_LABEL;
        } while (false);
        // fall through
      default:
        foo(2);
        break;
    }
  }

  private void testFallThroughLabeledStatement(int i) {
    switch (i) {
      case 1:
        INNER_LABEL:
        {
          if (false) {
            break INNER_LABEL;
          }
          return;
        }
        // fall through
      default:
        foo(2);
        break;
    }
  }

  private void testFallThroughIfCase(int i) {
    switch (i) {
      case 1:
        foo(1);
        if (false) {
          break;
        }
        // fall through
      default:
        foo(2);
        break;
    }
  }

  private void testFallThroughIfElseCase(int i) {
    switch (i) {
      case 1:
        if (true) {
          foo(1);
          break;
        } else {
          foo(2);
        }
        // fall through
      default:
        foo(2);
        break;
    }
  }

  private void testFallThoughLastCase(int i) {
    switch (i) {
      case 1:
        foo(1);
        break;
      default:
        foo(2);
        // fall through
    }
  }

  private void foo(int i) {}
}
