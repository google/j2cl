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
import com.google.j2cl.ast.visitors.BridgeMethodsCreator;
import com.google.j2cl.ast.visitors.ControlStatementFormatter;
import com.google.j2cl.ast.visitors.CreateDefaultConstructors;
import com.google.j2cl.ast.visitors.CreateOverlayImplementationTypesAndDevirtualizeCalls;
import com.google.j2cl.ast.visitors.DefaultMethodsResolver;
import com.google.j2cl.ast.visitors.DevirtualizeBoxedTypesAndJsFunctionImplementations;
import com.google.j2cl.ast.visitors.DevirtualizeMethodCalls;
import com.google.j2cl.ast.visitors.ExpandCompoundAssignments;
import com.google.j2cl.ast.visitors.FixBooleanOperators;
import com.google.j2cl.ast.visitors.FixSuperCallQualifiers;
import com.google.j2cl.ast.visitors.FixTypeVariablesInMethods;
import com.google.j2cl.ast.visitors.InsertBoxingConversions;
import com.google.j2cl.ast.visitors.InsertCastOnNewInstances;
import com.google.j2cl.ast.visitors.InsertDivisionCoercions;
import com.google.j2cl.ast.visitors.InsertErasureTypeSafetyCasts;
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
import com.google.j2cl.ast.visitors.JsBridgeMethodsCreator;
import com.google.j2cl.ast.visitors.MakeEnumConstructionsExplicit;
import com.google.j2cl.ast.visitors.MoveVariableDeclarationsToEnclosingBlock;
import com.google.j2cl.ast.visitors.NormalizationPass;
import com.google.j2cl.ast.visitors.NormalizeArrayCreations;
import com.google.j2cl.ast.visitors.NormalizeArrayLiterals;
import com.google.j2cl.ast.visitors.NormalizeCasts;
import com.google.j2cl.ast.visitors.NormalizeCatchClauses;
import com.google.j2cl.ast.visitors.NormalizeConstructors;
import com.google.j2cl.ast.visitors.NormalizeEquality;
import com.google.j2cl.ast.visitors.NormalizeInstanceOfs;
import com.google.j2cl.ast.visitors.NormalizeIntersectionTypes;
import com.google.j2cl.ast.visitors.NormalizeJsDocAnnotatedExpression;
import com.google.j2cl.ast.visitors.NormalizeJsVarargs;
import com.google.j2cl.ast.visitors.NormalizeLongs;
import com.google.j2cl.ast.visitors.NormalizeMultiExpressions;
import com.google.j2cl.ast.visitors.NormalizeNestedClassConstructors;
import com.google.j2cl.ast.visitors.NormalizeStaticMemberQualifiers;
import com.google.j2cl.ast.visitors.NormalizeStaticNativeMemberReferences;
import com.google.j2cl.ast.visitors.NormalizeTryWithResources;
import com.google.j2cl.ast.visitors.OptimizeAnonymousInnerClassesToFunctionExpressions;
import com.google.j2cl.ast.visitors.PackagePrivateMethodsDispatcher;
import com.google.j2cl.ast.visitors.RemoveUnneededJsDocAnnotations;
import com.google.j2cl.ast.visitors.UnimplementedMethodsCreator;
import com.google.j2cl.ast.visitors.VerifyParamAndArgCounts;
import com.google.j2cl.ast.visitors.VerifySingleAstReference;
import com.google.j2cl.ast.visitors.VerifyVariableScoping;
import com.google.j2cl.common.TimingCollector;
import com.google.j2cl.frontend.CompilationUnitBuilder;
import com.google.j2cl.frontend.CompilationUnitsAndTypeBindings;
import com.google.j2cl.frontend.FrontendFlags;
import com.google.j2cl.frontend.FrontendOptions;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.frontend.PackageInfoCache;
import com.google.j2cl.generator.OutputGeneratorStage;
import com.google.j2cl.problems.Problems;
import com.google.j2cl.problems.Problems.Message;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/** Translation tool for generating JavaScript source files from Java sources. */
public class J2clTranspiler {
  private final Problems problems = new Problems();
  private FrontendOptions options;
  private final TimingCollector timingCollector = TimingCollector.get();

  /** Represents the result of a transpilation. */
  public static class Result {
    private final int exitCode;
    private final Problems problems;

