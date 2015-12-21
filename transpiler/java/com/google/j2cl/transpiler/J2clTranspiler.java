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
import com.google.j2cl.ast.visitors.AddClassInitAtCallSite;
import com.google.j2cl.ast.visitors.ControlStatementFormatter;
import com.google.j2cl.ast.visitors.CreateDefaultConstructorsVisitor;
import com.google.j2cl.ast.visitors.CreateDevirtualizedStaticMethodsVisitor;
import com.google.j2cl.ast.visitors.CreateNativeTypeImplVisitor;
import com.google.j2cl.ast.visitors.DevirtualizeJsOverlayMemberReferencesVisitor;
import com.google.j2cl.ast.visitors.DevirtualizeMethodCallsVisitor;
import com.google.j2cl.ast.visitors.FixAnonymousClassConstructorsVisitor;
import com.google.j2cl.ast.visitors.FixBooleanOperatorsPass;
import com.google.j2cl.ast.visitors.FixSuperCallQualifiersVisitor;
import com.google.j2cl.ast.visitors.FixTypeVariableInMethodVisitors;
import com.google.j2cl.ast.visitors.InsertBoxingConversionVisitor;
import com.google.j2cl.ast.visitors.InsertClassInitStaticMethods;
import com.google.j2cl.ast.visitors.InsertExceptionConversionVisitor;
import com.google.j2cl.ast.visitors.InsertExplicitSuperCallsVisitor;
import com.google.j2cl.ast.visitors.InsertInstanceInitCallsVisitor;
import com.google.j2cl.ast.visitors.InsertNarrowingPrimitiveConversionVisitor;
import com.google.j2cl.ast.visitors.InsertStringConversionVisitor;
import com.google.j2cl.ast.visitors.InsertUnboxingConversionVisitor;
import com.google.j2cl.ast.visitors.InsertUnderflowOverflowConversionVisitor;
import com.google.j2cl.ast.visitors.InsertWideningPrimitiveConversionVisitor;
import com.google.j2cl.ast.visitors.MakeExplicitEnumConstructionVisitor;
import com.google.j2cl.ast.visitors.NormalizeArrayCreationsVisitor;
import com.google.j2cl.ast.visitors.NormalizeArrayLiteralsPass;
import com.google.j2cl.ast.visitors.NormalizeCastsVisitor;
import com.google.j2cl.ast.visitors.NormalizeCatchClausesVisitor;
import com.google.j2cl.ast.visitors.NormalizeEqualityVisitor;
import com.google.j2cl.ast.visitors.NormalizeInstanceOfsVisitor;
import com.google.j2cl.ast.visitors.NormalizeLongsVisitor;
import com.google.j2cl.ast.visitors.NormalizeNativeMethodCalls;
import com.google.j2cl.ast.visitors.NormalizeNestedClassConstructorsVisitor;
import com.google.j2cl.ast.visitors.NormalizeStaticMemberQualifiersPass;
import com.google.j2cl.ast.visitors.NormalizeTryWithResourceVisitor;
import com.google.j2cl.ast.visitors.RemoveUnusedMultiExpressionReturnValues;
import com.google.j2cl.ast.visitors.RewriteSystemGetPropertyVisitor;
import com.google.j2cl.ast.visitors.SourceInfoPrinter;
import com.google.j2cl.ast.visitors.SourceInfoPrinter.Type;
import com.google.j2cl.ast.visitors.SplitCompoundLongAssignmentsVisitor;
import com.google.j2cl.ast.visitors.VerifyParamAndArgCountsVisitor;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.frontend.CompilationUnitBuilder;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.generator.JavaScriptGeneratorStage;

