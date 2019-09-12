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

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.common.Problems;
import com.google.j2cl.generator.ImportGatherer.ImportCategory;
import java.util.HashSet;
import java.util.Set;

/** Generates JavaScript source header files. */
public class JavaScriptHeaderGenerator extends JavaScriptGenerator {

  public static final String FILE_SUFFIX = ".java.js";

  public JavaScriptHeaderGenerator(Problems problems, boolean declareLegacyNamespace, Type type) {
    super(problems, declareLegacyNamespace, type);
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

    if (declareLegacyNamespace && AstUtils.canBeRequiredFromJs(typeDeclaration)) {
      // Even if opted into declareLegacyNamespace, this only makes sense for classes that are
      // intended to be accessed from the native JS. Thus we only emit declareLegacyNamespace
      // for non-anonymous JsType classes.
      sourceBuilder.appendln("goog.module.declareLegacyNamespace();");
    }

    sourceBuilder.newLine();

    sourceBuilder.emitWithPrunableMapping(this::renderImports);

    String className = environment.aliasForType(type.getDeclaration());
    String implementationPath = typeDeclaration.getImplModuleName();
    sourceBuilder.appendLines(
        "const " + className + " = goog.require('" + implementationPath + "');");
    sourceBuilder.newLine();
    // Since declareLegacyNamespace makes this class globally accessible via exports, also add a
    // mapping from the exports to the original Java class.
    sourceBuilder.emitWithMapping(type.getSourcePosition(), () -> sourceBuilder.append("exports"));
    sourceBuilder.append(" = " + className + ";");
    sourceBuilder.newLine();
    return sourceBuilder.build();
  }

  private void renderImports() {
    Set<String> alreadyRequiredPaths = new HashSet<>();
    // Make sure we don't self-import
    alreadyRequiredPaths.add(type.getDeclaration().getQualifiedJsName());
    // goog.require(...) for eager imports.
    for (Import eagerImport : sortImports(importsByCategory.get(ImportCategory.LOADTIME))) {
      String path = eagerImport.getHeaderModulePath();
      if (alreadyRequiredPaths.add(path)) {
        sourceBuilder.appendln("goog.require('" + path + "');");
      }
    }
    // goog.require(...) for lazy imports
    Iterable<Import> lazyImports =
        sortImports(
            Iterables.concat(
                importsByCategory.get(ImportCategory.RUNTIME),
                importsByCategory.get(ImportCategory.JSDOC)));
    for (Import lazyImport : lazyImports) {
      String path = lazyImport.getHeaderModulePath();
      if (alreadyRequiredPaths.add(path)) {
        sourceBuilder.appendln("goog.require('" + path + "');");
      }
    }
    // externs imports are always available in the browser and don't need a header reference.
    sourceBuilder.newLine();
  }

  @Override
  public String getSuffix() {
    return FILE_SUFFIX;
  }
}