    private Result(int exitCode, Problems problems) {
      this.exitCode = exitCode;
      this.problems = problems;
    }

    public int getExitCode() {
      return exitCode;
    }

    public Problems getProblems() {
      return problems;
    }

    static Result fromException(int exitCode, Throwable throwable) {
      return fromErrorMessage(exitCode, throwable.toString());
    }

    static Result fromErrorMessage(int exitCode, String errorMessage) {
      Problems problems = new Problems();
      problems.error(errorMessage);
      return new Result(exitCode, problems);
    }

    static Result fromOutputMessage(int exitCode, String outputMessage) {
      Problems problems = new Problems();
      problems.info(outputMessage);
      return new Result(exitCode, problems);
    }
  }

  /** Runs the entire J2CL pipeline. */
  Result transpile(String... args) {
    try {
      loadOptions(args);
      CompilationUnitsAndTypeBindings jdtUnitsAndResolvedBindings =
          createJdtUnitsAndResolveBindings();
      List<CompilationUnit> j2clUnits = convertUnits(jdtUnitsAndResolvedBindings);
      checkUnits(j2clUnits);
      normalizeUnits(j2clUnits);
      generateOutputs(j2clUnits);
      maybeCloseFileSystem();
      maybeOutputTimeReport();
    } catch (Problems.Exit e) {
      return new Result(e.getExitCode(), problems);
    }
    return new Result(0, problems);
  }

  private void loadOptions(String[] args) {
    timingCollector.startSubSample("Parse flags");

    FrontendFlags flags = new FrontendFlags(problems);
    flags.parse(args);
    problems.abortIfRequested();

    options = new FrontendOptions(problems, flags);
    problems.abortIfRequested();
  }

  private List<CompilationUnit> convertUnits(
      CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings) {
    timingCollector.startSample("AST Conversion");

    // Records information about package-info files supplied as byte code.
    PackageInfoCache.init(options.getClasspathEntries(), problems);
    problems.abortIfRequested();

    List<CompilationUnit> compilationUnits =
        CompilationUnitBuilder.build(compilationUnitsAndTypeBindings);
    problems.abortIfRequested();
    return compilationUnits;
  }

  private CompilationUnitsAndTypeBindings createJdtUnitsAndResolveBindings() {
    timingCollector.startSample("JDT Parse");

    JdtParser parser = new JdtParser(options, problems);
    CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings =
        parser.parseFiles(options.getSourceFiles());
    problems.abortIfRequested();
    return compilationUnitsAndTypeBindings;
  }

  private void checkUnits(List<CompilationUnit> j2clUnits) {
    timingCollector.startSample("Check Units");
    JsInteropRestrictionsChecker.check(j2clUnits, problems);
    problems.abortIfRequested();
  }

