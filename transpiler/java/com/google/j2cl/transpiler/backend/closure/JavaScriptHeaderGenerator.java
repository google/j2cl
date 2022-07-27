/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.transpiler.backend.closure;

import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Generates JavaScript source header files. */
public class JavaScriptHeaderGenerator extends JavaScriptGenerator {

  public static final String FILE_SUFFIX = ".java.js";

  public JavaScriptHeaderGenerator(Problems problems, Type type, List<Import> imports) {
    super(problems, type, imports);
  }

  @Override
  public String renderOutput() {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    sourceBuilder.append("goog.module(");
    sourceBuilder.emitWithMapping(
        type.getSourcePosition(),
        () -> sourceBuilder.append("'" + typeDeclaration.getModuleName() + "'"));
    sourceBuilder.append(");");
    sourceBuilder.newLine();
    sourceBuilder.newLine();

    Set<String> alreadyRequiredPaths = new HashSet<>();
    // Make sure we don't self-import
    alreadyRequiredPaths.add(type.getDeclaration().getQualifiedJsName());

    // goog.require(...) for all imports
    imports.stream()
        .filter(i -> i.getImportCategory().needsGoogRequireInHeader())
        .forEach(
            headerImport -> {
              String path = headerImport.getHeaderModulePath();
              if (alreadyRequiredPaths.add(path)) {
                sourceBuilder.appendln("goog.require('" + path + "');");
              }
            });
    // externs imports are always available in the browser and don't need a header reference.
    sourceBuilder.newLine();

    String className = environment.aliasForType(type.getDeclaration());
    String implementationPath = typeDeclaration.getImplModuleName();
    sourceBuilder.appendLines(
        "const " + className + " = goog.require('" + implementationPath + "');");
    sourceBuilder.newLine();

    if (type.isOverlayImplementation()) {
      sourceBuilder.appendln("/** @nodts */");
    }
    // Emit a source-mapping for the exports statement back to the original Java file. Kythe models
    // imports using goog.require as an alias to the exports symbol of the goog.module file being
    // imported. This mapping will help kythe understand that the exports symbol is really just
    // the Java class.
    sourceBuilder.emitWithMapping(type.getSourcePosition(), () -> sourceBuilder.append("exports"));
    sourceBuilder.appendln(" = " + className + ";");
    return sourceBuilder.build();
  }

  @Override
  public String getSuffix() {
    return FILE_SUFFIX;
  }
}
