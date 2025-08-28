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
import com.google.j2cl.transpiler.backend.wasm.WasmGeneratorStage;
import com.google.j2cl.transpiler.passes.AddAbstractLambdaAdaptorClasses;
import com.google.j2cl.transpiler.passes.AddAbstractMethodStubs;
import com.google.j2cl.transpiler.passes.AddBridgeMethods;
import com.google.j2cl.transpiler.passes.AddDisambiguatingOverloadResolutionCastsJ2kt;
import com.google.j2cl.transpiler.passes.AddDisambiguatingSuperMethodForwardingStubs;
import com.google.j2cl.transpiler.passes.AddEntryPointBridgesWasm;
import com.google.j2cl.transpiler.passes.AddEnumImplicitMethods;
import com.google.j2cl.transpiler.passes.AddInterfaceConstructorCasts;
import com.google.j2cl.transpiler.passes.AddJavaLangObjectForwardingMethods;
import com.google.j2cl.transpiler.passes.AddNothingReturnStatements;
import com.google.j2cl.transpiler.passes.AddSwitchExpressionsExhaustivenessCheck;
import com.google.j2cl.transpiler.passes.AddVisibilityMethodBridgesJ2kt;
import com.google.j2cl.transpiler.passes.AnnotateProtobufMethodsAsKtProperties;
import com.google.j2cl.transpiler.passes.ConvertLocalFunctionDeclarationToFunctionExpressions;
import com.google.j2cl.transpiler.passes.ConvertMethodReferencesToLambdas;
import com.google.j2cl.transpiler.passes.CreateImplicitConstructors;
import com.google.j2cl.transpiler.passes.DesugarInstanceOfPatterns;
import com.google.j2cl.transpiler.passes.DevirtualizeBoxedTypesAndJsFunctionImplementations;
import com.google.j2cl.transpiler.passes.DevirtualizeMethodCalls;
import com.google.j2cl.transpiler.passes.ExpandCompoundAssignments;
import com.google.j2cl.transpiler.passes.ExtractNonIdempotentExpressions;
import com.google.j2cl.transpiler.passes.FilloutMissingSourceMapInformation;
import com.google.j2cl.transpiler.passes.FixJavaKotlinCollectionMethodsMismatch;
import com.google.j2cl.transpiler.passes.FixJavaKotlinMethodOverrideMismatch;
import com.google.j2cl.transpiler.passes.ImplementArraysAsClasses;
import com.google.j2cl.transpiler.passes.ImplementAssertStatements;
import com.google.j2cl.transpiler.passes.ImplementBitLevelOperatorsJ2kt;
import com.google.j2cl.transpiler.passes.ImplementClassMetadataViaConstructors;
import com.google.j2cl.transpiler.passes.ImplementClassMetadataViaGetters;
import com.google.j2cl.transpiler.passes.ImplementDivisionOperations;
import com.google.j2cl.transpiler.passes.ImplementFinallyViaControlFlow;
import com.google.j2cl.transpiler.passes.ImplementFloatingPointRemainderOperation;
import com.google.j2cl.transpiler.passes.ImplementInstanceInitialization;
import com.google.j2cl.transpiler.passes.ImplementInstanceOfs;
import com.google.j2cl.transpiler.passes.ImplementJsFunctionCopyMethod;
import com.google.j2cl.transpiler.passes.ImplementLambdaExpressionsViaImplementorClasses;
import com.google.j2cl.transpiler.passes.ImplementLambdaExpressionsViaJsFunctionAdaptor;
import com.google.j2cl.transpiler.passes.ImplementNotNullOperator;
import com.google.j2cl.transpiler.passes.ImplementStaticInitializationViaClinitFunctionRedirection;
import com.google.j2cl.transpiler.passes.ImplementStaticInitializationViaConditionChecks;
import com.google.j2cl.transpiler.passes.ImplementStringCompileTimeConstants;
import com.google.j2cl.transpiler.passes.ImplementStringConcatenation;
import com.google.j2cl.transpiler.passes.ImplementSwitchExpressionsViaIifes;
import com.google.j2cl.transpiler.passes.ImplementSynchronizedStatements;
import com.google.j2cl.transpiler.passes.ImplementSystemGetProperty;
import com.google.j2cl.transpiler.passes.InsertBitwiseOperatorBooleanCoercions;
import com.google.j2cl.transpiler.passes.InsertBoxingConversions;
import com.google.j2cl.transpiler.passes.InsertCastForLowerBounds;
import com.google.j2cl.transpiler.passes.InsertCastOnArrayAccess;
import com.google.j2cl.transpiler.passes.InsertCastOnNewInstances;
import com.google.j2cl.transpiler.passes.InsertCastsForBoxedTypes;
import com.google.j2cl.transpiler.passes.InsertCastsForTypeLiteralsJ2kt;
import com.google.j2cl.transpiler.passes.InsertCastsOnNullabilityMismatch;
import com.google.j2cl.transpiler.passes.InsertErasureTypeSafetyCasts;
import com.google.j2cl.transpiler.passes.InsertExceptionConversions;
import com.google.j2cl.transpiler.passes.InsertExplicitArrayCoercionCasts;
import com.google.j2cl.transpiler.passes.InsertExplicitSuperCalls;
import com.google.j2cl.transpiler.passes.InsertExternConversionsWasm;
import com.google.j2cl.transpiler.passes.InsertIntegerCoercions;
import com.google.j2cl.transpiler.passes.InsertJsDocCastsToTypeBounds;
import com.google.j2cl.transpiler.passes.InsertJsEnumBoxingAndUnboxingConversions;
import com.google.j2cl.transpiler.passes.InsertJsFunctionImplementationConversionCasts;
import com.google.j2cl.transpiler.passes.InsertNarrowingPrimitiveConversions;
import com.google.j2cl.transpiler.passes.InsertNarrowingPrimitiveConversionsJ2kt;
import com.google.j2cl.transpiler.passes.InsertNarrowingReferenceConversions;
import com.google.j2cl.transpiler.passes.InsertNotNullAssertionToPolyNullMethodCalls;
import com.google.j2cl.transpiler.passes.InsertNotNullAssertionsOnNullabilityMismatch;
import com.google.j2cl.transpiler.passes.InsertNumericCoercionsForAutoboxing;
import com.google.j2cl.transpiler.passes.InsertQualifierProjectionCasts;
import com.google.j2cl.transpiler.passes.InsertRawTypeCasts;
import com.google.j2cl.transpiler.passes.InsertStringConversions;
import com.google.j2cl.transpiler.passes.InsertStringConversionsJ2kt;
import com.google.j2cl.transpiler.passes.InsertTypeAnnotationOnGenericReturnTypes;
import com.google.j2cl.transpiler.passes.InsertUnboxingConversions;
import com.google.j2cl.transpiler.passes.InsertUnreachableAssertionErrors;
import com.google.j2cl.transpiler.passes.InsertWideningPrimitiveConversions;
import com.google.j2cl.transpiler.passes.InsertWideningPrimitiveConversionsJ2kt;
import com.google.j2cl.transpiler.passes.J2ktRestrictionsChecker;
import com.google.j2cl.transpiler.passes.JsInteropRestrictionsChecker;
import com.google.j2cl.transpiler.passes.MakeVariablesFinal;
import com.google.j2cl.transpiler.passes.MakeVariablesNonNull;
import com.google.j2cl.transpiler.passes.MarkNoFallthroughSwitchCases;
import com.google.j2cl.transpiler.passes.MoveNestedClassesToTop;
import com.google.j2cl.transpiler.passes.NormalizationPass;
import com.google.j2cl.transpiler.passes.NormalizeArrayAccesses;
import com.google.j2cl.transpiler.passes.NormalizeArrayCreations;
import com.google.j2cl.transpiler.passes.NormalizeArrayCreationsJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeArrayCreationsWasm;
import com.google.j2cl.transpiler.passes.NormalizeArrayLiterals;
import com.google.j2cl.transpiler.passes.NormalizeCasts;
import com.google.j2cl.transpiler.passes.NormalizeCatchClauses;
import com.google.j2cl.transpiler.passes.NormalizeConstructors;
import com.google.j2cl.transpiler.passes.NormalizeControlStatements;
import com.google.j2cl.transpiler.passes.NormalizeEnumClasses;
import com.google.j2cl.transpiler.passes.NormalizeEquality;
import com.google.j2cl.transpiler.passes.NormalizeFieldInitialization;
import com.google.j2cl.transpiler.passes.NormalizeFieldInitializationJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeForEachIterable;
import com.google.j2cl.transpiler.passes.NormalizeForEachStatement;
import com.google.j2cl.transpiler.passes.NormalizeForEachStatementJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeForStatements;
import com.google.j2cl.transpiler.passes.NormalizeFunctionExpressions;
import com.google.j2cl.transpiler.passes.NormalizeInstanceCompileTimeConstants;
import com.google.j2cl.transpiler.passes.NormalizeInstanceOfs;
import com.google.j2cl.transpiler.passes.NormalizeInstantiationThroughFactoryMethods;
import com.google.j2cl.transpiler.passes.NormalizeInterfaceMethods;
import com.google.j2cl.transpiler.passes.NormalizeInterfaces;
import com.google.j2cl.transpiler.passes.NormalizeJsAwaitMethodInvocations;
import com.google.j2cl.transpiler.passes.NormalizeJsDocCastExpressions;
import com.google.j2cl.transpiler.passes.NormalizeJsEnums;
import com.google.j2cl.transpiler.passes.NormalizeJsFunctionPropertyInvocations;
import com.google.j2cl.transpiler.passes.NormalizeJsVarargs;
import com.google.j2cl.transpiler.passes.NormalizeJsYieldMethodInvocations;
import com.google.j2cl.transpiler.passes.NormalizeLabeledStatements;
import com.google.j2cl.transpiler.passes.NormalizeLabels;
import com.google.j2cl.transpiler.passes.NormalizeLambdaExpressionsJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeLiterals;
import com.google.j2cl.transpiler.passes.NormalizeLongs;
import com.google.j2cl.transpiler.passes.NormalizeMethodParametersJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeMinValueIntegralLiterals;
import com.google.j2cl.transpiler.passes.NormalizeMultiExpressions;
import com.google.j2cl.transpiler.passes.NormalizeNativePropertyAccesses;
import com.google.j2cl.transpiler.passes.NormalizeNullLiterals;
import com.google.j2cl.transpiler.passes.NormalizeNumberLiterals;
import com.google.j2cl.transpiler.passes.NormalizeOverlayMembers;
import com.google.j2cl.transpiler.passes.NormalizePackagedJsEnumVarargsLiterals;
import com.google.j2cl.transpiler.passes.NormalizePrimitiveCastsJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeShifts;
import com.google.j2cl.transpiler.passes.NormalizeStaticMemberQualifiers;
import com.google.j2cl.transpiler.passes.NormalizeStaticNativeMemberReferences;
import com.google.j2cl.transpiler.passes.NormalizeSuperMemberReferences;
import com.google.j2cl.transpiler.passes.NormalizeSuspendFunctionCalls;
import com.google.j2cl.transpiler.passes.NormalizeSwitchConstructs;
import com.google.j2cl.transpiler.passes.NormalizeSwitchConstructsJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeSynchronizedConstructs;
import com.google.j2cl.transpiler.passes.NormalizeSystemGetPropertyCalls;
import com.google.j2cl.transpiler.passes.NormalizeTryWithResources;
import com.google.j2cl.transpiler.passes.NormalizeVarargInvocationsJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeVarargParametersJ2kt;
import com.google.j2cl.transpiler.passes.NormalizeVariableInitialization;
import com.google.j2cl.transpiler.passes.OptimizeAnonymousInnerClassesToFunctionExpressions;
import com.google.j2cl.transpiler.passes.OptimizeAutoValue;
import com.google.j2cl.transpiler.passes.OptimizeEnums;
import com.google.j2cl.transpiler.passes.OptimizeImplicitConstructors;
import com.google.j2cl.transpiler.passes.OptimizeImplicitSuperCalls;
import com.google.j2cl.transpiler.passes.OptimizeKotlinCompanions;
import com.google.j2cl.transpiler.passes.OptimizeXplatForEach;
import com.google.j2cl.transpiler.passes.OptimizeXplatLogger;
import com.google.j2cl.transpiler.passes.PreventSmartCasts;
import com.google.j2cl.transpiler.passes.ProjectCapturesInLambdaParameters;
import com.google.j2cl.transpiler.passes.PropagateCompileTimeConstants;
import com.google.j2cl.transpiler.passes.PropagateConstants;
import com.google.j2cl.transpiler.passes.PropagateNullability;
import com.google.j2cl.transpiler.passes.PropagateNullabilityInOverrides;
import com.google.j2cl.transpiler.passes.RecoverShortcutBooleanOperator;
import com.google.j2cl.transpiler.passes.RemoveCustomIsInstanceMethods;
import com.google.j2cl.transpiler.passes.RemoveEmptyFallthroughSwitchCases;
import com.google.j2cl.transpiler.passes.RemoveNameFromJsEnums;
import com.google.j2cl.transpiler.passes.RemoveNativeTypes;
import com.google.j2cl.transpiler.passes.RemoveNestedBlocks;
import com.google.j2cl.transpiler.passes.RemoveNonreferencedNativeMethods;
import com.google.j2cl.transpiler.passes.RemoveNoopStatements;
import com.google.j2cl.transpiler.passes.RemoveUnnecessaryLabels;
import com.google.j2cl.transpiler.passes.RemoveUnneededCasts;
import com.google.j2cl.transpiler.passes.RemoveUnneededCastsJ2kt;
import com.google.j2cl.transpiler.passes.RemoveUnneededJsDocCasts;
import com.google.j2cl.transpiler.passes.RemoveUnneededNotNullChecks;
import com.google.j2cl.transpiler.passes.RemoveUnreachableCode;
import com.google.j2cl.transpiler.passes.RemoveWasmAnnotatedMethodBodies;
import com.google.j2cl.transpiler.passes.ResolveCaptures;
import com.google.j2cl.transpiler.passes.ResolveImplicitInstanceQualifiers;
import com.google.j2cl.transpiler.passes.ResolveImplicitStaticQualifiers;
import com.google.j2cl.transpiler.passes.RewriteAnnotationTypesJ2kt;
import com.google.j2cl.transpiler.passes.RewriteAssignmentExpressions;
import com.google.j2cl.transpiler.passes.RewriteReferenceEqualityOperations;
import com.google.j2cl.transpiler.passes.RewriteShortcutOperators;
import com.google.j2cl.transpiler.passes.RewriteUnaryExpressions;
import com.google.j2cl.transpiler.passes.StaticallyEvaluateStringComparison;
import com.google.j2cl.transpiler.passes.StaticallyEvaluateStringConcatenation;
import com.google.j2cl.transpiler.passes.VariableDeclarationHoister;
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
          // Early run of determining whether variables are effectively final so that passes that
          // depend on Expression.isEffectivelyInvariant it can take advantage.
          MakeVariablesFinal::new,
          DesugarInstanceOfPatterns::new,
          NormalizeSuspendFunctionCalls::new,
          ConvertMethodReferencesToLambdas::new,
          NormalizePackagedJsEnumVarargsLiterals::new,
          ResolveImplicitInstanceQualifiers::new,
          // Must be run before NormalizeForEachStatement.
          OptimizeXplatForEach::new,
          NormalizeForEachIterable::new,
          // Must run after NormalizeForEachIterable.
          () -> new NormalizeForEachStatement(/* useDoubleForIndexVariable= */ true),
          NormalizeSuperMemberReferences::new,
          RecoverShortcutBooleanOperator::new,
          OptimizeXplatLogger::new);
    }

    @Override
    public void checkRestrictions(BackendOptions options, Library library, Problems problems) {
      JsInteropRestrictionsChecker.check(
          library,
          problems,
          /* checkWasmRestrictions= */ false,
          /* isNullMarkedSupported= */ options.isNullMarkedSupported(),
          /* optimizeAutoValue= */ options.getOptimizeAutoValue());
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getPassFactories(BackendOptions options) {
      // TODO(b/117155139): Review the ordering of passes.
      return ImmutableList.of(
          // Pre-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,

          // Passes that change the class hierarchy or nesting structure (and passes needed for
          // those).
          OptimizeAnonymousInnerClassesToFunctionExpressions::new,
          ImplementLambdaExpressionsViaJsFunctionAdaptor::new,
          NormalizeFunctionExpressions::new,
          ConvertLocalFunctionDeclarationToFunctionExpressions::new,
          // Compute bridge methods before optimizing autovalue, since inlining the autovalue
          // classes requires inlining the bridges as well.
          AddBridgeMethods::new,
          OptimizeKotlinCompanions::new,
          () -> new OptimizeAutoValue(options.getOptimizeAutoValue()),

          // Default constructors and explicit super calls should be synthesized first.
          CreateImplicitConstructors::new,
          InsertExplicitSuperCalls::new,
          // Make sure that array literals that might have been inserted by previous passes so that
          // JsEnum varargs literals have the proper array type.
          NormalizePackagedJsEnumVarargsLiterals::new,
          // Resolve captures
          ResolveCaptures::new,
          // ... and flatten the class hierarchy.
          MoveNestedClassesToTop::new,
          OptimizeEnums::new,
          AddEnumImplicitMethods::new,
          DevirtualizeBoxedTypesAndJsFunctionImplementations::new,
          NormalizeTryWithResources::new,
          NormalizeCatchClauses::new,
          InsertCastOnNewInstances::new,

          // Runs after all passes that synthesize overlays.
          NormalizeEnumClasses::new,
          NormalizeJsEnums::new,
          NormalizeOverlayMembers::new,
          RemoveNativeTypes::new,
          NormalizeInterfaceMethods::new,
          // End of class structure normalization.

          // Statement/Expression normalizations
          NormalizeShifts::new,
          NormalizeStaticMemberQualifiers::new,
          // Runs after NormalizeStaticMemberQualifiersPass.
          DevirtualizeMethodCalls::new,
          NormalizeControlStatements::new,
          NormalizeMultiExpressions::new,
          // Runs after NormalizeMultiExpressions to make sure it only sees valid l-values.
          ExpandCompoundAssignments::new,
          InsertErasureTypeSafetyCasts::new,
          // Runs before unboxing conversion.
          InsertStringConversions::new,
          InsertNarrowingReferenceConversions::new,
          InsertUnboxingConversions::new,
          () -> new InsertBoxingConversions(/* areBooleanAndDoubleAndLongBoxed= */ false),
          InsertNarrowingPrimitiveConversions::new,
          InsertWideningPrimitiveConversions::new,
          NormalizeLongs::new,
          InsertIntegerCoercions::new,
          InsertBitwiseOperatorBooleanCoercions::new,
          NormalizeJsFunctionPropertyInvocations::new,
          // Run before other passes that normalize JsEnum expressions, but after all the normal
          // Java semantic conversions.
          InsertJsEnumBoxingAndUnboxingConversions::new,
          NormalizeArrayLiterals::new, // Needs to run after conversions and coercions.
          RemoveUnneededCasts::new,
          AddSwitchExpressionsExhaustivenessCheck::new,
          ImplementSwitchExpressionsViaIifes::new,
          NormalizeSwitchConstructs::new,
          NormalizeArrayAccesses::new,
          ImplementAssertStatements::new,
          ImplementSynchronizedStatements::new,
          NormalizeFieldInitialization::new,
          ImplementInstanceInitialization::new,
          NormalizeConstructors::new,
          NormalizeCasts::new,
          NormalizeInstanceOfs::new,
          NormalizeEquality::new,
          NormalizeStaticNativeMemberReferences::new,
          InsertJsFunctionImplementationConversionCasts::new,

          // Needs to run after passes that do code synthesis are run so that it handles the
          // synthesize code as well.
          // TODO(b/35241823): Revisit this pass if jscompiler adds a way to express constraints
          // to template variables.
          InsertJsDocCastsToTypeBounds::new,

          // NormalizeJsVarargs breaks the invariants for running ConversionContextVisitor
          // related passes.
          NormalizeJsVarargs::new,
          NormalizeArrayCreations::new,
          InsertExceptionConversions::new,
          NormalizeLiterals::new,

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
          RemoveUnneededJsDocCasts::new,
          AddInterfaceConstructorCasts::new,
          NormalizeJsDocCastExpressions::new,
          NormalizeJsAwaitMethodInvocations::new,
          NormalizeJsYieldMethodInvocations::new,
          RemoveUnneededNotNullChecks::new,
          ImplementNotNullOperator::new,

          // Needs to run after all passes that create variable declarations in multi-expressions.
          () -> new VariableDeclarationHoister(/* allowDeclarationsInExpressions= */ false),
          NormalizeLabels::new,
          RemoveUnnecessaryLabels::new,
          RemoveUnreachableCode::new,
          RemoveNoopStatements::new,

          // Add qualifiers to static members after all transformations to simplify the handling
          // in the backend.
          ResolveImplicitStaticQualifiers::new,
          AddAbstractMethodStubs::new,
          AddNothingReturnStatements::new,

          // Enrich source mapping information for better stack deobfuscation.
          FilloutMissingSourceMapInformation::new,

          // Post-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::allowOnlyStatementScopes,
          VerifyNormalizedUnits::new);
    }

    @Override
    public boolean isClosure() {
      return true;
    }
  },
  WASM {
    @Override
    public void generateOutputs(BackendOptions options, Library library, Problems problems) {
      WasmGeneratorStage.generateMonolithicOutput(
          library,
          options.getOutput(),
          options.getLibraryInfoOutput(),
          options.getSourceMappingPathPrefix(),
          options.getEnableWasmCustomDescriptors(),
          problems);
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getDesugaringPassFactories() {
      return ImmutableList.of(
          // Early run of determining whether variables are effectively final so that passes that
          // depend on Expression.isEffectivelyInvariant it can take advantage.
          // TODO(b/277799806): Consider removing this pass if the immutable field optimization is
          // removed.
          MakeVariablesFinal::new,
          DesugarInstanceOfPatterns::new,
          ConvertMethodReferencesToLambdas::new,
          NormalizePackagedJsEnumVarargsLiterals::new,
          ResolveImplicitInstanceQualifiers::new,
          NormalizeForEachIterable::new,
          // Must run after NormalizeForEachIterable.
          () -> new NormalizeForEachStatement(/* useDoubleForIndexVariable= */ false),
          NormalizeSuperMemberReferences::new,
          RemoveWasmAnnotatedMethodBodies::new);
    }

    @Override
    public void checkRestrictions(BackendOptions options, Library library, Problems problems) {
      JsInteropRestrictionsChecker.check(
          library,
          problems,
          /* checkWasmRestrictions= */ true,
          /* isNullMarkedSupported= */ options.isNullMarkedSupported(),
          /* optimizeAutoValue= */ options.getOptimizeAutoValue());
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getPassFactories(BackendOptions options) {
      return ImmutableList.of(
          // Pre-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,
          OptimizeAnonymousInnerClassesToFunctionExpressions::new,
          ImplementLambdaExpressionsViaImplementorClasses::new,
          AddAbstractLambdaAdaptorClasses::new,

          // Default constructors and explicit super calls should be synthesized first.
          CreateImplicitConstructors::new,
          InsertExplicitSuperCalls::new,
          // Make sure that array literals that might have been inserted by previous passes so that
          // JsEnum varargs literals have the proper array type.
          NormalizePackagedJsEnumVarargsLiterals::new,

          // Resolve captures
          ResolveCaptures::new,
          // ... and flatten the class hierarchy.
          MoveNestedClassesToTop::new,
          AddBridgeMethods::new,
          AddEnumImplicitMethods::new,
          NormalizeTryWithResources::new,
          NormalizeCatchClauses::new,
          () -> new NormalizeEnumClasses(/* useMakeEnumNameIndirection= */ false),
          // Must run after NormalizeEnumClasses and before NormalizeOverlayMembers.
          RemoveNameFromJsEnums::new,
          NormalizeOverlayMembers::new,
          NormalizeInstanceCompileTimeConstants::new,
          () -> new NormalizeShifts(/* narrowAllToInt= */ false),
          NormalizeStaticMemberQualifiers::new,
          NormalizeMultiExpressions::new,
          // needs to run before ImplementSystemGetProperty
          () -> new ImplementAssertStatements(/* useWasmDebugFlag= */ true),
          () -> new ImplementSystemGetProperty(options.getDefinesForWasm()),

          // Rewrite operations that do not have direct support in wasm into ones that have.
          () -> new ExpandCompoundAssignments(/* expandAll= */ true),
          InsertErasureTypeSafetyCasts::new,
          RewriteUnaryExpressions::new,
          AddSwitchExpressionsExhaustivenessCheck::new,
          NormalizeSwitchConstructs::new,
          // Propagate constants needs to run after NormalizeSwitchStatements since it introduces
          // field references to constant fields.
          PropagateConstants::new,
          StaticallyEvaluateStringConcatenation::new,
          StaticallyEvaluateStringComparison::new,
          () -> new InsertStringConversions(/* skipPrimitivesAndNonNullableString= */ false),
          ImplementStringConcatenation::new,
          InsertNarrowingReferenceConversions::new,
          () -> new InsertUnboxingConversions(/* areBooleanAndDoubleAndLongBoxed= */ true),
          () -> new InsertBoxingConversions(/* areBooleanAndDoubleAndLongBoxed= */ true),
          () -> new InsertNarrowingPrimitiveConversions(/* treatFloatAsDouble= */ false),
          () -> new InsertWideningPrimitiveConversions(/* needFloatOrDoubleWidening= */ true),
          ImplementDivisionOperations::new,
          ImplementFloatingPointRemainderOperation::new,
          // Rewrite 'a || b' into 'a ? true : b' and 'a && b' into 'a ? b : false'
          RewriteShortcutOperators::new,
          NormalizeFieldInitialization::new,
          ImplementInstanceInitialization::new,
          NormalizeLabels::new,
          NormalizeInstantiationThroughFactoryMethods::new,
          ImplementStaticInitializationViaConditionChecks::new,
          ImplementClassMetadataViaGetters::new,
          ImplementStringCompileTimeConstants::new,
          NormalizeArrayCreationsWasm::new,
          InsertCastOnArrayAccess::new,

          // Normalize multiexpressions before rewriting assignments so that whenever there is a
          // multiexpression, the result is used.
          NormalizeMultiExpressions::new,

          // a = b => (a = b, a)
          RewriteAssignmentExpressions::new,
          // Must happen after RewriteAssignmentExpressions
          NormalizeNativePropertyAccesses::new,
          // NormalizeNativePropertyAccesses creates method calls whose qualifiers might need to be
          // extracted. After extracting qualifiers, we must again normalize multi-expressions.
          ExtractNonIdempotentExpressions::new,
          NormalizeMultiExpressions::new,
          () -> new AddEntryPointBridgesWasm(options.getWasmEntryPointPatterns()),
          ImplementFinallyViaControlFlow::new,

          // Needs to run at the end as the types in the ast will be invalid after the pass.
          ImplementArraysAsClasses::new,
          InsertExceptionConversions::new,
          InsertExternConversionsWasm::new,
          RemoveCustomIsInstanceMethods::new,
          RemoveNonreferencedNativeMethods::new,
          RemoveNoopStatements::new,

          // Passes that transform the AST to match the requirements of the Wasm instruction set.
          // Make null literals to have the type required by their use.
          NormalizeNullLiterals::new,
          // Rewrite 'a != b' to '!(a == b)'
          RewriteReferenceEqualityOperations::new,

          // Post-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,
          () -> new VerifyNormalizedUnits(/* verifyForWasm= **/ true));
    }

    @Override
    public boolean isWasm() {
      return true;
    }
  },
  WASM_MODULAR {
    @Override
    public void generateOutputs(BackendOptions options, Library library, Problems problems) {
      WasmGeneratorStage.generateModularOutput(
          library,
          options.getOutput(),
          options.getLibraryInfoOutput(),
          options.getSourceMappingPathPrefix(),
          options.getEnableWasmCustomDescriptors(),
          problems);
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getDesugaringPassFactories() {
      return ImmutableList.of(
          // Early run of determining whether variables are effectively final so that passes that
          // depend on Expression.isEffectivelyInvariant it can take advantage.
          // TODO(b/277799806): Consider removing this pass if the immutable field optimization is
          // removed.
          MakeVariablesFinal::new,
          DesugarInstanceOfPatterns::new,
          ConvertMethodReferencesToLambdas::new,
          NormalizePackagedJsEnumVarargsLiterals::new,
          ResolveImplicitInstanceQualifiers::new,
          NormalizeForEachIterable::new,
          // Must run after NormalizeForEachIterable.
          () -> new NormalizeForEachStatement(/* useDoubleForIndexVariable= */ false),
          NormalizeSuperMemberReferences::new,
          RemoveWasmAnnotatedMethodBodies::new);
    }

    @Override
    public void checkRestrictions(BackendOptions options, Library library, Problems problems) {
      JsInteropRestrictionsChecker.check(
          library,
          problems,
          /* checkWasmRestrictions= */ true,
          /* isNullMarkedSupported= */ options.isNullMarkedSupported(),
          /* optimizeAutoValue= */ options.getOptimizeAutoValue());
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getPassFactories(BackendOptions options) {
      return ImmutableList.of(
          // Pre-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,
          OptimizeAnonymousInnerClassesToFunctionExpressions::new,
          ImplementLambdaExpressionsViaImplementorClasses::new,
          AddAbstractLambdaAdaptorClasses::new,

          // Default constructors and explicit super calls should be synthesized first.
          CreateImplicitConstructors::new,
          InsertExplicitSuperCalls::new,
          // Make sure that array literals that might have been inserted by previous passes so that
          // JsEnum varargs literals have the proper array type.
          NormalizePackagedJsEnumVarargsLiterals::new,

          // Resolve captures
          ResolveCaptures::new,
          // ... and flatten the class hierarchy.
          MoveNestedClassesToTop::new,
          AddBridgeMethods::new,
          AddEnumImplicitMethods::new,
          NormalizeSystemGetPropertyCalls::new,
          NormalizeTryWithResources::new,
          NormalizeCatchClauses::new,
          () -> new NormalizeEnumClasses(/* useMakeEnumNameIndirection= */ false),
          // Must run after NormalizeEnumClasses
          RemoveNameFromJsEnums::new,
          NormalizeOverlayMembers::new,
          NormalizeInstanceCompileTimeConstants::new,
          () -> new NormalizeShifts(/* narrowAllToInt= */ false),
          NormalizeStaticMemberQualifiers::new,
          NormalizeMultiExpressions::new,

          // Rewrite operations that do not have direct support in wasm into ones that have.
          () -> new ExpandCompoundAssignments(/* expandAll= */ true),
          InsertErasureTypeSafetyCasts::new,
          RewriteUnaryExpressions::new,
          AddSwitchExpressionsExhaustivenessCheck::new,
          NormalizeSwitchConstructs::new,
          // Propagate constants needs to run after NormalizeSwitchStatements since it introduces
          // field references to constant fields.
          PropagateCompileTimeConstants::new,
          StaticallyEvaluateStringConcatenation::new,
          StaticallyEvaluateStringComparison::new,
          () -> new InsertStringConversions(/* skipPrimitivesAndNonNullableString= */ false),
          ImplementStringConcatenation::new,
          InsertNarrowingReferenceConversions::new,
          () -> new InsertUnboxingConversions(/* areBooleanAndDoubleAndLongBoxed= */ true),
          () -> new InsertBoxingConversions(/* areBooleanAndDoubleAndLongBoxed= */ true),
          () -> new InsertNarrowingPrimitiveConversions(/* treatFloatAsDouble= */ false),
          () -> new InsertWideningPrimitiveConversions(/* needFloatOrDoubleWidening= */ true),
          ImplementDivisionOperations::new,
          ImplementFloatingPointRemainderOperation::new,
          // Rewrite 'a || b' into 'a ? true : b' and 'a && b' into 'a ? b : false'
          RewriteShortcutOperators::new,
          NormalizeFieldInitialization::new,
          ImplementInstanceInitialization::new,
          NormalizeLabels::new,
          NormalizeInstantiationThroughFactoryMethods::new,
          ImplementStaticInitializationViaConditionChecks::new,
          ImplementClassMetadataViaGetters::new,
          NormalizeArrayCreationsWasm::new,
          InsertCastOnArrayAccess::new,
          () -> new ImplementAssertStatements(/* useWasmDebugFlag= */ true),

          // Normalize multiexpressions before rewriting assignments so that whenever there is a
          // multiexpression, the result is used.
          NormalizeMultiExpressions::new,

          // a = b => (a = b, a)
          RewriteAssignmentExpressions::new,
          // Must happen after RewriteAssignmentExpressions
          NormalizeNativePropertyAccesses::new,
          // NormalizeNativePropertyAccesses creates method calls whose qualifiers might need to be
          // extracted. After extracting qualifiers, we must again normalize multi-expressions.
          ExtractNonIdempotentExpressions::new,
          NormalizeMultiExpressions::new,
          ImplementFinallyViaControlFlow::new,

          // Needs to run at the end as the types in the ast will be invalid after the pass.
          ImplementArraysAsClasses::new,
          InsertExceptionConversions::new,
          InsertExternConversionsWasm::new,
          RemoveCustomIsInstanceMethods::new,
          RemoveNonreferencedNativeMethods::new,
          RemoveNoopStatements::new,

          // Passes that transform the AST to match the requirements of the Wasm instruction set.
          // Make null literals to have the type required by their use.
          NormalizeNullLiterals::new,
          // Rewrite 'a != b' to '!(a == b)'
          RewriteReferenceEqualityOperations::new,

          // Post-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,
          () -> new VerifyNormalizedUnits(/* verifyForWasm= **/ true));
    }

    @Override
    public boolean isWasm() {
      return true;
    }
  },
  KOTLIN {
    @Override
    public void generateOutputs(BackendOptions options, Library library, Problems problems) {
      new KotlinGeneratorStage(options.getOutput(), problems, options.getObjCNamePrefix())
          .generateOutputs(library);
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getDesugaringPassFactories() {
      return ImmutableList.of(
          NormalizeNullLiterals::new,
          MakeVariablesFinal::new,
          DesugarInstanceOfPatterns::new,
          MakeVariablesNonNull::new,
          ConvertMethodReferencesToLambdas::new,
          PropagateNullability::new,
          ProjectCapturesInLambdaParameters::new,
          ResolveImplicitInstanceQualifiers::new);
    }

    @Override
    public void checkRestrictions(BackendOptions options, Library library, Problems problems) {
      J2ktRestrictionsChecker.check(library, problems);
    }

    @Override
    public ImmutableList<Supplier<NormalizationPass>> getPassFactories(BackendOptions options) {
      return ImmutableList.of(
          // Pre-verifications
          VerifySingleAstReference::new,
          VerifyParamAndArgCounts::new,
          VerifyReferenceScoping::new,
          RemoveEmptyFallthroughSwitchCases::new,

          // Normalizations
          AddDisambiguatingSuperMethodForwardingStubs::new,

          // Must be run after AddDisambiguatingSuperMethodForwardingStubs
          FixJavaKotlinMethodOverrideMismatch::new,
          AnnotateProtobufMethodsAsKtProperties::new,
          RewriteAnnotationTypesJ2kt::new,
          NormalizeMinValueIntegralLiterals::new,
          CreateImplicitConstructors::new,
          InsertExplicitSuperCalls::new,
          NormalizeLambdaExpressionsJ2kt::new,
          AddJavaLangObjectForwardingMethods::new,
          AddDisambiguatingOverloadResolutionCastsJ2kt::new,
          AddVisibilityMethodBridgesJ2kt::new,
          NormalizeSynchronizedConstructs::new,
          PropagateNullabilityInOverrides::new,
          NormalizeInterfaces::new,
          NormalizeTryWithResources::new,
          NormalizeForEachIterable::new,
          // Must run after NormalizeForEachIterable and benefits from running
          // after MakeVariablesFinal.
          NormalizeForEachStatementJ2kt::new,
          NormalizeStaticMemberQualifiers::new,
          () -> new VariableDeclarationHoister(/* allowDeclarationsInExpressions= */ true),
          NormalizeMultiExpressions::new,
          () -> new ExpandCompoundAssignments(/* expandAll= */ true),
          RewriteAssignmentExpressions::new,
          () -> new InsertUnboxingConversions(/* areBooleanAndDoubleAndLongBoxed= */ true),
          InsertNumericCoercionsForAutoboxing::new,
          InsertWideningPrimitiveConversionsJ2kt::new,
          InsertNarrowingPrimitiveConversionsJ2kt::new,
          NormalizeVarargParametersJ2kt::new,
          NormalizeFieldInitializationJ2kt::new,
          NormalizeVariableInitialization::new,
          MakeVariablesFinal::new,
          // Needs to run after NormalizeVarargParametersJ2kt and MakeVariablesFinal.
          NormalizeMethodParametersJ2kt::new,
          MakeVariablesNonNull::new,
          NormalizeLabels::new,
          NormalizeForStatements::new,
          MarkNoFallthroughSwitchCases::new,
          NormalizeSwitchConstructsJ2kt::new,
          NormalizeLabeledStatements::new,
          () -> new NormalizeShifts(/* narrowAllToInt= */ true),
          NormalizeNumberLiterals::new,
          NormalizePrimitiveCastsJ2kt::new,
          ImplementBitLevelOperatorsJ2kt::new,
          InsertQualifierProjectionCasts::new,
          InsertNotNullAssertionToPolyNullMethodCalls::new,
          InsertNotNullAssertionsOnNullabilityMismatch::new,
          InsertCastsForTypeLiteralsJ2kt::new,
          InsertCastsOnNullabilityMismatch::new,
          InsertCastForLowerBounds::new,
          InsertRawTypeCasts::new,
          InsertCastsForBoxedTypes::new,

          // Needs to run after non-null assertions are inserted.
          InsertStringConversionsJ2kt::new,

          // Needs to run after NormalizeNonFinalVariablesJ2kt.
          InsertExplicitArrayCoercionCasts::new,
          NormalizeMultiExpressions::new,
          () -> new RemoveUnnecessaryLabels(/* onlyLoopsAreBreakable= */ true),
          RemoveNestedBlocks::new,
          RemoveNoopStatements::new,
          RemoveUnneededCastsJ2kt::new,

          // Passes that breaks the invariants for running ConversionContextVisitor related passes.
          NormalizeVarargInvocationsJ2kt::new,
          NormalizeArrayCreationsJ2kt::new,
          OptimizeImplicitSuperCalls::new,
          OptimizeImplicitConstructors::new,
          InsertUnreachableAssertionErrors::new,
          FixJavaKotlinCollectionMethodsMismatch::new,

          // This needs to run after all passes that can potentially add casts.
          PreventSmartCasts::new,

          // Verification
          VerifySingleAstReference::new,
          VerifyReferenceScoping::allowExpressionScopes);
    }

    @Override
    public boolean isKotlin() {
      return true;
    }
  };

  public abstract ImmutableList<Supplier<NormalizationPass>> getDesugaringPassFactories();

  public abstract ImmutableList<Supplier<NormalizationPass>> getPassFactories(
      BackendOptions options);

  public void checkRestrictions(BackendOptions options, Library library, Problems problems) {}

  public abstract void generateOutputs(BackendOptions options, Library library, Problems problems);

  public boolean isWasm() {
    return false;
  }

  public boolean isKotlin() {
    return false;
  }

  public boolean isClosure() {
    return false;
  }
}
