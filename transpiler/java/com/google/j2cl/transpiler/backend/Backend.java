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
import com.google.j2cl.common.Problems;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.backend.closure.OutputGeneratorStage;
import com.google.j2cl.transpiler.backend.kotlin.KotlinGeneratorStage;
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
import com.google.j2cl.transpiler.passes.ImplementArraysAsClasses;
import com.google.j2cl.transpiler.passes.ImplementAssertStatements;
import com.google.j2cl.transpiler.passes.ImplementClassMetadataViaConstructors;
import com.google.j2cl.transpiler.passes.ImplementClassMetadataViaGetters;
import com.google.j2cl.transpiler.passes.ImplementDivisionOperations;
import com.google.j2cl.transpiler.passes.ImplementFloatingPointRemainderOperation;
import com.google.j2cl.transpiler.passes.ImplementInstanceInitialization;
import com.google.j2cl.transpiler.passes.ImplementInstanceOfs;
import com.google.j2cl.transpiler.passes.ImplementJsFunctionCopyMethod;
import com.google.j2cl.transpiler.passes.ImplementKotlinBitLevelOperators;
import com.google.j2cl.transpiler.passes.ImplementLambdaExpressionsViaImplementorClasses;
import com.google.j2cl.transpiler.passes.ImplementLambdaExpressionsViaJsFunctionAdaptor;
import com.google.j2cl.transpiler.passes.ImplementStaticInitializationViaClinitFunctionRedirection;
import com.google.j2cl.transpiler.passes.ImplementStaticInitializationViaConditionChecks;
import com.google.j2cl.transpiler.passes.ImplementStringCompileTimeConstants;
import com.google.j2cl.transpiler.passes.ImplementStringConcatenation;
import com.google.j2cl.transpiler.passes.ImplementSynchronizedStatements;
import com.google.j2cl.transpiler.passes.ImplementSystemGetProperty;
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
import com.google.j2cl.transpiler.passes.InsertNotNullAssertions;
import com.google.j2cl.transpiler.passes.InsertStringConversions;
import com.google.j2cl.transpiler.passes.InsertStringConversionsKotlin;
import com.google.j2cl.transpiler.passes.InsertTypeAnnotationOnGenericReturnTypes;
import com.google.j2cl.transpiler.passes.InsertUnboxingConversions;
import com.google.j2cl.transpiler.passes.InsertWideningPrimitiveConversions;
import com.google.j2cl.transpiler.passes.InsertWideningPrimitiveConversionsKotlin;
import com.google.j2cl.transpiler.passes.MoveVariableDeclarationsToEnclosingBlock;
import com.google.j2cl.transpiler.passes.NormalizationPass;
import com.google.j2cl.transpiler.passes.NormalizeArrayCreations;
import com.google.j2cl.transpiler.passes.NormalizeArrayCreationsWasm;
import com.google.j2cl.transpiler.passes.NormalizeArrayLiterals;
import com.google.j2cl.transpiler.passes.NormalizeBasicCasts;
import com.google.j2cl.transpiler.passes.NormalizeCasts;
import com.google.j2cl.transpiler.passes.NormalizeCatchClauses;
import com.google.j2cl.transpiler.passes.NormalizeConstructors;
import com.google.j2cl.transpiler.passes.NormalizeEnumClasses;
import com.google.j2cl.transpiler.passes.NormalizeEquality;
import com.google.j2cl.transpiler.passes.NormalizeFieldInitialization;
import com.google.j2cl.transpiler.passes.NormalizeFieldInitializationKotlin;
import com.google.j2cl.transpiler.passes.NormalizeForEachStatement;
import com.google.j2cl.transpiler.passes.NormalizeForStatements;
import com.google.j2cl.transpiler.passes.NormalizeFunctionExpressions;
import com.google.j2cl.transpiler.passes.NormalizeInstanceCompileTimeConstants;
import com.google.j2cl.transpiler.passes.NormalizeInstanceOfs;
import com.google.j2cl.transpiler.passes.NormalizeInstantiationThroughFactoryMethods;
import com.google.j2cl.transpiler.passes.NormalizeInterfaceMethods;
import com.google.j2cl.transpiler.passes.NormalizeJsAwaitMethodInvocations;
import com.google.j2cl.transpiler.passes.NormalizeJsDocCastExpressions;
import com.google.j2cl.transpiler.passes.NormalizeJsEnums;
import com.google.j2cl.transpiler.passes.NormalizeJsFunctionPropertyInvocations;
import com.google.j2cl.transpiler.passes.NormalizeJsVarargs;
import com.google.j2cl.transpiler.passes.NormalizeLabeledStatements;
import com.google.j2cl.transpiler.passes.NormalizeLabels;
import com.google.j2cl.transpiler.passes.NormalizeLiterals;
import com.google.j2cl.transpiler.passes.NormalizeLiteralsKotlin;
import com.google.j2cl.transpiler.passes.NormalizeLongs;
import com.google.j2cl.transpiler.passes.NormalizeMultiExpressions;
import com.google.j2cl.transpiler.passes.NormalizeNestedBlocks;
import com.google.j2cl.transpiler.passes.NormalizeNestedClassConstructors;
import com.google.j2cl.transpiler.passes.NormalizeOverlayMembers;
import com.google.j2cl.transpiler.passes.NormalizeShifts;
import com.google.j2cl.transpiler.passes.NormalizeStaticMemberQualifiers;
import com.google.j2cl.transpiler.passes.NormalizeStaticNativeMemberReferences;
import com.google.j2cl.transpiler.passes.NormalizeSwitchStatements;
import com.google.j2cl.transpiler.passes.NormalizeTryWithResources;
import com.google.j2cl.transpiler.passes.OptimizeAnonymousInnerClassesToFunctionExpressions;
import com.google.j2cl.transpiler.passes.OptimizeAutoValue;
import com.google.j2cl.transpiler.passes.PropagateConstants;
import com.google.j2cl.transpiler.passes.RemoveAssertStatements;
import com.google.j2cl.transpiler.passes.RemoveNoopStatements;
import com.google.j2cl.transpiler.passes.RemoveUnneededCasts;
import com.google.j2cl.transpiler.passes.RemoveUnneededJsDocCasts;
import com.google.j2cl.transpiler.passes.ResolveImplicitStaticQualifiers;
import com.google.j2cl.transpiler.passes.RewriteAssignmentExpressions;
import com.google.j2cl.transpiler.passes.RewriteReferenceEqualityOperations;
import com.google.j2cl.transpiler.passes.RewriteShortcutOperators;
import com.google.j2cl.transpiler.passes.RewriteUnaryExpressions;
import com.google.j2cl.transpiler.passes.StaticallyEvaluateStringConcatenation;
import com.google.j2cl.transpiler.passes.VerifyNormalizedUnits;
import com.google.j2cl.transpiler.passes.VerifyParamAndArgCounts;
import com.google.j2cl.transpiler.passes.VerifyReferenceScoping;
import com.google.j2cl.transpiler.passes.VerifySingleAstReference;
import java.util.function.Supplier;

