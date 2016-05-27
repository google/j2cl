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

import com.google.common.collect.Sets;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JsInteropRestrictionsChecker;
import com.google.j2cl.ast.visitors.ArrayAccessNormalizer;
import com.google.j2cl.ast.visitors.ControlStatementFormatter;
import com.google.j2cl.ast.visitors.CreateDefaultConstructors;
import com.google.j2cl.ast.visitors.CreateOverlayImplementationTypesAndDevirtualizeCalls;
import com.google.j2cl.ast.visitors.DevirtualizeBoxedTypesAndJsFunctionImplementations;
import com.google.j2cl.ast.visitors.DevirtualizeMethodCalls;
import com.google.j2cl.ast.visitors.FixAnonymousClassConstructors;
import com.google.j2cl.ast.visitors.FixBooleanOperators;
import com.google.j2cl.ast.visitors.FixSuperCallQualifiers;
import com.google.j2cl.ast.visitors.FixTypeVariablesInMethods;
import com.google.j2cl.ast.visitors.InsertBoxingConversion;
import com.google.j2cl.ast.visitors.InsertCastOnGenericReturnTypes;
import com.google.j2cl.ast.visitors.InsertCastOnNewInstances;
import com.google.j2cl.ast.visitors.InsertCastOnNullabilityMismatch;
import com.google.j2cl.ast.visitors.InsertExceptionConversions;
import com.google.j2cl.ast.visitors.InsertExplicitSuperCalls;
import com.google.j2cl.ast.visitors.InsertInstanceInitCalls;
import com.google.j2cl.ast.visitors.InsertNarrowingPrimitiveConversions;
import com.google.j2cl.ast.visitors.InsertNarrowingReferenceConversions;
import com.google.j2cl.ast.visitors.InsertStaticClassInitializerMethods;
import com.google.j2cl.ast.visitors.InsertStringConversions;
import com.google.j2cl.ast.visitors.InsertUnboxingConversions;
import com.google.j2cl.ast.visitors.InsertUnderflowOverflowConversions;
import com.google.j2cl.ast.visitors.InsertWideningPrimitiveConversions;
import com.google.j2cl.ast.visitors.MakeEnumConstructionsExplicit;
import com.google.j2cl.ast.visitors.NormalizeArrayCreations;
import com.google.j2cl.ast.visitors.NormalizeArrayLiterals;
import com.google.j2cl.ast.visitors.NormalizeCasts;
import com.google.j2cl.ast.visitors.NormalizeCatchClauses;
import com.google.j2cl.ast.visitors.NormalizeConstructors;
import com.google.j2cl.ast.visitors.NormalizeEquality;
import com.google.j2cl.ast.visitors.NormalizeInstanceOfs;
import com.google.j2cl.ast.visitors.NormalizeJsVarargs;
import com.google.j2cl.ast.visitors.NormalizeLongs;
import com.google.j2cl.ast.visitors.NormalizeMultiExpressions;
import com.google.j2cl.ast.visitors.NormalizeNativeMethodCalls;
import com.google.j2cl.ast.visitors.NormalizeNestedClassConstructors;
import com.google.j2cl.ast.visitors.NormalizeStaticMemberQualifiersPass;
import com.google.j2cl.ast.visitors.NormalizeTryWithResources;
import com.google.j2cl.ast.visitors.RemoveUnusedMultiExpressionReturnValues;
import com.google.j2cl.ast.visitors.SplitCompoundLongAssignments;
import com.google.j2cl.ast.visitors.VerifyParamAndArgCounts;
import com.google.j2cl.common.PackageInfoCache;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.frontend.CompilationUnitBuilder;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.generator.OutputGeneratorStage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

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
    checkUnits(j2clUnits);
    normalizeUnits(j2clUnits);
    generateOutputs(j2clUnits);
    maybeCloseFileSystem();
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
    // Records information about package-info files supplied as byte code.
    PackageInfoCache.init(options.getClasspathEntries(), errors);
    maybeExitGracefully();

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

  private void checkUnits(List<CompilationUnit> j2clUnits) {
    for (CompilationUnit compilationUnit : j2clUnits) {
      JsInteropRestrictionsChecker.check(compilationUnit, errors);
    }
    maybeExitGracefully();
  }

  private void normalizeUnits(List<CompilationUnit> j2clUnits) {
    for (CompilationUnit j2clUnit : j2clUnits) {
      verifyUnit(j2clUnit);

      // Class structure normalizations.
      // Default constructors and explicit super calls should be synthesized first.
      CreateDefaultConstructors.applyTo(j2clUnit);
      InsertExplicitSuperCalls.applyTo(j2clUnit);
      DevirtualizeBoxedTypesAndJsFunctionImplementations.applyTo(j2clUnit);

      NormalizeTryWithResources.applyTo(j2clUnit);
      NormalizeCatchClauses.applyTo(j2clUnit);
      // Runs before normalizing nested classes.
      InsertCastOnNewInstances.applyTo(j2clUnit);
      FixAnonymousClassConstructors.applyTo(j2clUnit);
      MakeEnumConstructionsExplicit.applyTo(j2clUnit);
      FixSuperCallQualifiers.applyTo(j2clUnit);
      InsertInstanceInitCalls.applyTo(j2clUnit);
      NormalizeNestedClassConstructors.applyTo(j2clUnit);
      // Runs at the very end of 'Class structure normalizations' section since we do not need to
      // apply other normalizations on the synthesized native JS types.
      CreateOverlayImplementationTypesAndDevirtualizeCalls.applyTo(j2clUnit);

      // Statement/Expression normalizations
      NormalizeArrayLiterals.applyTo(j2clUnit);
      NormalizeStaticMemberQualifiersPass.applyTo(j2clUnit);
      // Runs after NormalizeStaticMemberQualifiersPass.
      DevirtualizeMethodCalls.applyTo(j2clUnit);
      ControlStatementFormatter.applyTo(j2clUnit);
      SplitCompoundLongAssignments.applyTo(j2clUnit);
      ArrayAccessNormalizer.applyTo(j2clUnit);
      // Runs before unboxing conversion.
      InsertNarrowingReferenceConversions.applyTo(j2clUnit);
      InsertUnboxingConversions.applyTo(j2clUnit);
      InsertBoxingConversion.applyTo(j2clUnit);
      InsertNarrowingPrimitiveConversions.applyTo(j2clUnit);
      InsertWideningPrimitiveConversions.applyTo(j2clUnit);
      // TODO: InsertWideningAndNarrowingPrimitiveConversionVisitor.applyTo(j2clUnit);
      NormalizeLongs.applyTo(j2clUnit);
      InsertUnderflowOverflowConversions.applyTo(j2clUnit);
      FixBooleanOperators.applyTo(j2clUnit);
      InsertStringConversions.applyTo(j2clUnit);
      NormalizeConstructors.applyTo(j2clUnit);
      NormalizeCasts.applyTo(j2clUnit);
      NormalizeInstanceOfs.applyTo(j2clUnit);
      NormalizeEquality.applyTo(j2clUnit);
      NormalizeNativeMethodCalls.applyTo(j2clUnit);
      NormalizeJsVarargs.applyTo(j2clUnit);
      NormalizeArrayCreations.applyTo(j2clUnit);
      InsertExceptionConversions.applyTo(j2clUnit);
      NormalizeMultiExpressions.applyTo(j2clUnit);
      InsertCastOnNullabilityMismatch.applyTo(j2clUnit);

      // Dodge JSCompiler limitations.
      // TODO: remove the temporary fix once switch to JSCompiler's new type checker.
      InsertCastOnGenericReturnTypes.applyTo(j2clUnit);
      // TODO: remove the temporary fix once switch to JSCompiler's new type checker.
      FixTypeVariablesInMethods.applyTo(j2clUnit);
      RemoveUnusedMultiExpressionReturnValues.applyTo(j2clUnit);
      InsertStaticClassInitializerMethods.applyTo(j2clUnit);
      verifyUnit(j2clUnit);
    }
  }

  private void verifyUnit(CompilationUnit j2clUnit) {
    VerifyParamAndArgCounts.applyTo(j2clUnit);
  }

  private void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    Charset charset = Charset.forName(options.getEncoding());

    new OutputGeneratorStage(
            charset,
            Sets.newHashSet(options.getOmitSourceFiles()),
            options.getNativeSourceZipEntries(),
            options.getOutputFileSystem(),
            options.getOutput(),
            options.getDeclareLegacyNamespace(),
            options.getShouldPrintReadableSourceMap(),
            errors)
        .generateOutputs(j2clCompilationUnits);
    maybeExitGracefully();
  }

  private void maybeCloseFileSystem() {
    if (options.getOutputFileSystem() instanceof com.sun.nio.zipfs.ZipFileSystem) {
      try {
        options.getOutputFileSystem().close();
      } catch (IOException e) {
        errors.error(Errors.Error.ERR_CANNOT_CLOSE_ZIP);
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
