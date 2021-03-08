/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.transpiler.backend;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.backend.closure.OutputGeneratorStage;
import com.google.j2cl.transpiler.backend.wasm.WasmModuleGenerator;
import com.google.j2cl.transpiler.passes.ArrayAccessNormalizer;
import com.google.j2cl.transpiler.passes.BridgeMethodsCreator;
import com.google.j2cl.transpiler.passes.ControlStatementFormatter;
import com.google.j2cl.transpiler.passes.CreateImplicitConstructors;
import com.google.j2cl.transpiler.passes.DevirtualizeBoxedTypesAndJsFunctionImplementations;
import com.google.j2cl.transpiler.passes.DevirtualizeMethodCalls;
import com.google.j2cl.transpiler.passes.EnumMethodsCreator;
import com.google.j2cl.transpiler.passes.ExpandCompoundAssignments;
import com.google.j2cl.transpiler.passes.ExtractNonIdempotentExpressions;
import com.google.j2cl.transpiler.passes.FilloutMissingSourceMapInformation;
import com.google.j2cl.transpiler.passes.FixSuperCallQualifiers;
import com.google.j2cl.transpiler.passes.ImplementAssertStatements;
import com.google.j2cl.transpiler.passes.ImplementClassMetadata;
import com.google.j2cl.transpiler.passes.ImplementInstanceInitialization;
import com.google.j2cl.transpiler.passes.ImplementInstanceOfs;
import com.google.j2cl.transpiler.passes.ImplementJsFunctionCopyMethod;
import com.google.j2cl.transpiler.passes.ImplementLambdaExpressions;
import com.google.j2cl.transpiler.passes.ImplementStaticInitializationViaClinitFunctionRedirection;
import com.google.j2cl.transpiler.passes.ImplementStaticInitializationViaConditionChecks;
import com.google.j2cl.transpiler.passes.ImplementStringCompileTimeConstants;
import com.google.j2cl.transpiler.passes.ImplementSynchronizedStatements;
import com.google.j2cl.transpiler.passes.InsertBitwiseOperatorBooleanCoercions;
import com.google.j2cl.transpiler.passes.InsertBoxingConversions;
import com.google.j2cl.transpiler.passes.InsertCastOnArrayAccess;
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
import com.google.j2cl.transpiler.passes.MoveVariableDeclarationsToEnclosingBlock;
import com.google.j2cl.transpiler.passes.NormalizationPass;
import com.google.j2cl.transpiler.passes.NormalizeArrayCreations;
import com.google.j2cl.transpiler.passes.NormalizeArrayCreationsWasm;
import com.google.j2cl.transpiler.passes.NormalizeArrayLiterals;
import com.google.j2cl.transpiler.passes.NormalizeCasts;
import com.google.j2cl.transpiler.passes.NormalizeCatchClauses;
import com.google.j2cl.transpiler.passes.NormalizeConstructors;
import com.google.j2cl.transpiler.passes.NormalizeEnumClasses;
import com.google.j2cl.transpiler.passes.NormalizeEquality;
import com.google.j2cl.transpiler.passes.NormalizeFieldInitialization;
import com.google.j2cl.transpiler.passes.NormalizeForEachStatement;
import com.google.j2cl.transpiler.passes.NormalizeFunctionExpressions;
import com.google.j2cl.transpiler.passes.NormalizeInstanceCompileTimeConstants;
import com.google.j2cl.transpiler.passes.NormalizeInstanceOfs;
import com.google.j2cl.transpiler.passes.NormalizeInterfaceMethods;
import com.google.j2cl.transpiler.passes.NormalizeJsAwaitMethodInvocations;
import com.google.j2cl.transpiler.passes.NormalizeJsDocCastExpressions;
import com.google.j2cl.transpiler.passes.NormalizeJsEnums;
import com.google.j2cl.transpiler.passes.NormalizeJsFunctionPropertyInvocations;
import com.google.j2cl.transpiler.passes.NormalizeJsVarargs;
import com.google.j2cl.transpiler.passes.NormalizeLabels;
import com.google.j2cl.transpiler.passes.NormalizeLiterals;
import com.google.j2cl.transpiler.passes.NormalizeLongs;
import com.google.j2cl.transpiler.passes.NormalizeMultiExpressions;
import com.google.j2cl.transpiler.passes.NormalizeNestedClassConstructors;
import com.google.j2cl.transpiler.passes.NormalizeOverlayMembers;
import com.google.j2cl.transpiler.passes.NormalizeStaticMemberQualifiers;
import com.google.j2cl.transpiler.passes.NormalizeStaticNativeMemberReferences;
import com.google.j2cl.transpiler.passes.NormalizeStringLiterals;
import com.google.j2cl.transpiler.passes.NormalizeSwitchStatements;
import com.google.j2cl.transpiler.passes.NormalizeTryWithResources;
import com.google.j2cl.transpiler.passes.NormalizeTypeLiterals;
import com.google.j2cl.transpiler.passes.OptimizeAnonymousInnerClassesToFunctionExpressions;
import com.google.j2cl.transpiler.passes.OptimizeAutoValue;
import com.google.j2cl.transpiler.passes.RemoveNoopStatements;
import com.google.j2cl.transpiler.passes.RemoveUnneededJsDocCasts;
import com.google.j2cl.transpiler.passes.RewriteAssignmentExpressions;
import com.google.j2cl.transpiler.passes.RewriteReferenceNotEquals;
import com.google.j2cl.transpiler.passes.RewriteShortcutOperators;
import com.google.j2cl.transpiler.passes.RewriteStringEquals;
import com.google.j2cl.transpiler.passes.RewriteUnaryExpressions;
import com.google.j2cl.transpiler.passes.VerifyNormalizedUnits;
import com.google.j2cl.transpiler.passes.VerifyParamAndArgCounts;
import com.google.j2cl.transpiler.passes.VerifyReferenceScoping;
import com.google.j2cl.transpiler.passes.VerifySingleAstReference;
import java.nio.file.Path;
import java.util.List;

