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

import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.visitors.ControlStatementFormatter;
import com.google.j2cl.ast.visitors.CreateDefaultConstructorsVisitor;
import com.google.j2cl.ast.visitors.FixAnonymousClassConstructorsVisitor;
import com.google.j2cl.ast.visitors.FixSuperCallQualifiersVisitor;
import com.google.j2cl.ast.visitors.FixTypeVariableInMethodVisitors;
import com.google.j2cl.ast.visitors.InsertBoxingConversionVisitor;
import com.google.j2cl.ast.visitors.InsertExplicitSuperCallsVisitor;
import com.google.j2cl.ast.visitors.InsertInstanceInitCallsVisitor;
import com.google.j2cl.ast.visitors.InsertNarrowingPrimitiveConversionVisitor;
import com.google.j2cl.ast.visitors.InsertStringConversionVisitor;
import com.google.j2cl.ast.visitors.InsertUnboxingConversionVisitor;
import com.google.j2cl.ast.visitors.InsertWideningPrimitiveConversionVisitor;
import com.google.j2cl.ast.visitors.MakeExplicitEnumConstructionVisitor;
import com.google.j2cl.ast.visitors.NormalizeCastsVisitor;
import com.google.j2cl.ast.visitors.NormalizeLongsVisitor;
import com.google.j2cl.ast.visitors.NormalizeNestedClassConstructorsVisitor;
import com.google.j2cl.ast.visitors.RemoveUnusedMultiExpressionReturnValues;
import com.google.j2cl.ast.visitors.SplitCompoundLongAssignmentsVisitor;
import com.google.j2cl.ast.visitors.VerifyParamAndArgCountsVisitor;
import com.google.j2cl.common.VelocityUtil;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.frontend.CompilationUnitBuilder;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.generator.GeneratorUtils;
import com.google.j2cl.generator.JavaScriptHeaderGenerator;
import com.google.j2cl.generator.JavaScriptImplGenerator;

import org.apache.velocity.app.VelocityEngine;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translation tool for generating JavaScript source files from Java sources.
 */
public class J2clTranspiler {
  private final String[] args;
  private final Errors errors = new Errors();
  private FrontendOptions options;
  private final VelocityEngine velocityEngine = VelocityUtil.createEngine();

  private J2clTranspiler(String[] args) {
    this.args = args;
  }

  /**
   * Runs the entire J2CL pipeline.
   */
  private void run() {
    loadOptions();
    List<CompilationUnit> j2clUnits = convertUnits(createJdtUnits());
    normalizeUnits(j2clUnits);
    generateJsSources(j2clUnits);
    generateSourceMaps(j2clUnits);
  }

  private void loadOptions() {
    FrontendFlags flags = new FrontendFlags(errors);
    flags.parse(args);
    maybeExitGracefully();

    options = new FrontendOptions(errors, flags);
    maybeExitGracefully();
  }

  private List<CompilationUnit> convertUnits(
      Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath) {
    List<CompilationUnit> compilationUnits = CompilationUnitBuilder.build(jdtUnitsByFilePath);
    maybeExitGracefully();
    return compilationUnits;
  }

  private Map<String, org.eclipse.jdt.core.dom.CompilationUnit> createJdtUnits() {
    JdtParser parser = new JdtParser(options, errors);
    Map<String, org.eclipse.jdt.core.dom.CompilationUnit> jdtUnitsByFilePath =
        parser.parseFiles(options.getSourceFiles());
    maybeExitGracefully();
    return jdtUnitsByFilePath;
  }

