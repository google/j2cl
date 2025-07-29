/*
 * Copyright 2022 Google Inc.
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
package labelstatement

/**
 * Readable test for labeled statements.
 *
 * Note: Kotlin does not support referencing labeled statements outside of loops.
 */
class LabeledStatement {
  fun loopStatements() {
    LABEL@ while (true) {
      break@LABEL
    }

    LABEL@ do {
      break@LABEL
    } while (true)

    LABEL@ for (i in 1..100) {
      break@LABEL
    }

    WHILE@ while (true) {
      SWITCH@ when (0) {
        0 -> continue@WHILE
      }
    }
  }

  fun simpleStatement() {
    // Test a labeled statement for consistency with Java. The label cannot be referenced in Kotlin.
    LABEL@ foo()

    do {
      LABEL@ continue
    } while (false)

    LABEL@ return
  }

  fun nestedScopes() {
    // Test a lexically nested labeled statement that are do not conflict since they are in
    // different scopes.
    LABEL@ do {
      object {
        fun m() {
          LABEL@ do {} while (false)
        }
      }
    } while (false)

    // And kotlin also allows nested labels to shadow outer labels.
    LABEL@ do {
      LABEL@ do {} while (false)
    } while (false)
  }

  private fun foo() {}
}