/** Drives the backend to generate outputs. */
public enum Backend {
  CLOSURE {
    @Override
    public void generateOutputs(
        List<CompilationUnit> j2clCompilationUnits,
        ImmutableList<FileInfo> nativeSources,
        Path output,
        Path libraryInfoOutput,
        boolean emitReadableLibraryInfo,
        boolean emitReadableSourceMap,
        boolean generateKytheIndexingMetadata,
        ImmutableSet<String> entryPoints,
        Problems problems) {

      new OutputGeneratorStage(
              nativeSources,
              output,
              libraryInfoOutput,
              emitReadableLibraryInfo,
              emitReadableSourceMap,
              generateKytheIndexingMetadata,
              problems)
          .generateOutputs(j2clCompilationUnits);
    }

    @Override
    public ImmutableList<NormalizationPass> getDesugaringPasses() {
      return ImmutableList.of(new NormalizeForEachStatement(/* useDoubleForIndexVariable= */ true));
    }

    @Override
    public ImmutableList<NormalizationPass> getPasses(boolean experimentalOptimizeAutovalue) {
      // TODO(b/117155139): Review the ordering of passes.
      return ImmutableList.of(
          // Pre-verifications
          new VerifySingleAstReference(),
          new VerifyParamAndArgCounts(),
          new VerifyReferenceScoping(),

          // Class structure normalizations.
          new OptimizeAutoValue(experimentalOptimizeAutovalue),
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
          new ImplementStaticInitializationViaClinitFunctionRedirection(),
          // Needs to run after ImplementStaticInitialization since ImplementIsInstanceMethods
          // creates static methods which should not call $clinit.
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
          new VerifyReferenceScoping(),
          new VerifyNormalizedUnits());
    }
  },
  WASM {
    @Override
    public void generateOutputs(
        List<CompilationUnit> j2clUnits,
        ImmutableList<FileInfo> nativeSources,
        Path output,
        Path libraryInfoOutput,
        boolean emitReadableLibraryInfo,
        boolean emitReadableSourceMap,
        boolean generateKytheIndexingMetadata,
        ImmutableSet<String> entryPoints,
        Problems problems) {
      new WasmModuleGenerator(output, entryPoints, problems).generateOutputs(j2clUnits);
    }

    @Override
    public ImmutableList<NormalizationPass> getDesugaringPasses() {
      return ImmutableList.of(
          new NormalizeForEachStatement(/* useDoubleForIndexVariable= */ false));
    }

    @Override
    public ImmutableList<NormalizationPass> getPasses(boolean experimentalOptimizeAutovalue) {
      return ImmutableList.of(
          // Pre-verifications
          new VerifySingleAstReference(),
          new VerifyParamAndArgCounts(),
          new VerifyReferenceScoping(),

          // Default constructors and explicit super calls should be synthesized first.
          new CreateImplicitConstructors(),
          new InsertExplicitSuperCalls(),
          new BridgeMethodsCreator(),
          new EnumMethodsCreator(),

          // Must run before Enum normalization
          new FixSuperCallQualifiers(),
          new NormalizeInstanceCompileTimeConstants(),
          new NormalizeStringLiterals(),
          new NormalizeEnumClasses(/* useMakeEnumNameIndirection= */ false),
          new NormalizeStaticMemberQualifiers(),
          new NormalizeMultiExpressions(),
          new NormalizeSwitchStatements(),

          // Rewrite operations that do not have direct support in wasm into ones that have.
          new ExpandCompoundAssignments(/* expandAll= */ true),
          new InsertErasureTypeSafetyCasts(),
          // Rewrite 'a != b' to '!(a == b)'
          new RewriteReferenceNotEquals(),
          new RewriteUnaryExpressions(),
          // Rewrite 'a || b' into 'a ? true : b' and 'a && b' into 'a ? b : false'
          new RewriteShortcutOperators(),
          new ImplementStringCompileTimeConstants(),
          new NormalizeFieldInitialization(),
          new ImplementInstanceInitialization(),
          new NormalizeNestedClassConstructors(),
          new NormalizeLabels(),
          new NormalizeArrayLiterals(),
          new NormalizeArrayCreationsWasm(),
          new InsertCastOnArrayAccess(),
          new ImplementStaticInitializationViaConditionChecks(),
          new ExtractNonIdempotentExpressions(),

          // Normalize multiexpressions before rewriting assignments so that whenever there is a
          // multiexpression, the result is used.
          new NormalizeMultiExpressions(),

          // a = b => (a = b, a)
          new RewriteAssignmentExpressions(),
          // Post-verifications
          new VerifySingleAstReference(),
          new VerifyParamAndArgCounts(),
          new VerifyReferenceScoping(),
          new VerifyNormalizedUnits(/* verifyForWasm= **/ true));
    }
  };

  public abstract ImmutableList<NormalizationPass> getDesugaringPasses();

  public abstract ImmutableList<NormalizationPass> getPasses(boolean experimentalOptimizeAutovalue);

  public abstract void generateOutputs(
      List<CompilationUnit> j2clUnits,
      ImmutableList<FileInfo> nativeSources,
      Path output,
      Path libraryInfoOutput,
      boolean emitReadableLibraryInfo,
      boolean emitReadableSourceMap,
      boolean generateKytheIndexingMetadata,
      ImmutableSet<String> entryPoints,
      Problems problems);
}
