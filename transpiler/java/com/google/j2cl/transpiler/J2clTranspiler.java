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
import com.google.common.util.concurrent.Futures;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.visitors.ArrayAccessNormalizer;
import com.google.j2cl.ast.visitors.BridgeMethodsCreator;
import com.google.j2cl.ast.visitors.ControlStatementFormatter;
import com.google.j2cl.ast.visitors.CreateDefaultConstructors;
import com.google.j2cl.ast.visitors.DefaultMethodsResolver;
import com.google.j2cl.ast.visitors.DevirtualizeBoxedTypesAndJsFunctionImplementations;
import com.google.j2cl.ast.visitors.DevirtualizeMethodCalls;
import com.google.j2cl.ast.visitors.ExpandCompoundAssignments;
import com.google.j2cl.ast.visitors.FilloutMissingSourceMapInformation;
import com.google.j2cl.ast.visitors.FixSuperCallQualifiers;
import com.google.j2cl.ast.visitors.ImplementAssertStatements;
import com.google.j2cl.ast.visitors.ImplementInstanceInitialization;
import com.google.j2cl.ast.visitors.ImplementLambdaExpressions;
import com.google.j2cl.ast.visitors.ImplementStaticInitialization;
import com.google.j2cl.ast.visitors.ImplementSynchronizedStatements;
import com.google.j2cl.ast.visitors.InsertBitwiseOperatorBooleanCoercions;
import com.google.j2cl.ast.visitors.InsertBoxingConversions;
import com.google.j2cl.ast.visitors.InsertCastOnNewInstances;
import com.google.j2cl.ast.visitors.InsertCastsToTypeBounds;
import com.google.j2cl.ast.visitors.InsertDivisionCoercions;
import com.google.j2cl.ast.visitors.InsertErasureTypeSafetyCasts;
import com.google.j2cl.ast.visitors.InsertExceptionConversions;
import com.google.j2cl.ast.visitors.InsertExplicitSuperCalls;
import com.google.j2cl.ast.visitors.InsertJsEnumBoxingAndUnboxingConversions;
import com.google.j2cl.ast.visitors.InsertNarrowingPrimitiveConversions;
import com.google.j2cl.ast.visitors.InsertNarrowingReferenceConversions;
import com.google.j2cl.ast.visitors.InsertStringConversions;
import com.google.j2cl.ast.visitors.InsertTypeAnnotationOnGenericReturnTypes;
import com.google.j2cl.ast.visitors.InsertUnboxingConversions;
import com.google.j2cl.ast.visitors.InsertUnsignedRightShiftCoercions;
import com.google.j2cl.ast.visitors.InsertWideningPrimitiveConversions;
import com.google.j2cl.ast.visitors.JsBridgeMethodsCreator;
import com.google.j2cl.ast.visitors.JsInteropRestrictionsChecker;
import com.google.j2cl.ast.visitors.MoveVariableDeclarationsToEnclosingBlock;
import com.google.j2cl.ast.visitors.NormalizationPass;
import com.google.j2cl.ast.visitors.NormalizeArrayCreations;
import com.google.j2cl.ast.visitors.NormalizeArrayLiterals;
import com.google.j2cl.ast.visitors.NormalizeCasts;
import com.google.j2cl.ast.visitors.NormalizeCatchClauses;
import com.google.j2cl.ast.visitors.NormalizeConstructors;
import com.google.j2cl.ast.visitors.NormalizeEnumClasses;
import com.google.j2cl.ast.visitors.NormalizeEquality;
import com.google.j2cl.ast.visitors.NormalizeFieldInitialization;
import com.google.j2cl.ast.visitors.NormalizeInstanceOfs;
import com.google.j2cl.ast.visitors.NormalizeInterfaceMethods;
import com.google.j2cl.ast.visitors.NormalizeJsAwaitMethodInvocations;
import com.google.j2cl.ast.visitors.NormalizeJsDocCastExpressions;
import com.google.j2cl.ast.visitors.NormalizeJsEnums;
import com.google.j2cl.ast.visitors.NormalizeJsFunctionPropertyInvocations;
import com.google.j2cl.ast.visitors.NormalizeJsOverlayMembers;
import com.google.j2cl.ast.visitors.NormalizeJsVarargs;
import com.google.j2cl.ast.visitors.NormalizeLiterals;
import com.google.j2cl.ast.visitors.NormalizeLongs;
import com.google.j2cl.ast.visitors.NormalizeMultiExpressions;
import com.google.j2cl.ast.visitors.NormalizeNestedClassConstructors;
import com.google.j2cl.ast.visitors.NormalizeStaticMemberQualifiers;
import com.google.j2cl.ast.visitors.NormalizeStaticNativeMemberReferences;
import com.google.j2cl.ast.visitors.NormalizeSwitchStatements;
import com.google.j2cl.ast.visitors.NormalizeTryWithResources;
import com.google.j2cl.ast.visitors.NormalizeTypeLiterals;
import com.google.j2cl.ast.visitors.OptimizeAnonymousInnerClassesToFunctionExpressions;
import com.google.j2cl.ast.visitors.PackagePrivateMethodsDispatcher;
import com.google.j2cl.ast.visitors.RemoveUnneededJsDocCasts;
import com.google.j2cl.ast.visitors.UnimplementedMethodsCreator;
import com.google.j2cl.ast.visitors.VerifyParamAndArgCounts;
import com.google.j2cl.ast.visitors.VerifySingleAstReference;
import com.google.j2cl.ast.visitors.VerifyVariableScoping;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.frontend.CompilationUnitBuilder;
import com.google.j2cl.frontend.CompilationUnitsAndTypeBindings;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.frontend.PackageInfoCache;
import com.google.j2cl.generator.OutputGeneratorStage;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Translation tool for generating JavaScript source files from Java sources. */
class J2clTranspiler {

