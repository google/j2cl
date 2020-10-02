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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.Problems.FatalError;
import com.google.j2cl.generator.OutputGeneratorStage;
import com.google.j2cl.transpiler.passes.ArrayAccessNormalizer;
import com.google.j2cl.transpiler.passes.BridgeMethodsCreator;
import com.google.j2cl.transpiler.passes.ControlStatementFormatter;
import com.google.j2cl.transpiler.passes.CreateImplicitConstructors;
import com.google.j2cl.transpiler.passes.DevirtualizeBoxedTypesAndJsFunctionImplementations;
import com.google.j2cl.transpiler.passes.DevirtualizeMethodCalls;
import com.google.j2cl.transpiler.passes.EnumMethodsCreator;
import com.google.j2cl.transpiler.passes.ExpandCompoundAssignments;
import com.google.j2cl.transpiler.passes.FilloutMissingSourceMapInformation;
import com.google.j2cl.transpiler.passes.FixSuperCallQualifiers;
import com.google.j2cl.transpiler.passes.ImplementAssertStatements;
import com.google.j2cl.transpiler.passes.ImplementClassMetadata;
import com.google.j2cl.transpiler.passes.ImplementInstanceInitialization;
import com.google.j2cl.transpiler.passes.ImplementInstanceOfs;
import com.google.j2cl.transpiler.passes.ImplementJsFunctionCopyMethod;
import com.google.j2cl.transpiler.passes.ImplementLambdaExpressions;
import com.google.j2cl.transpiler.passes.ImplementStaticInitialization;
import com.google.j2cl.transpiler.passes.ImplementSynchronizedStatements;
import com.google.j2cl.transpiler.passes.InsertBitwiseOperatorBooleanCoercions;
import com.google.j2cl.transpiler.passes.InsertBoxingConversions;
import com.google.j2cl.transpiler.passes.InsertCastOnNewInstances;
import com.google.j2cl.transpiler.passes.InsertCastsToTypeBounds;
import com.google.j2cl.transpiler.passes.InsertErasureTypeSafetyCasts;
import com.google.j2cl.transpiler.passes.InsertExceptionConversions;
import com.google.j2cl.transpiler.passes.InsertExplicitSuperCalls;
import com.google.j2cl.transpiler.passes.InsertIntegerCoercions;
import com.google.j2cl.transpiler.passes.InsertJsEnumBoxingAndUnboxingConversions;
import com.google.j2cl.transpiler.passes.InsertNarrowingPrimitiveConversions;
import com.google.j2cl.transpiler.passes.InsertNarrowingReferenceConversions;
import com.google.j2cl.transpiler.passes.InsertStringConversions;
import com.google.j2cl.transpiler.passes.InsertTypeAnnotationOnGenericReturnTypes;
import com.google.j2cl.transpiler.passes.InsertUnboxingConversions;
import com.google.j2cl.transpiler.passes.InsertWideningPrimitiveConversions;
import com.google.j2cl.transpiler.passes.JsInteropRestrictionsChecker;
import com.google.j2cl.transpiler.passes.MoveVariableDeclarationsToEnclosingBlock;
import com.google.j2cl.transpiler.passes.NormalizationPass;
import com.google.j2cl.transpiler.passes.NormalizeArrayCreations;
import com.google.j2cl.transpiler.passes.NormalizeArrayLiterals;
import com.google.j2cl.transpiler.passes.NormalizeCasts;
import com.google.j2cl.transpiler.passes.NormalizeCatchClauses;
import com.google.j2cl.transpiler.passes.NormalizeConstructors;
import com.google.j2cl.transpiler.passes.NormalizeEnumClasses;
import com.google.j2cl.transpiler.passes.NormalizeEquality;
import com.google.j2cl.transpiler.passes.NormalizeFieldInitialization;
import com.google.j2cl.transpiler.passes.NormalizeFunctionExpressions;
import com.google.j2cl.transpiler.passes.NormalizeInstanceOfs;
import com.google.j2cl.transpiler.passes.NormalizeInterfaceMethods;
import com.google.j2cl.transpiler.passes.NormalizeJsAwaitMethodInvocations;
import com.google.j2cl.transpiler.passes.NormalizeJsDocCastExpressions;
import com.google.j2cl.transpiler.passes.NormalizeJsEnums;
import com.google.j2cl.transpiler.passes.NormalizeJsFunctionPropertyInvocations;
import com.google.j2cl.transpiler.passes.NormalizeJsVarargs;
import com.google.j2cl.transpiler.passes.NormalizeLiterals;
import com.google.j2cl.transpiler.passes.NormalizeLongs;
import com.google.j2cl.transpiler.passes.NormalizeMultiExpressions;
import com.google.j2cl.transpiler.passes.NormalizeNestedClassConstructors;
import com.google.j2cl.transpiler.passes.NormalizeOverlayMembers;
import com.google.j2cl.transpiler.passes.NormalizeStaticMemberQualifiers;
import com.google.j2cl.transpiler.passes.NormalizeStaticNativeMemberReferences;
import com.google.j2cl.transpiler.passes.NormalizeSwitchStatements;
import com.google.j2cl.transpiler.passes.NormalizeTryWithResources;
import com.google.j2cl.transpiler.passes.NormalizeTypeLiterals;
import com.google.j2cl.transpiler.passes.OptimizeAnonymousInnerClassesToFunctionExpressions;
import com.google.j2cl.transpiler.passes.OptimizeAutoValue;
import com.google.j2cl.transpiler.passes.RemoveNoopStatements;
import com.google.j2cl.transpiler.passes.RemoveUnneededJsDocCasts;
import com.google.j2cl.transpiler.passes.RewriteStringEquals;
import com.google.j2cl.transpiler.passes.VerifyNormalizedUnits;
import com.google.j2cl.transpiler.passes.VerifyParamAndArgCounts;
import com.google.j2cl.transpiler.passes.VerifySingleAstReference;
import com.google.j2cl.transpiler.passes.VerifyVariableScoping;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
    // normally observed since the transpiler in normal circumstances ends with System.exit() which
    // ends all threads. But when the transpilation throws an exception, the exception propagates
    // out of main() and the process lingers due the live threads from these executors.
    executorService.shutdown();

    try {
      return Uninterruptibles.getUninterruptibly(result);
    } catch (ExecutionException e) {
      // Try unwrapping the cause...
      Throwables.throwIfUnchecked(e.getCause());
      throw new AssertionError(e.getCause());
    }
  }

  private final Problems problems = new Problems();
  private final J2clTranspilerOptions options;

  private J2clTranspiler(J2clTranspilerOptions options) {
    this.options = options;
  }

  private Problems transpileImpl() {
    try {
      List<CompilationUnit> j2clUnits =
          options
              .getFrontend()
              .getCompilationUnits(
                  options.getClasspaths(),
                  options.getSources(),
                  options.getGenerateKytheIndexingMetadata(),
                  problems);
      if (!j2clUnits.isEmpty()) {
        checkUnits(j2clUnits);
        normalizeUnits(j2clUnits);
      }
      generateOutputs(j2clUnits);
      return problems;
    } catch (Problems.Exit e) {
      return e.getProblems();
    } finally {
      maybeCloseFileSystem();
    }
  }

  private void checkUnits(List<CompilationUnit> j2clUnits) {
    JsInteropRestrictionsChecker.check(j2clUnits, problems);
    problems.abortIfHasErrors();
  }

  private void normalizeUnits(List<CompilationUnit> j2clUnits) {
    for (CompilationUnit j2clUnit : j2clUnits) {
      for (NormalizationPass pass : getPasses()) {
        pass.execute(j2clUnit);
      }
    }
  }

  private ImmutableList<NormalizationPass> getPasses() {
    // TODO(b/117155139): Review the ordering of passes.
    return ImmutableList.of(
        // Pre-verifications
        new VerifySingleAstReference(),
        new VerifyParamAndArgCounts(),
        new VerifyVariableScoping(),

        // Class structure normalizations.
        new OptimizeAutoValue(options.getExperimentalOptimizeAutovalue()),
        new ImplementLambdaExpressions(),
        new OptimizeAnonymousInnerClassesToFunctionExpressions(),
        new NormalizeFunctionExpressions(),
        // Default constructors and explicit super calls should be synthesized first.
        new CreateImplicitConstructors(),
        new InsertExplicitSuperCalls(),
        new BridgeMethodsCreator(),
        new EnumMethodsCreator(),
        // TODO(b/31865368): Remove RewriteStringEquals pass once delayed field initialization
        //  is introduced and String.java gets updated to use it.
        new RewriteStringEquals(),
        new DevirtualizeBoxedTypesAndJsFunctionImplementations(),
        new NormalizeTryWithResources(),
        new NormalizeCatchClauses(),
        // Runs before normalizing nested classes.
        new InsertCastOnNewInstances(),
        // Must run before Enum normalization
        new FixSuperCallQualifiers(),

        // Runs after all passes that synthesize overlays.
        new NormalizeEnumClasses(),
        new NormalizeJsEnums(),
        new NormalizeOverlayMembers(),
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
        new InsertIntegerCoercions(),
        new InsertBitwiseOperatorBooleanCoercions(),
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

        // TODO(b/72652198): remove the temporary fix once switch to JSCompiler's new type
        // checker.
        new InsertTypeAnnotationOnGenericReturnTypes(),

        // Perform post cleanups.
        new ImplementStaticInitialization(),
        // Needs to run after ImplementStaticInitialization since ImplementIsInstanceMethods creates
        // static methods which should not call $clinit.
        new ImplementInstanceOfs(),
        new ImplementJsFunctionCopyMethod(),
        new ImplementClassMetadata(),
        // Normalize multiexpressions again to remove unnecessary clutter, but run before
        // variable motion.
        new NormalizeMultiExpressions(),
        new MoveVariableDeclarationsToEnclosingBlock(),
        // Remove redundant JsDocCasts.
        new RemoveUnneededJsDocCasts(),
        new NormalizeJsDocCastExpressions(),

        // Handle await keyword.
        new NormalizeJsAwaitMethodInvocations(),
        new RemoveNoopStatements(),

        // Enrich source mapping information for better stack deobfuscation.
        new FilloutMissingSourceMapInformation(),

        // Post-verifications
        new VerifySingleAstReference(),
        new VerifyParamAndArgCounts(),
        new VerifyVariableScoping(),
        new VerifyNormalizedUnits());
  }

  private void generateOutputs(List<CompilationUnit> j2clCompilationUnits) {
    new OutputGeneratorStage(
            options.getNativeSources(),
            options.getOutput(),
            options.getLibraryInfoOutput(),
            options.getEmitReadableLibraryInfo(),
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
