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
@SuppressWarnings("CheckReturnValue")
public class J2ktRestrictionsCheckerTest extends TestCase {
  public void testEmptyClass() {
    assertTranspileSucceeds("test.Empty", "class Empty {}");
  }

  public void testGenericConstructorFails() {
    assertTranspileFails("test.Main", "class Main {", "  <T> Main(T t) {}", "}")
        .assertErrorsWithSourcePosition(
            "Error:Main.java:3: Constructor 'Main(T t)' cannot declare type variables.");
  }

  public void testMemberVisibilityWarnings() {
    assertTranspileSucceeds(
            "test.Public",
            "class Pkg {}",
            "public class Public {",
            "  public void pkgParam(Pkg pkg) {}",
            "  public Pkg pkgReturnType() { return null; }",
            "  public Pkg pkgField;",
            "  static class InnerPkg {",
            "    public InnerPkg() {}",
            "    public Pkg pkgReturnType() { return null; }",
            "    public Pkg pkgField;",
            "  }",
            "}")
        .assertWarningsWithSourcePosition(
            "Warning:Public.java:4: Member 'void Public.pkgParam(Pkg pkg)' (public) should not have"
                + " wider visibility than 'Pkg' (default).",
            "Warning:Public.java:5: Member 'Pkg Public.pkgReturnType()' (public) should not have"
                + " wider visibility than 'Pkg' (default).",
            "Warning:Public.java:6: Member 'Public.pkgField' (public) should not have wider"
                + " visibility than 'Pkg' (default).");
  }

  public void testClassVisibilityWarnings() {
    assertTranspileSucceeds("test.Main", "class Pkg {}", "public class Main extends Pkg {}")
        .assertWarningsWithSourcePosition(
            "Warning:Main.java:3: Type 'Main' (public) should not have wider visibility than its"
                + " super type 'Pkg' (default).");
  }

  public void testInterfaceVisibilityWarnings() {
    assertTranspileSucceeds("test.Main", "interface Pkg {}", "public interface Main extends Pkg {}")
        .assertWarningsWithSourcePosition(
            "Warning:Main.java:3: Type 'Main' (public) should not have wider visibility than its"
                + " super type 'Pkg' (default).");
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