/** Drives the backend to generate outputs. */
public enum Backend {
  CLOSURE {
    @Override
    public void generateOutputs(BackendOptions options, Library library, Problems problems) {
      new OutputGeneratorStage(
              options.getNativeSources(),
              options.getOutput(),
              options.getLibraryInfoOutput(),
              options.getEmitReadableLibraryInfo(),
              options.getEmitReadableSourceMap(),
              options.getGenerateKytheIndexingMetadata(),
              problems)
          .generateOutputs(library);
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getDesugaringPassFactories() {
      return ImmutableList.of(
          () -> new NormalizeForEachStatement(/* useDoubleForIndexVariable= */ true));
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getPassFactories(BackendOptions options) {
      // TODO(b/117155139): Review the ordering of passes.
      return ImmutableList.of(
          // Pre-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,

          // Class structure normalizations.
          () -> new OptimizeAutoValue(options.getOptimizeAutoValue()),
          ImplementLambdaExpressionsViaJsFunctionAdaptor::new,
          OptimizeAnonymousInnerClassesToFunctionExpressions::new,
          NormalizeFunctionExpressions::new,
          // Default constructors and explicit super calls should be synthesized first.
          CreateImplicitConstructors::new,
          InsertExplicitSuperCalls::new,
          BridgeMethodsCreator::new,
          EnumMethodsCreator::new,
          DevirtualizeBoxedTypesAndJsFunctionImplementations::new,
          NormalizeTryWithResources::new,
          NormalizeCatchClauses::new,
          // Runs before normalizing nested classes.
          InsertCastOnNewInstances::new,
          // Must run before Enum normalization
          FixSuperCallQualifiers::new,

          // Runs after all passes that synthesize overlays.
          NormalizeEnumClasses::new,
          NormalizeJsEnums::new,
          NormalizeOverlayMembers::new,
          NormalizeInterfaceMethods::new,
          // End of class structure normalization.

          // Statement/Expression normalizations
          NormalizeArrayLiterals::new,
          NormalizeShifts::new,
          NormalizeStaticMemberQualifiers::new,
          // Runs after NormalizeStaticMemberQualifiersPass.
          DevirtualizeMethodCalls::new,
          ControlStatementFormatter::new,
          NormalizeMultiExpressions::new,
          // Runs after NormalizeMultiExpressions to make sure it only sees valid l-values.
          ExpandCompoundAssignments::new,
          InsertErasureTypeSafetyCasts::new,
          // Runs before unboxing conversion.
          InsertStringConversions::new,
          InsertNarrowingReferenceConversions::new,
          InsertUnboxingConversions::new,
          InsertBoxingConversions::new,
          InsertNarrowingPrimitiveConversions::new,
          InsertWideningPrimitiveConversions::new,
          NormalizeLongs::new,
          InsertIntegerCoercions::new,
          InsertBitwiseOperatorBooleanCoercions::new,
          NormalizeJsFunctionPropertyInvocations::new,
          // Run before other passes that normalize JsEnum expressions, but after all the normal
          // Java semantic conversions.
          InsertJsEnumBoxingAndUnboxingConversions::new,
          RemoveUnneededCasts::new,
          NormalizeSwitchStatements::new,
          ArrayAccessNormalizer::new,
          ImplementAssertStatements::new,
          ImplementSynchronizedStatements::new,
          NormalizeFieldInitialization::new,
          ImplementInstanceInitialization::new,
          NormalizeNestedClassConstructors::new,
          NormalizeConstructors::new,
          NormalizeCasts::new,
          NormalizeInstanceOfs::new,
          NormalizeEquality::new,
          NormalizeStaticNativeMemberReferences::new,
          NormalizeJsVarargs::new,
          NormalizeArrayCreations::new,
          InsertExceptionConversions::new,
          NormalizeLiterals::new,

          // Needs to run after passes that do code synthesis are run so that it handles the
          // synthesize code as well.
          // TODO(b/35241823): Revisit this pass if jscompiler adds a way to express constraints
          // to template variables.
          InsertCastsToTypeBounds::new,

          // TODO(b/72652198): remove the temporary fix once switch to JSCompiler's type
          // checker.
          InsertTypeAnnotationOnGenericReturnTypes::new,

          // Perform post cleanups.
          ImplementStaticInitializationViaClinitFunctionRedirection::new,
          // Needs to run after ImplementStaticInitialization since ImplementIsInstanceMethods
          // creates static methods which should not call $clinit.
          ImplementInstanceOfs::new,
          ImplementJsFunctionCopyMethod::new,
          ImplementClassMetadataViaConstructors::new,
          // Normalize multiexpressions again to remove unnecessary clutter, but run before
          // variable motion.
          NormalizeMultiExpressions::new,
          MoveVariableDeclarationsToEnclosingBlock::new,
          RemoveUnneededJsDocCasts::new,
          NormalizeJsDocCastExpressions::new,
          NormalizeJsAwaitMethodInvocations::new,
          RemoveNoopStatements::new,

          // Add qualifiers to static members after all transformations to simplify the handling
          // in the backend.
          ResolveImplicitStaticQualifiers::new,

          // Enrich source mapping information for better stack deobfuscation.
          FilloutMissingSourceMapInformation::new,

          // Post-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,
          VerifyNormalizedUnits::new);
    }
  },
  WASM {
    @Override
    public void generateOutputs(BackendOptions options, Library library, Problems problems) {
      new WasmModuleGenerator(options.getOutput(), options.getWasmEntryPoints(), problems)
          .generateOutputs(library);
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getDesugaringPassFactories() {
      return ImmutableList.of(
          () -> new NormalizeForEachStatement(/* useDoubleForIndexVariable= */ false));
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getPassFactories(BackendOptions options) {
      return ImmutableList.of(
          // Pre-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,
          ImplementLambdaExpressionsViaImplementorClasses::new,

          // Default constructors and explicit super calls should be synthesized first.
          CreateImplicitConstructors::new,
          InsertExplicitSuperCalls::new,
          BridgeMethodsCreator::new,
          EnumMethodsCreator::new,
          () -> new ImplementSystemGetProperty(options.getDefinesForWasm()),

          // Must run before Enum normalization
          FixSuperCallQualifiers::new,
          NormalizeTryWithResources::new,
          NormalizeCatchClauses::new,
          NormalizeInstanceCompileTimeConstants::new,
          () -> new NormalizeEnumClasses(/* useMakeEnumNameIndirection= */ false),
          () -> new NormalizeShifts(/* narrowAllToInt= */ false),
          NormalizeStaticMemberQualifiers::new,
          NormalizeMultiExpressions::new,

          // Rewrite operations that do not have direct support in wasm into ones that have.
          () -> new ExpandCompoundAssignments(/* expandAll= */ true),
          InsertErasureTypeSafetyCasts::new,
          // Rewrite 'a != b' to '!(a == b)'
          RewriteReferenceEqualityOperations::new,
          RewriteUnaryExpressions::new,
          NormalizeSwitchStatements::new,
          // Propagate constants needs to run after NormalizeSwitchStatements since it introduces
          // field references to constant fields.
          PropagateConstants::new,
          StaticallyEvaluateStringConcatenation::new,
          ImplementStringConcatenation::new,
          InsertNarrowingReferenceConversions::new,
          () -> new InsertUnboxingConversions(/* areBooleanAndDoubleBoxed */ true),
          () -> new InsertBoxingConversions(/* areBooleanAndDoubleBoxed */ true),
          () -> new InsertNarrowingPrimitiveConversions(/* treatFloatAsDouble */ false),
          () -> new InsertWideningPrimitiveConversions(/* needFloatOrDoubleWidening */ true),
          ImplementDivisionOperations::new,
          ImplementFloatingPointRemainderOperation::new,
          // Rewrite 'a || b' into 'a ? true : b' and 'a && b' into 'a ? b : false'
          RewriteShortcutOperators::new,
          NormalizeFieldInitialization::new,
          ImplementInstanceInitialization::new,
          NormalizeNestedClassConstructors::new,
          NormalizeLabels::new,
          ImplementStaticInitializationViaConditionChecks::new,
          ImplementClassMetadataViaGetters::new,
          ImplementStringCompileTimeConstants::new,
          NormalizeArrayCreationsWasm::new,
          InsertCastOnArrayAccess::new,
          ExtractNonIdempotentExpressions::new,
          options.getWasmRemoveAssertStatement()
              ? RemoveAssertStatements::new
              : ImplementAssertStatements::new,

          // Normalize multiexpressions before rewriting assignments so that whenever there is a
          // multiexpression, the result is used.
          NormalizeMultiExpressions::new,

          // a = b => (a = b, a)
          RewriteAssignmentExpressions::new,
          // Needs to run at the end as the types in the ast will be invalid after the pass.
          ImplementArraysAsClasses::new,
          NormalizeInstantiationThroughFactoryMethods::new,

          // Post-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,
          () -> new VerifyNormalizedUnits(/* verifyForWasm= **/ true));
    }
  },
  KOTLIN {
    @Override
    public void generateOutputs(BackendOptions options, Library library, Problems problems) {
      new KotlinGeneratorStage(options.getOutput(), problems).generateOutputs(library);
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getDesugaringPassFactories() {
      return ImmutableList.of();
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getPassFactories(BackendOptions options) {
      return ImmutableList.of(
          // Pre-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,

          // Normalizations
          CreateImplicitConstructors::new,
          InsertExplicitSuperCalls::new,
          NormalizeStaticMemberQualifiers::new,
          NormalizeMultiExpressions::new,
          () -> new ExpandCompoundAssignments(/* expandAll= */ true),
          RewriteAssignmentExpressions::new,
          NormalizeLiteralsKotlin::new,
          NormalizeFieldInitializationKotlin::new,
          NormalizeNestedClassConstructors::new,
          NormalizeLabels::new,
          NormalizeForStatements::new,
          NormalizeLabeledStatements::new,
          NormalizeNestedBlocks::new,
          () -> new NormalizeShifts(/* narrowAllToInt= */ true),
          InsertWideningPrimitiveConversionsKotlin::new,
          NormalizeBasicCasts::new,
          ImplementKotlinBitLevelOperators::new,
          InsertNotNullAssertions::new,

          // Needs to run after non-null assertions are inserted.
          InsertStringConversionsKotlin::new,

          // Verification
          VerifySingleAstReference::new,
          VerifyReferenceScoping::new);
    }
  };

  public abstract ImmutableList<Supplier<NormalizationPass>> getDesugaringPassFactories();

  public abstract ImmutableList<Supplier<NormalizationPass>> getPassFactories(
      BackendOptions options);

  public abstract void generateOutputs(BackendOptions options, Library library, Problems problems);
}
