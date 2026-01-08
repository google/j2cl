/*
 * Copyright 2025 Google Inc.
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
import com.google.j2cl.transpiler.ast.Library;

/** Generates JavaScript externs for allowing JavaScript callers to use exported JsTypes. */
final class JsExternsGenerator {

  private final WasmGenerationEnvironment environment;
  private final Output output;

  private JsExternsGenerator(Output output, WasmGenerationEnvironment environment) {
    this.environment = environment;
    this.output = output;
  }

  /** Generates the JavaScript code to support the imports. */
  public static void generateOutputs(
      Output output, WasmGenerationEnvironment environment, Library library) {
    JsExternsGenerator externsGenerator = new JsExternsGenerator(output, environment);
    externsGenerator.generateExterns(library);
  }

  private void generateExterns(Library library) {
    if (!environment.isCustomDescriptorsJsInteropEnabled()) {
      return;
    }

    // TODO(b/466162335): Generate externs for each type. Once complete, dependencies are resolved,
    // and JS code is complete, change to .js. This "externs.js" is a placeholder.
    output.write("externs/externs.js.txt", "");
  }
}
