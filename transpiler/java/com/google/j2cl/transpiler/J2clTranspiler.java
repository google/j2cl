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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.passes.LibraryNormalizationPass;
import com.google.j2cl.transpiler.passes.NormalizationPass;
import java.util.function.Supplier;

/** Translation tool for generating JavaScript source files from Java sources. */
class J2clTranspiler {

  /** Runs the entire J2CL pipeline. */
  static void transpile(J2clTranspilerOptions options, Problems problems) {
    new J2clTranspiler(options, problems).transpileImpl();
  }

  private final J2clTranspilerOptions options;
  private final Problems problems;

  private J2clTranspiler(J2clTranspilerOptions options, Problems problems) {
    this.options = options;
    this.problems = problems;
  }

  private void transpileImpl() {
    if (options.getBackend().isWasm()) {
      // TODO(b/178738483): Remove hack that makes mangling backend dependent.
      TypeDeclaration.setImplementWasmJsEnumSemantics();
      if (!options.getEnableWasmCustomDescriptorsJsInterop()) {
        // TODO(b/317164851): Remove hack that makes jsinfo ignored for non-native types in Wasm.
        FieldDescriptor.setIgnoreNonNativeJsInfo();
        MethodDescriptor.setIgnoreNonNativeJsInfo();
      }
      // TODO(b/340930928): This is a temporary hack since JsFunction is not supported in Wasm.
      TypeDeclaration.setIgnoreJsFunctionAnnotations();
      // TODO(b/178738483): Remove hack that makes it possible to ignore DoNotAutobox in Wasm.
      AstUtils.setIgnoreDoNotAutoboxAnnotations();
    } else if (options.getBackend().isClosure()) {
      MemberDescriptor.setClosureManglingPatterns();
    }

    Library library =
        options.getSources().isEmpty()
            ? Library.newEmpty()
            : options.getFrontend().parse(options, problems);
    try {
      problems.abortIfHasErrors();
      if (!library.isEmpty()) {
        desugarLibrary(library);
        checkLibrary(library);
        normalizeLibrary(library);
      }
      options.getBackend().generateOutputs(options, library, problems);
    } finally {
      // Now we are done, release resources from the frontend if needed.
      library.dispose();
    }
  }

  private void desugarLibrary(Library library) {
    runPasses(library, options.getBackend().getDesugaringPassFactories());
  }

  private void checkLibrary(Library library) {
    // Check backend-specific restrictions.
    options.getBackend().checkRestrictions(options, library, problems);

    problems.abortIfHasErrors();
  }

  private void normalizeLibrary(Library library) {
    runPasses(library, options.getBackend().getPassFactories(options));
  }

  private void runPasses(
      Library library, ImmutableList<Supplier<NormalizationPass>> passFactories) {
    for (Supplier<NormalizationPass> passFactory : passFactories) {
      NormalizationPass pass = instantiatePass(passFactory);
      if (pass instanceof LibraryNormalizationPass libraryNormalizationPass) {
        libraryNormalizationPass.execute(library);
        problems.abortIfHasErrors();
        continue;
      }
      for (CompilationUnit compilationUnit : library.getCompilationUnits()) {
        instantiatePass(passFactory).execute(compilationUnit);
        problems.abortIfCancelled();
      }
      problems.abortIfHasErrors();
    }
  }

  private NormalizationPass instantiatePass(Supplier<NormalizationPass> passFactory) {
    NormalizationPass pass = passFactory.get();
    pass.setProblems(problems);
    return pass;
  }
}
