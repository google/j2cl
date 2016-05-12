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

import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor.ImportCategory;
import com.google.j2cl.generator.visitors.ImportUtils;

/**
 * Generates JavaScript source header files.
 */
public class JavaScriptHeaderGenerator extends JavaScriptGenerator {

  public JavaScriptHeaderGenerator(
      Errors errors, boolean declareLegacyNamespace, JavaType javaType) {
    super(errors, declareLegacyNamespace, javaType);
  }

  @Override
  public String toSource() {
    TypeDescriptor selfTypeDescriptor = javaType.getDescriptor().getRawTypeDescriptor();
    Import selfImport = new Import(selfTypeDescriptor.getSimpleName(), selfTypeDescriptor);
    String binaryName = javaType.getDescriptor().getRawTypeDescriptor().getBinaryName();
    sb = new SourceBuilder();
    sb.appendln("/**");
    sb.appendln(" * @fileoverview Header transpiled from %s", binaryName);
    sb.appendln(" *");
    sb.appendln(" * @suppress {lateProvide}");
    sb.appendln(" */");
    sb.appendln("goog.module('%s');", selfImport.getHeaderModulePath());
    if (declareLegacyNamespace) {
      sb.appendln("goog.module.declareLegacyNamespace();");
    }
    sb.newLine();
    sb.newLine();
    sb.appendln("// Imports headers for both eager and lazy dependencies to ensure that");
    sb.appendln("// all files are included in the dependency tree.");

    // goog.require(...) for eager imports.
    for (Import eagerImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.EAGER))) {
      String alias = eagerImport.getAlias();
      String path = eagerImport.getHeaderModulePath();
      sb.appendln("let _%s = goog.require('%s');", alias, path);
    }
    // goog.require(...) for lazy imports.
    for (Import eagerImport : ImportUtils.sortedList(importsByCategory.get(ImportCategory.LAZY))) {
      String alias = eagerImport.getAlias();
      String path = eagerImport.getHeaderModulePath();
      sb.appendln("let _%s = goog.require('%s');", alias, path);
    }
    // externs imports are always available in the browser and don't need a header reference.
    sb.newLine();
    sb.newLine();

    String className = environment.aliasForType(javaType.getDescriptor());
    String implementationPath = selfImport.getImplModulePath();
    sb.appendln("// Re-exports the implementation.");
    sb.appendln("var %s = goog.require('%s');", className, implementationPath);
    sb.appendln("exports = %s;", className);
    return sb.build();
  }

  @Override
  public String getSuffix() {
    return ".js";
  }
}