  private void normalizeUnits(@SuppressWarnings("unused") List<CompilationUnit> j2clUnits) {
    for (CompilationUnit j2clUnit : j2clUnits) {
      verifyUnit(j2clUnit);

      // Class structure normalizations.
      // Default constructors and explicit super calls should be synthesized first.
      CreateDefaultConstructorsVisitor.applyTo(j2clUnit);
      InsertExplicitSuperCallsVisitor.applyTo(j2clUnit);

      FixAnonymousClassConstructorsVisitor.applyTo(j2clUnit);
      MakeExplicitEnumConstructionVisitor.applyTo(j2clUnit);
      FixSuperCallQualifiersVisitor.applyTo(j2clUnit);
      InsertInstanceInitCallsVisitor.applyTo(j2clUnit);
      NormalizeNestedClassConstructorsVisitor.applyTo(j2clUnit);

      // Statement/Expression normalizations
      ControlStatementFormatter.applyTo(j2clUnit);
      SplitCompoundLongAssignmentsVisitor.applyTo(j2clUnit);
      InsertUnboxingConversionVisitor.applyTo(j2clUnit);
      NormalizeLongsVisitor.applyTo(j2clUnit);
      // TODO: InsertUnderflowConversionVisitor.applyTo(j2clUnit);
      InsertNarrowingPrimitiveConversionVisitor.applyTo(j2clUnit);
      InsertWideningPrimitiveConversionVisitor.applyTo(j2clUnit);
      // TODO: InsertWideningAndNarrowingPrimitiveConversionVisitor.applyTo(j2clUnit);
      InsertBoxingConversionVisitor.applyTo(j2clUnit);
      InsertStringConversionVisitor.applyTo(j2clUnit);
      NormalizeCastsVisitor.applyTo(j2clUnit);

      // Dodge JSCompiler limitations.
      // TODO: remove the temporary fix once switch to JSCompiler's new type checker.
      FixTypeVariableInMethodVisitors.applyTo(j2clUnit);
      RemoveUnusedMultiExpressionReturnValues.applyTo(j2clUnit);

      verifyUnit(j2clUnit);
    }
  }

  private void verifyUnit(CompilationUnit j2clUnit) {
    VerifyParamAndArgCountsVisitor.applyTo(j2clUnit);
  }

  private void generateJsSources(List<CompilationUnit> j2clCompilationUnits) {
    // The parameters may be changed after the previous passes are implemented.
    Charset charset = Charset.forName(options.getEncoding());

    Set<String> superSourceFiles = new HashSet<>();
    superSourceFiles.addAll(options.getSuperSourceFiles());

    for (CompilationUnit j2clCompilationUnit : j2clCompilationUnits) {
      if (superSourceFiles.contains(j2clCompilationUnit.getFilePath())) {
        continue;
      }

      for (JavaType javaType : j2clCompilationUnit.getTypes()) {
        JavaScriptImplGenerator jsImplGenerator =
            new JavaScriptImplGenerator(errors, javaType, velocityEngine);
        Path absolutePathForImpl =
            GeneratorUtils.getAbsolutePath(
                options.getOutputFileSystem(),
                options.getOutput(),
                GeneratorUtils.getRelativePath(javaType),
                jsImplGenerator.getSuffix());
        jsImplGenerator.writeToFile(absolutePathForImpl, charset);

        JavaScriptHeaderGenerator jsHeaderGenerator =
            new JavaScriptHeaderGenerator(errors, javaType, velocityEngine);
        Path absolutePathForHeader =
            GeneratorUtils.getAbsolutePath(
                options.getOutputFileSystem(),
                options.getOutput(),
                GeneratorUtils.getRelativePath(javaType),
                jsHeaderGenerator.getSuffix());
        jsHeaderGenerator.writeToFile(absolutePathForHeader, charset);
      }
    }

    options.maybeCloseFileSystem();
  }

  private void generateSourceMaps(@SuppressWarnings("unused") List<CompilationUnit> j2clUnits) {
    // TODO: implement.
  }

  private void maybeExitGracefully() {
    if (errors.errorCount() > 0) {
      errors.report();
      throw new ExitGracefullyException();
    }
  }

  /**
   * Indicates that errors were found and reported and transpiler execution should now end silently.
   *
   * <p>Is preferred over System.exit() since it is caught here and will not end an external caller.
   */
  private class ExitGracefullyException extends RuntimeException {}

  /**
   * Entry point for the tool, which runs the entire J2CL pipeline.
   */
  public static void main(String[] args) {
    J2clTranspiler transpiler = new J2clTranspiler(args);
    try {
      transpiler.run();
    } catch (ExitGracefullyException e) {
      // Error already reported. End execution now and with an exit code that indicates failure.
      System.exit(1);
    }
  }
}
