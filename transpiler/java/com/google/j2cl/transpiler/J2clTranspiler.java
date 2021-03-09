/*
 * Copyright 2015 Google Inc.
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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.backend.Backend;
import com.google.j2cl.transpiler.passes.JsInteropRestrictionsChecker;
import com.google.j2cl.transpiler.passes.NormalizationPass;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/** Translation tool for generating JavaScript source files from Java sources. */
class J2clTranspiler {

  /** Runs the entire J2CL pipeline. */
  static void transpile(J2clTranspilerOptions options, Problems problems) {
    // Compiler has no static state, but rather uses thread local variables.
    // Because of this, we invoke the compiler on a different thread each time.
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<?> result =
        executorService.submit(() -> new J2clTranspiler(options, problems).transpileImpl());
    // Shutdown the executor service since it will only run a single transpilation. If not shutdown
    // it prevents the JVM from ending the process (see Executors.newFixedThreadPool()). This is not
    // normally observed since the transpiler in normal circumstances ends with System.exit() which
    // ends all threads. But when the transpilation throws an exception, the exception propagates
    // out of main() and the process lingers due the live threads from these executors.
    executorService.shutdown();

    try {
      Uninterruptibles.getUninterruptibly(result);
    } catch (ExecutionException e) {
      // Try unwrapping the cause...
      Throwables.throwIfUnchecked(e.getCause());
      throw new AssertionError(e.getCause());
    }
  }

  private final J2clTranspilerOptions options;
  private final Problems problems;

  private J2clTranspiler(J2clTranspilerOptions options, Problems problems) {
    this.options = options;
    this.problems = problems;
  }

  private void transpileImpl() {
    if (options.getBackend() == Backend.WASM) {
      // TODO(rluble): cleanup the static state.
      MemberDescriptor.setWasmManglingPatterns();
      TypeDeclaration.setIgnoreJsEnumAnnotations();
    }
    List<CompilationUnit> j2clUnits =
        options
            .getFrontend()
            .getCompilationUnits(
                options.getClasspaths(),
                options.getSources(),
                options.getGenerateKytheIndexingMetadata(),
                problems);
    if (!j2clUnits.isEmpty()) {
      desugarUnits(j2clUnits);
      checkUnits(j2clUnits);
      normalizeUnits(j2clUnits);
    }
    options
        .getBackend()
        .generateOutputs(
            j2clUnits,
            options.getNativeSources(),
            options.getOutput(),
            options.getLibraryInfoOutput(),
            options.getEmitReadableLibraryInfo(),
            options.getEmitReadableSourceMap(),
            options.getGenerateKytheIndexingMetadata(),
            options.getWasmEntryPoints(),
            problems);
  }

  private void desugarUnits(List<CompilationUnit> j2clUnits) {
    runPasses(j2clUnits, options.getBackend().getDesugaringPassFactories());
  }

  private void checkUnits(List<CompilationUnit> j2clUnits) {
    JsInteropRestrictionsChecker.check(
        j2clUnits, problems, /* enableWasmChecks= */ options.getBackend() == Backend.WASM);
    problems.abortIfHasErrors();
  }

  private void normalizeUnits(List<CompilationUnit> j2clUnits) {
    runPasses(
        j2clUnits,
        options.getBackend().getPassFactories(options.getExperimentalOptimizeAutovalue()));
  }

  private static void runPasses(
      List<CompilationUnit> j2clUnits, ImmutableList<Supplier<NormalizationPass>> passFactories) {
    for (CompilationUnit j2clUnit : j2clUnits) {
      for (Supplier<NormalizationPass> passFactory : passFactories) {
        passFactory.get().execute(j2clUnit);
      }
    }
  }
}