  /** Runs the entire J2CL pipeline. */
  static Problems transpile(J2clTranspilerOptions options) {
    // Compiler has no static state, but rather uses thread local variables.
    // Because of this, we invoke the compiler on a different thread each time.
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Problems> result =
        executorService.submit(() -> new J2clTranspiler(options).transpileImpl());
    // Shutdown the executor service since it will only run a single transpilation. If not shutdown
    // it prevents the JVM from ending the process (see Executors.newFixedThreadPool()). This is not
    // normally obvserved since the transpiler in normal circumstances ends with System.exit() which
    // ends all threads. But when the transpilation throws an exception, the exception propagates
    // out of main() and the process lingers due the live threads from these executors.
    executorService.shutdown();
    return Futures.getUnchecked(result);
  }

  private final Problems problems = new Problems();
  private final J2clTranspilerOptions options;

  private J2clTranspiler(J2clTranspilerOptions options) {
    this.options = options;
  }

  private Problems transpileImpl() {
    try {
      CompilationUnitsAndTypeBindings jdtUnitsAndResolvedBindings =
          createJdtUnitsAndResolveBindings();
      List<CompilationUnit> j2clUnits = convertUnits(jdtUnitsAndResolvedBindings);
      checkUnits(j2clUnits);
      normalizeUnits(j2clUnits);
      generateOutputs(j2clUnits);
      return problems;
    } catch (Problems.Exit e) {
      return e.getProblems();
    } finally {
      maybeCloseFileSystem();
    }
  }

  private List<CompilationUnit> convertUnits(
      CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings) {
    // Records information about package-info files supplied as byte code.
    PackageInfoCache.init(options.getClasspaths(), problems);
    return CompilationUnitBuilder.build(compilationUnitsAndTypeBindings);
  }

  private CompilationUnitsAndTypeBindings createJdtUnitsAndResolveBindings() {
    JdtParser parser = new JdtParser(options.getClasspaths(), problems);
    CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings =
        parser.parseFiles(options.getSources(), options.getGenerateKytheIndexingMetadata());
    problems.abortIfHasErrors();
    return compilationUnitsAndTypeBindings;
  }

  private void checkUnits(List<CompilationUnit> j2clUnits) {
    JsInteropRestrictionsChecker.check(j2clUnits, problems);
    problems.abortIfHasErrors();
  }