import java.nio.charset.Charset;
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
    generateJavaScriptSources(j2clUnits);
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

  private void normalizeUnits(List<CompilationUnit> j2clUnits) {
    for (CompilationUnit j2clUnit : j2clUnits) {
      verifyUnit(j2clUnit);

      if (options.getShouldPrintInputSourceInfo()) {
        SourceInfoPrinter.applyTo(j2clUnit, Type.INPUT);
      }

      // Class structure normalizations.
      // Default constructors and explicit super calls should be synthesized first.
      CreateDefaultConstructorsVisitor.applyTo(j2clUnit);
      InsertExplicitSuperCallsVisitor.applyTo(j2clUnit);
      CreateDevirtualizedStaticMethodsVisitor.applyTo(j2clUnit);

      NormalizeTryWithResourceVisitor.applyTo(j2clUnit);
      NormalizeCatchClausesVisitor.applyTo(j2clUnit);
      FixAnonymousClassConstructorsVisitor.applyTo(j2clUnit);
      MakeExplicitEnumConstructionVisitor.applyTo(j2clUnit);
      FixSuperCallQualifiersVisitor.applyTo(j2clUnit);
      InsertInstanceInitCallsVisitor.applyTo(j2clUnit);
      NormalizeNestedClassConstructorsVisitor.applyTo(j2clUnit);
      // Runs at the very end of 'Class structure normalizations' section since we do not need to
      // apply other normalizations on the synthesized native JS types.
      CreateNativeTypeImplVisitor.applyTo(j2clUnit);

      // Statement/Expression normalizations
      RewriteSystemGetPropertyVisitor.applyTo(j2clUnit);
      NormalizeArrayLiteralsPass.applyTo(j2clUnit);
      NormalizeStaticMemberQualifiersPass.applyTo(j2clUnit);
      // Runs after NormalizeStaticMemberQualifiersPass.
      DevirtualizeJsOverlayMemberReferencesVisitor.applyTo(j2clUnit);
      DevirtualizeMethodCallsVisitor.applyTo(j2clUnit);
      ControlStatementFormatter.applyTo(j2clUnit);
      SplitCompoundLongAssignmentsVisitor.applyTo(j2clUnit);
      InsertUnboxingConversionVisitor.applyTo(j2clUnit);
      NormalizeLongsVisitor.applyTo(j2clUnit);
      InsertNarrowingPrimitiveConversionVisitor.applyTo(j2clUnit);
      InsertWideningPrimitiveConversionVisitor.applyTo(j2clUnit);
      // TODO: InsertWideningAndNarrowingPrimitiveConversionVisitor.applyTo(j2clUnit);
      InsertUnderflowOverflowConversionVisitor.applyTo(j2clUnit);
      FixBooleanOperatorsPass.applyTo(j2clUnit);
      InsertBoxingConversionVisitor.applyTo(j2clUnit);
      InsertStringConversionVisitor.applyTo(j2clUnit);
      NormalizeCastsVisitor.applyTo(j2clUnit);
      NormalizeInstanceOfsVisitor.applyTo(j2clUnit);
      NormalizeEqualityVisitor.applyTo(j2clUnit);
      NormalizeNativeMethodCalls.applyTo(j2clUnit);
      NormalizeArrayCreationsVisitor.applyTo(j2clUnit);
      InsertExceptionConversionVisitor.applyTo(j2clUnit);

      // Dodge JSCompiler limitations.
      // TODO: remove the temporary fix once switch to JSCompiler's new type checker.
      FixTypeVariableInMethodVisitors.applyTo(j2clUnit);
      RemoveUnusedMultiExpressionReturnValues.applyTo(j2clUnit);
      // TODO: remove this pass when b/25512693 is resolved.
      AddClassInitAtCallSite.applyTo(j2clUnit);
      // has to be done after AddClassInitAtCallSite
      InsertClassInitStaticMethods.applyTo(j2clUnit);

      verifyUnit(j2clUnit);
    }
  }

  private void verifyUnit(CompilationUnit j2clUnit) {
    VerifyParamAndArgCountsVisitor.applyTo(j2clUnit);
  }

  private void generateJavaScriptSources(List<CompilationUnit> j2clCompilationUnits) {
    Charset charset = Charset.forName(options.getEncoding());

    Set<String> omitSourceFiles = new HashSet<>();
    omitSourceFiles.addAll(options.getOmitSourceFiles());

    Set<String> nativeJavaScriptFileZipPaths = new HashSet<>();
    nativeJavaScriptFileZipPaths.addAll(options.getNativeSourceZipEntries());

    new JavaScriptGeneratorStage(
            charset,
            omitSourceFiles,
            nativeJavaScriptFileZipPaths,
            options.getOutputFileSystem(),
            options.getOutput(),
            errors)
        .generateJavaScriptSources(j2clCompilationUnits);
    maybeExitGracefully();
  }

  private void generateSourceMaps(List<CompilationUnit> j2clUnits) {
    if (options.getShouldPrintOutputSourceInfo()) {
      for (CompilationUnit j2clUnit : j2clUnits) {
        SourceInfoPrinter.applyTo(j2clUnit, Type.OUTPUT);
      }
    }
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
