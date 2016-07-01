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

import com.google.common.collect.ImmutableList;
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
import com.google.j2cl.ast.visitors.InsertCastOnNewInstances;
import com.google.j2cl.ast.visitors.InsertExceptionConversions;
import com.google.j2cl.ast.visitors.InsertExplicitSuperCalls;
import com.google.j2cl.ast.visitors.InsertInstanceInitCalls;
import com.google.j2cl.ast.visitors.InsertNarrowingPrimitiveConversions;
import com.google.j2cl.ast.visitors.InsertNarrowingReferenceConversions;
import com.google.j2cl.ast.visitors.InsertStaticClassInitializerMethods;
import com.google.j2cl.ast.visitors.InsertStringConversions;
import com.google.j2cl.ast.visitors.InsertTypeAnnotationOnGenericReturnTypes;
import com.google.j2cl.ast.visitors.InsertUnboxingConversions;
import com.google.j2cl.ast.visitors.InsertUnderflowOverflowConversions;
import com.google.j2cl.ast.visitors.InsertWideningPrimitiveConversions;
import com.google.j2cl.ast.visitors.MakeEnumConstructionsExplicit;
import com.google.j2cl.ast.visitors.NormalizationPass;
import com.google.j2cl.ast.visitors.NormalizeArrayCreations;
import com.google.j2cl.ast.visitors.NormalizeArrayLiterals;
import com.google.j2cl.ast.visitors.NormalizeCasts;
import com.google.j2cl.ast.visitors.NormalizeCatchClauses;
import com.google.j2cl.ast.visitors.NormalizeConstructors;
import com.google.j2cl.ast.visitors.NormalizeEquality;
import com.google.j2cl.ast.visitors.NormalizeInstanceOfs;
import com.google.j2cl.ast.visitors.NormalizeIntersectionTypes;
import com.google.j2cl.ast.visitors.NormalizeJsVarargs;
import com.google.j2cl.ast.visitors.NormalizeLongs;
import com.google.j2cl.ast.visitors.NormalizeMultiExpressions;
import com.google.j2cl.ast.visitors.NormalizeNestedClassConstructors;
import com.google.j2cl.ast.visitors.NormalizeStaticMemberQualifiersPass;
import com.google.j2cl.ast.visitors.NormalizeStaticNativeMemberReferences;
import com.google.j2cl.ast.visitors.NormalizeTryWithResources;
import com.google.j2cl.ast.visitors.OptimizeAnonymousInnerClassesToFunctionExpressions;
import com.google.j2cl.ast.visitors.RemoveUnusedMultiExpressionReturnValues;
import com.google.j2cl.ast.visitors.SplitCompoundLongAssignments;
import com.google.j2cl.ast.visitors.UnimplementedMethodsCreator;
import com.google.j2cl.ast.visitors.VerifyParamAndArgCounts;
import com.google.j2cl.errors.Errors;
import com.google.j2cl.frontend.CompilationUnitBuilder;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.frontend.PackageInfoCache;
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
    List<NormalizationPass> passes =
        ImmutableList.<NormalizationPass>of(
            // Class structure normalizations.
            new OptimizeAnonymousInnerClassesToFunctionExpressions(),
            // Default constructors and explicit super calls should be synthesized first.
            new CreateDefaultConstructors(),
            new InsertExplicitSuperCalls(),
            new DevirtualizeBoxedTypesAndJsFunctionImplementations(),
            new NormalizeIntersectionTypes(),
            new NormalizeTryWithResources(),
            new NormalizeCatchClauses(),
            // Runs before normalizing nested classes.
            new InsertCastOnNewInstances(),
            new FixAnonymousClassConstructors(),
            new MakeEnumConstructionsExplicit(),
            new FixSuperCallQualifiers(),
            new InsertInstanceInitCalls(),
            new NormalizeNestedClassConstructors(),
            // Runs at the very end of 'Class structure normalizations' section since we do not need
            // to apply other normalizations on the synthesized native JS types.
            new CreateOverlayImplementationTypesAndDevirtualizeCalls(),

            // Statement/Expression normalizations
            new NormalizeArrayLiterals(),
            new NormalizeStaticMemberQualifiersPass(),
            // Runs after NormalizeStaticMemberQualifiersPass.
            new DevirtualizeMethodCalls(),
            new ControlStatementFormatter(),
            new SplitCompoundLongAssignments(),
            new ArrayAccessNormalizer(),
            // Runs before unboxing conversion.
            new InsertNarrowingReferenceConversions(),
            new InsertUnboxingConversions(),
            new InsertBoxingConversion(),
            new InsertNarrowingPrimitiveConversions(),
            new InsertWideningPrimitiveConversions(),
            // TODO: InsertWideningAndNarrowingPrimitiveConversionVisitor.applyTo(j2clUnit);
            new NormalizeLongs(),
            new InsertUnderflowOverflowConversions(),
            new FixBooleanOperators(),
            new InsertStringConversions(),
            new NormalizeConstructors(),
            new NormalizeCasts(),
            new NormalizeInstanceOfs(),
            new NormalizeEquality(),
            new NormalizeStaticNativeMemberReferences(),
            new NormalizeJsVarargs(),
            new NormalizeArrayCreations(),
            new InsertExceptionConversions(),
            new NormalizeMultiExpressions(),

            // Dodge JSCompiler limitations.
            new UnimplementedMethodsCreator(),
            // TODO: remove the temporary fix once switch to JSCompiler's new type checker.
            new InsertTypeAnnotationOnGenericReturnTypes(),
            // TODO: remove the temporary fix once switch to JSCompiler's new type checker.
            new FixTypeVariablesInMethods(),
            new RemoveUnusedMultiExpressionReturnValues(),
            new InsertStaticClassInitializerMethods());

    for (CompilationUnit j2clUnit : j2clUnits) {
      verifyUnit(j2clUnit);
      for (NormalizationPass pass : passes) {
        pass.applyTo(j2clUnit);
      }
      verifyUnit(j2clUnit);
    }
  }

  private void verifyUnit(CompilationUnit j2clUnit) {
    VerifyParamAndArgCounts.applyTo(j2clUnit);
  }

  private void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    new OutputGeneratorStage(
            Charset.forName(options.getEncoding()),
            options.getNativeSourceZipEntries(),
            options.getOutputFileSystem(),
            options.getOutput(),
            options.getDeclareLegacyNamespace(),
            options.getDepinfoPath(),
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
  private static class ExitGracefullyException extends RuntimeException {}

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