  private void normalizeUnits(List<CompilationUnit> j2clUnits) {
    // TODO(b/117155139): Review the ordering of passes.
    List<NormalizationPass> passes =
        ImmutableList.of(
            // Class structure normalizations.
            new ImplementLambdaExpressions(),
            new OptimizeAnonymousInnerClassesToFunctionExpressions(),
            new NormalizeJsEnums(),
            // Default constructors and explicit super calls should be synthesized first.
            new CreateDefaultConstructors(),
            new InsertExplicitSuperCalls(),
            new DefaultMethodsResolver(),
            new PackagePrivateMethodsDispatcher(),
            new BridgeMethodsCreator(),
            new JsBridgeMethodsCreator(),
            new DevirtualizeBoxedTypesAndJsFunctionImplementations(),
            new NormalizeTryWithResources(),
            new NormalizeCatchClauses(),
            // Runs before normalizing nested classes.
            new InsertCastOnNewInstances(),
            // Must run before Enum normalization
            new FixSuperCallQualifiers(),

            // Runs after all passes that synthesize overlays.
            new NormalizeJsOverlayMembers(),
            new NormalizeEnumClasses(),
            new NormalizeInterfaceMethods(),
            // End of class structure normalization.

            // Statement/Expression normalizations
            new NormalizeArrayLiterals(),
            new NormalizeStaticMemberQualifiers(),
            // Runs after NormalizeStaticMemberQualifiersPass.
            new DevirtualizeMethodCalls(),
            new ControlStatementFormatter(),
            new NormalizeMultiExpressions(),
            // Runs after NormalizeMultiExpressions to make sure it only sees valid l-values.
            new ExpandCompoundAssignments(),
            new InsertErasureTypeSafetyCasts(),
            // Runs before unboxing conversion.
            new InsertStringConversions(),
            new InsertNarrowingReferenceConversions(),
            new InsertUnboxingConversions(),
            new InsertBoxingConversions(),
            new InsertNarrowingPrimitiveConversions(),
            new InsertWideningPrimitiveConversions(),
            new NormalizeLongs(),
            new InsertDivisionCoercions(),
            new InsertBitwiseOperatorBooleanCoercions(),
            new InsertUnsignedRightShiftCoercions(),
            new NormalizeJsFunctionPropertyInvocations(),
            // Run before other passes that normalize JsEnum expressions, but after all the normal
            // Java semantic conversions.
            new InsertJsEnumBoxingAndUnboxingConversions(),
            new NormalizeSwitchStatements(),
            new ArrayAccessNormalizer(),
            new ImplementAssertStatements(),
            new ImplementSynchronizedStatements(),
            new NormalizeFieldInitialization(),
            new ImplementInstanceInitialization(),
            new NormalizeNestedClassConstructors(),
            new NormalizeConstructors(),
            new NormalizeTypeLiterals(),
            new NormalizeCasts(),
            new NormalizeInstanceOfs(),
            new NormalizeEquality(),
            new NormalizeStaticNativeMemberReferences(),
            new NormalizeJsVarargs(),
            new NormalizeArrayCreations(),
            new InsertExceptionConversions(),
            new NormalizeLiterals(),

            // Needs to run after passes that do code synthesis are run so that it handles the
            // synthesize code as well.
            // TODO(b/35241823): Revisit this pass if jscompiler adds a way to express constraints
            // to template variables.
            new InsertCastsToTypeBounds(),
            new RemoveUnneededJsDocCasts(),
            new NormalizeJsDocCastExpressions(),

            // Dodge OTI limitations.
            // TODO(b/30365337): remove after JSCompiler stops requiring unnecessary abstract
            // methods on abstract classes.
            new UnimplementedMethodsCreator(),
            // TODO(b/72652198): remove the temporary fix once switch to JSCompiler's new type
            // checker.
            new InsertTypeAnnotationOnGenericReturnTypes(),

            // Perform post cleanups.
            new ImplementStaticInitialization(),
            // Normalize multiexpressions again to remove unnecessary clutter, but run before
            // variable motion.
            new NormalizeMultiExpressions(),
            new MoveVariableDeclarationsToEnclosingBlock(),

            // Handle await keyword
            new NormalizeJsAwaitMethodInvocations(),

            // Enrich source mapping information for better stack deobfuscation.
            new FilloutMissingSourceMapInformation());

    for (CompilationUnit j2clUnit : j2clUnits) {
      verifyUnit(j2clUnit);
      for (NormalizationPass pass : passes) {
        pass.applyTo(j2clUnit);
      }
      verifyUnit(j2clUnit);
    }
  }

  private void verifyUnit(CompilationUnit j2clUnit) {
    VerifySingleAstReference.applyTo(j2clUnit);
    VerifyParamAndArgCounts.applyTo(j2clUnit);
    VerifyVariableScoping.applyTo(j2clUnit);
  }

  private void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    new OutputGeneratorStage(
            options.getNativeSources(),
            options.getOutput(),
            options.getLibraryInfoOutput(),
            options.getDeclareLegacyNamespace(),
            options.getEmitReadableSourceMap(),
            options.getGenerateKytheIndexingMetadata(),
            problems)
        .generateOutputs(j2clCompilationUnits);
  }

  private void maybeCloseFileSystem() {
    FileSystem outputFileSystem = options.getOutput().getFileSystem();
    if (outputFileSystem.getClass().getCanonicalName().equals("com.sun.nio.zipfs.ZipFileSystem")
        || outputFileSystem.getClass().getCanonicalName().equals("jdk.nio.zipfs.ZipFileSystem")) {
      try {
        outputFileSystem.close();
      } catch (IOException e) {
        problems.fatal(FatalError.CANNOT_CLOSE_ZIP, e.getMessage());
      }
    }
  }
}
