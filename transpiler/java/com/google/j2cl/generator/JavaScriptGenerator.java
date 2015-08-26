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

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.generator.visitors.Import;
import com.google.j2cl.generator.visitors.ImportGatheringVisitor;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.Set;

/**
 * Generates JavaScript source files.
 */
public class JavaScriptGenerator extends AbstractSourceGenerator {
  private static final String TEMPLATE_FILE_PATH = "com/google/j2cl/generator/JsCompilationUnit.vm";
  private final CompilationUnit compilationUnit;
  protected final VelocityEngine velocityEngine;

  public JavaScriptGenerator(
      Errors errors,
      FileSystem outputFileSystem,
      String outputLocationPath,
      Charset charset,
      CompilationUnit compilationUnit,
      VelocityEngine velocityEngine) {
    super(
        errors,
        outputFileSystem,
        outputLocationPath,
        createRelativeFilePath(compilationUnit),
        charset);
    this.compilationUnit = compilationUnit;
    this.velocityEngine = velocityEngine;
  }

  private static String createRelativeFilePath(CompilationUnit compilationUnit) {
    return TranspilerUtils.getOutputPath(compilationUnit);
  }

  @Override
  public String toSource() {
    VelocityContext velocityContext = createContext();
    StringWriter outputBuffer = new StringWriter();

    boolean success =
        velocityEngine.mergeTemplate(
            TEMPLATE_FILE_PATH, StandardCharsets.UTF_8.name(), velocityContext, outputBuffer);

    if (!success) {
      errors.error(Errors.ERR_CANNOT_GENERATE_OUTPUT);
      return "";
    }
    return outputBuffer.toString();
  }

  private VelocityContext createContext() {
    VelocityContext context = new VelocityContext();

    Set<Import> imports = ImportGatheringVisitor.gatherImports(compilationUnit);
    StatementSourceGenerator statementSourceGenerator = new StatementSourceGenerator(imports);

    context.put("compilationUnit", compilationUnit);
    context.put("TranspilerUtils", TranspilerUtils.class);
    context.put("ManglingNameUtils", ManglingNameUtils.class);
    context.put("imports", imports);
    context.put("statementSourceGenerator", statementSourceGenerator);
    context.put("javaLangClassTypeDecriptor", TypeDescriptors.CLASS_TYPE_DESCRIPTOR);
    context.put("nativeUtilTypeDecriptor", TypeDescriptors.NATIVE_UTIL_TYPE_DESCRIPTOR);

    return context;
  }

  @Override
  public String getSuffix() {
    return ".js";
  }
}
