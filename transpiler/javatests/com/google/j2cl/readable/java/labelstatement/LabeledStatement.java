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
package labelstatement;

public class LabeledStatement {
  public void loopStatements() {
    LABEL:
    for (; ; ) {
      break LABEL;
    }

    WHILE:
    while (true) {
      SWITCH:
      switch (0) {
        case 0:
          continue WHILE;
      }
    }
  }

  public void simpleStatement() {
    LABEL:
    foo();

    // Test that none of these trip jscompiler (b/330169941).
    LABEL:
    break LABEL;
    LABEL:
    LABEL2:
    break LABEL;
    LABEL2:
    LABEL:
    break LABEL;

    do {
      LABEL:
      continue;
    } while (false);

    LABEL:
    return;
  }

  public void block() {
    LABEL:
    {
      break LABEL;
    }
  }

  public void ifStatement() {
    LABEL:
    if (true) {
      break LABEL;
    }
  }

  public void switchStatement() {
    LABEL:
    switch (0) {
      case 0:
        if (true) {
          break;
        }
    }
  }

  public void nestedScopes() {
    // Test a lexically nested labeled statement that are do not conflict since they are in
    // different scopes.
    LABEL:
    do {
      new Object() {
        void m() {
          LABEL:
          do {} while (false);
        }
      };
    } while (false);
  }

  private void foo() {}
}
