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
package com.google.j2cl.transpiler;

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.frontend.CompilationUnitBuilder;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.generator.JavaScriptGenerator;
import com.google.j2cl.generator.VelocityUtil;

import org.apache.velocity.app.VelocityEngine;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Translation tool for generating JavaScript source files from Java sources.
 */
public class J2clTranspiler implements JdtParser.Handler {
  private final Errors errors;
  private final FrontendOptions options;
  /**
   * A VelocityEngine instance that is used for code generation and shared by
   * JavaScriptGenerator instances.
   */
  private final VelocityEngine velocityEngine;

  public J2clTranspiler(FrontendOptions options, Errors errors) {
    this.options = options;
    this.errors = errors;
    this.velocityEngine = VelocityUtil.createEngine();
  }

  @Override
  public void handleCompilationUnit(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit jdtCompilationUnit) {
    System.out.println(sourceFilePath);
    System.out.println(jdtCompilationUnit);

    // transform JDT AST to J2cl AST
    CompilationUnit j2clCompilationUnit =
        CompilationUnitBuilder.build(sourceFilePath, jdtCompilationUnit);

    // normalize J2cl AST

    generateJsSource(j2clCompilationUnit);

    // output source map file.
  }

  private void generateJsSource(CompilationUnit unit) {
    // The parameters may be changed after the previous passes are implemented.
    String outputPath = computeOutputPath(unit);
    File outputDirectory = options.getOutputDirectory();
    Charset charset = Charset.forName(options.getEncoding());
    // this is a dummy CompilationUnit instance, which should have been
    // generated from the previous passes.
    JavaScriptGenerator jsGenerator =
        new JavaScriptGenerator(errors, outputPath, outputDirectory, charset, unit, velocityEngine);
    jsGenerator.writeToFile();
  }

  private String computeOutputPath(CompilationUnit unit) {
    String unitName = unit.getName();
    String packageName = unit.getPackageName();

    return packageName.replace('.', '/') + "/" + unitName;
  }

  /**
   * Runs the entire J2cl pipeline.
   */
  public static void run(Errors errors, String[] args) {
    FrontendFlags flags = new FrontendFlags(errors);
    flags.parse(args);
    if (errors.errorCount() > 0) {
      errors.report();
      return;
    }

    FrontendOptions options = new FrontendOptions(errors, flags);
    if (errors.errorCount() > 0) {
      errors.report();
      return;
    }

    J2clTranspiler transpiler = new J2clTranspiler(options, errors);
    JdtParser parser = new JdtParser(options, errors);
    parser.parseFiles(options.getSourceFiles(), transpiler);
    if (errors.errorCount() > 0) {
      errors.report();
    }
  }

  /**
   * Entry point for the tool, which runs the entire J2cl pipeline.
   */
  public static void main(String[] args) {
    Errors errors = new Errors();
    run(errors, args);
  }
}
