/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler;

import static com.google.j2cl.transpiler.TranspilerTester.newTesterWithDefaultsWasm;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.transpiler.TranspilerTester.TranspileResult;
import java.util.List;
import junit.framework.TestCase;

/** Tests for J2wasm transpilation. */
public final class J2wasmTranspilerTest extends TestCase {
  public void testEmptyClass() {
    assertTranspileSucceeds("test.empty", "class Empty {}");
  }

  public void testEntryPointFoundPasses() {
    assertTranspileSucceedsWithEntryPoints(
        "wasm.entrypoint.Main",
        ImmutableList.of("wasm.entrypoint.Main#main"),
        "class Main {",
        "  public static void main() {}",
        "}");
  }

  public void testEntryPointRegexFoundPasses() {
    assertTranspileSucceedsWithEntryPoints(
        "wasm.entrypoint.Main",
        ImmutableList.of("wasm.entrypoint.Main#m.*"),
        "class Main {",
        "  public static void main() {}",
        "}");
  }

  public void testMultipleEntryPointRegexFoundPasses() {
    assertTranspileSucceedsWithEntryPoints(
        "wasm.entrypoint.Main",
        ImmutableList.of("wasm.entrypoint.Main#m.*"),
        "class Main {",
        "  public static void main() {}",
        "  public static void main2() {}",
        "}");
  }

  public void testEntryPointNotFoundFails() {
    assertTranspileFailsWithEntryPoints(
            "wasm.entrypoint.Main",
            ImmutableList.of("wasm.entrypoint.Main#notFound"),
            "class Main {",
            "  public static void main() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "No public static method matched the entry point string"
                + " 'wasm.entrypoint.Main#notFound'.");
  }

  public void testEntryPointRegexNotFoundFails() {
    assertTranspileFailsWithEntryPoints(
            "wasm.entrypoint.Main",
            ImmutableList.of("wasm.entrypoint.Main#not.*", "wasm.entrypoint.Main#alsoNot"),
            "class Main {",
            "  public static void main() {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "No public static method matched the entry point string 'wasm.entrypoint.Main#not.*'.",
            "No public static method matched the entry point string"
                + " 'wasm.entrypoint.Main#alsoNot'.");
  }

  public void testMultipleEntryPointSameNameFails() {
    assertTranspileFailsWithEntryPoints(
            "wasm.entrypoint.Main",
            ImmutableList.of("wasm.entrypoint.Main#main"),
            "class Main {",
            "  public static void main() {}",
            "  public static void main(int arg) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "More than one method are exported with the same name 'main'.");
  }

  public void testMultipleEntryPointRegexSameNameFails() {
    assertTranspileFailsWithEntryPoints(
            "wasm.entrypoint.Main",
            ImmutableList.of("wasm.entrypoint.Main#m.*"),
            "class Main {",
            "  public static void main() {}",
            "  public static void main(int arg) {}",
            "}")
        .assertErrorsWithoutSourcePosition(
            "More than one method are exported with the same name 'main'.");
  }

  public void testBadEntryPointSyntax() {
    assertTranspileFailsWithEntryPoints(
            "wasm.entrypoint.Main",
            ImmutableList.of("wasm\\.entrypoint.Main#m.*"),
            "class Main {",
            "}")
        .assertErrorsWithoutSourcePosition(
            "Invalid entry point syntax in 'wasm\\.entrypoint.Main#m.*'.");
  }

  @CanIgnoreReturnValue
  private TranspileResult assertTranspileSucceeds(String compilationUnitName, String... code) {
    return newTesterWithDefaultsWasm()
        .addCompilationUnit(compilationUnitName, code)
        .assertTranspileSucceeds();
  }

  @CanIgnoreReturnValue
  private TranspileResult assertTranspileSucceedsWithEntryPoints(
      String compilationUnitName, List<String> entryPoints, String... code) {
    TranspilerTester tester =
        newTesterWithDefaultsWasm().addCompilationUnit(compilationUnitName, code);
    for (String entryPoint : entryPoints) {
      tester.addArgs("-generateWasmExport", entryPoint);
    }
    return tester.assertTranspileSucceeds();
  }

  private TranspileResult assertTranspileFailsWithEntryPoints(
      String compilationUnitName, List<String> entryPoints, String... code) {
    TranspilerTester tester =
        newTesterWithDefaultsWasm().addCompilationUnit(compilationUnitName, code);
    for (String entryPoint : entryPoints) {
      tester.addArgs("-generateWasmExport", entryPoint);
    }
    return tester.assertTranspileFails();
  }
}
