/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import com.google.j2cl.common.OutputUtils.Output;
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.backend.common.SourceBuilder;

/** Generates a JavaScript imports mapping for the WASM module. */
final class JsImportsGenerator {

  /** Top-level module name in the imports map containing all generated imports. */
  public static final String MODULE = "imports";

  /**
   * Gets the name of the JS import for the specified JS method. This is suitable for referencing
   * the function in WASM.
   */
  public static String getJsImportName(MethodDescriptor methodDescriptor) {
    String qualifiedJsName = methodDescriptor.getQualifiedJsName();
    if (methodDescriptor.isConstructor()) {
      // TODO(b/264466634): This is a hack that won't be needed after JS import generation is
      // implemented. After JS imports are generated, we won't need the constructor to be a
      // human-readable name, and this can be removed.
      qualifiedJsName = qualifiedJsName.replace("<init>", "constructor");
    } else if (methodDescriptor.isPropertyGetter()) {
      qualifiedJsName = "get " + qualifiedJsName;
    } else if (methodDescriptor.isPropertySetter()) {
      qualifiedJsName = "set " + qualifiedJsName;
    }
    return qualifiedJsName;
  }

  private final Problems problems;
  private final Output output;
  private final SourceBuilder builder = new SourceBuilder();

  public JsImportsGenerator(Output output, Problems problems) {
    this.output = output;
    this.problems = problems;
  }

  public void generateOutputs(Library library) {
    collectJsImports(library);
    emitRequires();
    emitJsImports();
    builder.newLine(); // Ends in a new line for human readability.
    output.write("imports.txt", builder.build());
  }

  private void collectJsImports(Library library) {
    // TODO(b/264466634): Implement JS import generation.
  }

  private void emitRequires() {
    // TODO(b/264466634): Implement JS import generation.
  }

  private void emitJsImports() {
    builder.newLine();
    builder.append("/** @return {!Object<!Object>} WASM import object */");
    builder.newLine();
    builder.append("function getImports() ");
    builder.openBrace();
    builder.newLine();
    builder.append("return ");
    builder.openBrace();
    builder.newLine();
    builder.append(String.format("'%s': ", MODULE));
    builder.openBrace();

    // TODO(b/264466634): Implement JS import generation.

    builder.closeBrace();
    builder.closeBrace();
    builder.append(";");
    builder.closeBrace();
  }
}
