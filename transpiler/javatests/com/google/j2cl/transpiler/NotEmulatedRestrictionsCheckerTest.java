/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaults;
import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithWasmDefaults;

import junit.framework.TestCase;

/** Tests for NotEmulatedRestrictionsChecker. */
public class NotEmulatedRestrictionsCheckerTest extends TestCase {

  public void testNotEmulatedMethodCallsFail() {
    assertWithInlineMessages(
        "test.Main",
        """
        import javaemul.internal.annotations.NotEmulated;
        class Api {
          @NotEmulated
          static void notEmulatedMethod() {}

          static void emulatedMethod() {}

          @NotEmulated
          void notEmulatedInstanceMethod() {}

          void emulatedInstanceMethod() {}
        }
        class SubApi extends Api {}
        class Main {
          void main(Api api, SubApi subApi) {
            Api.notEmulatedMethod();
        > Error: 'void Api.notEmulatedMethod()' is not emulated.
            Runnable r = Api::notEmulatedMethod;
        > Error: 'void Api.notEmulatedMethod()' is not emulated.
            api.notEmulatedInstanceMethod();
        > Error: 'void Api.notEmulatedInstanceMethod()' is not emulated.
            subApi.notEmulatedInstanceMethod();
        > Error: 'void Api.notEmulatedInstanceMethod()' is not emulated.
            Api.emulatedMethod();
            api.emulatedInstanceMethod();
          }
        }
        """);
  }

  private void assertWithInlineMessages(String... compilationUnitsAndSources) {
    newTesterWithDefaults().assertWithInlineMessages(compilationUnitsAndSources);
    newTesterWithWasmDefaults().assertWithInlineMessages(compilationUnitsAndSources);
  }
}
