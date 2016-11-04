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
package com.google.j2cl.generator;

import com.google.j2cl.ast.AnonymousType;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatherer.ImportCategory;
import com.google.j2cl.problems.Problems;
import java.util.HashSet;
import java.util.Set;

/**
 * Generates JavaScript source header files.
 */
public class JavaScriptHeaderGenerator extends JavaScriptGenerator {

  public static final String FILE_SUFFIX = ".java.js";
  public JavaScriptHeaderGenerator(Problems problems, boolean declareLegacyNamespace, Type type) {
    super(problems, declareLegacyNamespace, type);
  }

  @Override
  public String renderOutput() {
    TypeDescriptor selfTypeDescriptor = type.getDescriptor().getRawTypeDescriptor();
    String binaryName = type.getDescriptor().getRawTypeDescriptor().getBinaryName();
    sourceBuilder.appendLines(
        "/**",
        " * @fileoverview Header transpiled from " + binaryName,
        " *",
        " * @suppress {lateProvide}",
        " */",
        "goog.module('" + selfTypeDescriptor.getModuleName() + "');");
    sourceBuilder.newLine();

    if (declareLegacyNamespace
        && type.getDescriptor().isJsType()
        && !(type instanceof AnonymousType)) {
      // Even if opted into declareLegacyNamespace, this only makes sense for classes that are
      // intended to be accessed from the native JS. Thus we only emit declareLegacyNamespace
      // for non-anonymous JsType classes.
      sourceBuilder.appendln("goog.module.declareLegacyNamespace();");
    }

    sourceBuilder.newLine();
    sourceBuilder.newLine();
    sourceBuilder.appendLines(
        "// Imports headers for both eager and lazy dependencies to ensure that",
        "// all files are included in the dependency tree.");
    sourceBuilder.newLine();

    Set<String> requiredPaths = new HashSet<>();
    // goog.require(...) for eager imports.
    for (Import eagerImport : sortImports(importsByCategory.get(ImportCategory.EAGER))) {
      String alias = eagerImport.getAlias();
      String path = eagerImport.getHeaderModulePath();
      if (requiredPaths.add(path)) {
        sourceBuilder.appendln("let _" + alias + " = goog.require('" + path + "');");
      }
    }
    // goog.require(...) for lazy imports.
    for (Import lazyImport : sortImports(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = lazyImport.getAlias();
      String path = lazyImport.getHeaderModulePath();
      if (requiredPaths.add(path)) {
        sourceBuilder.appendln("let _" + alias + " = goog.require('" + path + "');");
      }
    }
    // externs imports are always available in the browser and don't need a header reference.
    sourceBuilder.newLine();
    sourceBuilder.newLine();

    String className = environment.aliasForType(type.getDescriptor());
    String implementationPath = selfTypeDescriptor.getImplModuleName();
    sourceBuilder.appendLines(
        "// Re-exports the implementation.",
        "var " + className + " = goog.require('" + implementationPath + "');",
        "exports = " + className + ";");
    sourceBuilder.newLine();
    return sourceBuilder.build();
  }

  @Override
  public String getSuffix() {
    return FILE_SUFFIX;
  }
}
