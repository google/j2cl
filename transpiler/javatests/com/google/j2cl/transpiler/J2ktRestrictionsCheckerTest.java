/*
 * Copyright 2022 Google Inc.
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


import com.google.j2cl.transpiler.TranspilerTester.TranspileResult;
import junit.framework.TestCase;

/** Tests for J2ktRestrictionsChecker. */
public class J2ktRestrictionsCheckerTest extends TestCase {
  public void testEmptyClass() {
    assertTranspileSucceeds("test.Empty", "class Empty {}");
  }

  public void testGenericConstructorFails() {
    assertTranspileFails("test.Main", "class Main {", "  <T> Main(T t) {}", "}")
        .assertErrorsWithSourcePosition(
            "Error:Main.java:3: Constructor 'Main(T t)' cannot declare type variables.");
  }

  private TranspileResult assertTranspileSucceeds(String compilationUnitName, String... code) {
    return newTranspilerTester(compilationUnitName, code).assertTranspileSucceeds();
  }

  private TranspileResult assertTranspileFails(String compilationUnitName, String... code) {
    return newTranspilerTester(compilationUnitName, code).assertTranspileFails();
  }

  private TranspilerTester newTranspilerTester(String compilationUnitName, String... code) {
    return TranspilerTester.newTesterWithDefaults()
        .addArgs("-backend", "KOTLIN")
        .addCompilationUnit(compilationUnitName, code);
  }
}