  private void normalizeUnits(List<CompilationUnit> j2clUnits) {
    timingCollector.startSample("Normalize Units");
    timingCollector.startSubSample("Create Pass List");

    List<NormalizationPass> passes =
        ImmutableList.of(
            // Class structure normalizations.
            new OptimizeAnonymousInnerClassesToFunctionExpressions(),
            // Default constructors and explicit super calls should be synthesized first.
            new CreateDefaultConstructors(),
            new InsertExplicitSuperCalls(),
            new DefaultMethodsResolver(),
            new PackagePrivateMethodsDispatcher(),
            new BridgeMethodsCreator(),
            new JsBridgeMethodsCreator(),
            new InsertErasureTypeSafetyCasts(),
            new DevirtualizeBoxedTypesAndJsFunctionImplementations(),
            new NormalizeIntersectionTypes(),
            new NormalizeTryWithResources(),
            new NormalizeCatchClauses(),
            // Runs before normalizing nested classes.
            new InsertCastOnNewInstances(),
            new MakeEnumConstructionsExplicit(),
            new FixSuperCallQualifiers(),
            new InsertInstanceInitCalls(),
            new NormalizeNestedClassConstructors(),
            // Runs at the very end of 'Class structure normalizations' section since we do not need
            // to apply other normalizations on the synthesized native JS types.
            new CreateOverlayImplementationTypesAndDevirtualizeCalls(),

            // Statement/Expression normalizations
            new NormalizeArrayLiterals(),
            new NormalizeStaticMemberQualifiers(),
            // Runs after NormalizeStaticMemberQualifiersPass.
            new DevirtualizeMethodCalls(),
            new ControlStatementFormatter(),
            new NormalizeMultiExpressions(),
            // Runs after NormalizeMultiExpressions to make sure it only sees valid l-values.
            new ExpandCompoundAssignments(),
            // Runs before unboxing conversion.
            new InsertStringConversions(),
            new InsertNarrowingReferenceConversions(),
            new InsertUnboxingConversions(),
            new InsertBoxingConversions(),
            new InsertNarrowingPrimitiveConversions(),
            new InsertWideningPrimitiveConversions(),
            // TODO: InsertWideningAndNarrowingPrimitiveConversionVisitor.applyTo(j2clUnit);
            new NormalizeLongs(),
            new InsertUnderflowOverflowConversions(),
            new InsertDivisionCoercions(),
            new FixBooleanOperators(),
            new ArrayAccessNormalizer(),
            new NormalizeConstructors(),
            new NormalizeCasts(),
            new NormalizeInstanceOfs(),
            new NormalizeEquality(),
            new NormalizeStaticNativeMemberReferences(),
            new NormalizeJsVarargs(),
            new NormalizeArrayCreations(),
            new InsertExceptionConversions(),
            new RemoveUnneededJsDocAnnotations(),
            new NormalizeJsDocAnnotatedExpression(),

            // Dodge JSCompiler limitations.
            new UnimplementedMethodsCreator(),
            // TODO: remove the temporary fix once switch to JSCompiler's new type checker.
            new InsertTypeAnnotationOnGenericReturnTypes(),
            // TODO: remove the temporary fix once we switch to JSCompiler's new type checker.
            new FixTypeVariablesInMethods(),
            new InsertStaticClassInitializerMethods(),
            // Normalize multiexpressions again to remove unnecessary clutter, but run before
            // variable motion.
            new NormalizeMultiExpressions(),
            new MoveVariableDeclarationsToEnclosingBlock());

    for (CompilationUnit j2clUnit : j2clUnits) {
      verifyUnit(j2clUnit);
      for (NormalizationPass pass : passes) {
        timingCollector.startSample("Pass " + pass.getClass().getName());
        pass.applyTo(j2clUnit);
      }
      verifyUnit(j2clUnit);
    }

    timingCollector.endSubSample(); // End the sub sample
  }

  private void verifyUnit(CompilationUnit j2clUnit) {
    timingCollector.startSample("Verify Unit");

    VerifySingleAstReference.applyTo(j2clUnit);
    VerifyParamAndArgCounts.applyTo(j2clUnit);
    VerifyVariableScoping.applyTo(j2clUnit);
  }

  private void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    timingCollector.startSample("Generate output");
    timingCollector.startSubSample("OutputGeneratorStage Constructor");

    new OutputGeneratorStage(
            Charset.forName(options.getEncoding()),
            options.getNativeSourceZipEntries(),
            options.getOutputFileSystem(),
            options.getOutput(),
            options.getDeclareLegacyNamespace(),
            options.getDepinfoPath(),
            options.getShouldPrintReadableSourceMap(),
            problems)
        .generateOutputs(j2clCompilationUnits);
    timingCollector.endSubSample();
    problems.abortIfRequested();
  }

  private void maybeCloseFileSystem() {
    timingCollector.startSample("Close File System");

    if (options.getOutputFileSystem() instanceof com.sun.nio.zipfs.ZipFileSystem) {
      try {
        options.getOutputFileSystem().close();
      } catch (IOException e) {
        problems.error(Message.ERR_CANNOT_CLOSE_ZIP, e.getMessage());
      }
    }
  }

  private void maybeOutputTimeReport() {
    timingCollector.endSubSample();
    if (options.getGenerateTimeReport()) {
      timingCollector.printReport();
    }
  }

  /** Entry point for the tool, which runs the entire J2CL pipeline. */
  public static void main(String[] args) {
    J2clTranspiler transpiler = new J2clTranspiler();
    Result result = transpiler.transpile(args);
    result.getProblems().report(System.out, System.err);
    System.exit(result.getExitCode());
  }
}
