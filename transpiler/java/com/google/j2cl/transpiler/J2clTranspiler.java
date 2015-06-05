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
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.generator.JavaScriptGenerator;
import com.google.j2cl.generator.VelocityUtil;

import org.apache.velocity.app.VelocityEngine;
import org.kohsuke.args4j.CmdLineException;

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
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit unit) {
    System.out.println(sourceFilePath);
    System.out.println(unit);

    // transform JDT AST to J2cl AST

    // normalize J2cl AST

    generateJsSource(sourceFilePath, unit);

    // output source map file.
  }

  private void generateJsSource(
      String sourceFilePath, org.eclipse.jdt.core.dom.CompilationUnit unit) {
    // The parameters may be changed after the previous passes are implemented.
    String unitName =
        sourceFilePath.substring(
            sourceFilePath.lastIndexOf('/') + 1, sourceFilePath.lastIndexOf('.'));
    String outputPath =
        unit
                .getPackage()
                .getName()
                .getFullyQualifiedName()
                .replace('.', '/')
            + "/"
            + unitName;
    File outputDirectory = options.getOutputDirectory();
    Charset charset = Charset.forName(options.getEncoding());
    // this is a dummy CompilationUnit instance, which should have been
    // generated from the previous passes.
    String packageName = unit.getPackage().getName().getFullyQualifiedName();
    CompilationUnit compilationUnit = new CompilationUnit(unitName, sourceFilePath, packageName);
    JavaScriptGenerator jsGenerator =
        new JavaScriptGenerator(
            errors, outputPath, outputDirectory, charset, compilationUnit, velocityEngine);
    jsGenerator.writeToFile();
  }

  /**
   * Runs the entire J2cl pipeline.
   */
  public void run() {
    JdtParser parser = new JdtParser(options, errors);
    parser.parseFiles(options.getSourceFiles(), this);
    errors.maybeReportAndExit();
  }

  /**
   * Entry point for the tool. Parses options, creates a J2cl transpiler and
   * calls run()
   */
  public static void main(String[] args) {
    Errors errors = new Errors();
    FrontendFlags flags = new FrontendFlags();
    try {
      flags.parse(args);
    } catch (CmdLineException e) {
      errors.error(Errors.ERR_INVALID_FLAG, e.getMessage());
    }
    errors.maybeReportAndExit();

    FrontendOptions options = new FrontendOptions(errors, flags);
    errors.maybeReportAndExit();

    J2clTranspiler transpiler = new J2clTranspiler(options, errors);
    transpiler.run();
  }
}
