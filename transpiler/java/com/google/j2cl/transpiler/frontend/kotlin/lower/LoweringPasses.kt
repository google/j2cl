/*
 * Copyright 2022 Google Inc.
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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.kotlin.ir.IntrinsicMethods
import com.google.j2cl.transpiler.frontend.kotlin.ir.IrProviderFromPublicSignature
import com.google.j2cl.transpiler.frontend.kotlin.ir.J2clIrDeserializer
import com.google.j2cl.transpiler.frontend.kotlin.ir.populate
import com.google.j2cl.transpiler.frontend.kotlin.lower.SmuggledJvmLoweringPasses.*
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationsRemoveLowering
import org.jetbrains.kotlin.backend.common.lower.ExpressionBodyTransformer
import org.jetbrains.kotlin.backend.common.lower.InitializersCleanupLowering
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.backend.common.lower.StripTypeAliasDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializer
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.lower.cleanup.CleanupLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.SymbolTable

/** The list of lowering passes to run in execution order. */
private val loweringPhase = loweringPhase {
  // Remove expect declarations, the actuals will be used instead.
  moduleLowering(::ExpectDeclarationsRemoveLowering)
  // The K2 frontend does not create a FileKt class  for referenced/ top-level members defined in
  // another compilation unit. Instead, it attaches them to an IrPackageFragment.
  // This lowering addresses this by creating the necessary file class and moving the top-level
  // members into it. This is needed to ensure the serialized IR for top-level inline functions
  // can be deserialized correctly. (The serialized IR being part of the metadata of the enclosing
  // class after Kotlin/JVM compilation.)
  moduleLowering(::ExternalPackageParentPatcherLowering)
  // Remove assignments to definedExternally.
  moduleLowering(::DefinedExternallyLowering)
  // Remove typealias declarations from the IR.
  perFileLowering(::StripTypeAliasDeclarationsLowering)
  // Put file level function and property declaration into a class.
  moduleLowering(fileClassLoweringFactory)
  // Invent names for local classes and anonymous objects. Later passes may require all classes
  // to have a name for computing function signature.
  perFileLowering(jvmInventNamesForLocalClassesFactory)
  // Transform all callable reference (including defaults) to inline lambdas, mark inline lambdas
  // for later passes.
  perFileLowering(::J2clInlineCallableReferenceToLambdaPhase)
  // Rewrites `Array(size) { index -> value }` using type-specific initializer lambdas.
  perFileLowering(::ArrayConstructorLowering)
  // Create nullable backing fields and insert nullability checks for lateinit properties and
  // variables. Must run before JvmPropertiesLowering.
  perFileLowering(::LateinitLowering)
  // Extract anonymous classes from inline lambdas.
  perFileLowering(::LocalClassesInInlineLambdasLowering)
  // Resolve captures for anonymous class defined in inline functions.
  perFileLowering(::LocalClassesInInlineFunctionsLowering)
  // Move the anonymous classes from inline functions into the nearest declaration container.
  perFileLowering(::LocalClassesExtractionFromInlineFunctionsLowering.asPostfix())
  // Create public bridge for private top level function called from inline functions.
  perFileLowering(::SyntheticAccessorLowering)
  // TODO(b/449125803) : remove that pass and use `UpgradeCallableReferences` instead.
  // Replace reference to inline function with reified parameter with reference to a synthetic
  // non-inline function where types parameters have been substituted so there is no reference to
  // inline functions anymore.
  // ex:
  // inline fun <reified T> castTo(param: Any): T  = param as T
  // reference like `::castTo<String>` is replaced with `::castTo$wrap` where
  // fun castTo$wrap(param: Any) String = castTo<String>(param)
  perFileLowering(::WrapInlineDeclarationsWithReifiedTypeParametersLowering)
  // Perform function inlining.
  moduleLowering(::J2clFunctionInlining)
  // Remove inline functions with reified type parameters as these functions cannot be called from
  // Java
  perFileLowering(::RemoveInlineDeclarationsWithReifiedTypeParametersLowering)
  // Replace `emptyArray()` calls with `arrayOf()` calls.
  perFileLowering(::EmptyArrayLowering)
  // Replace `coroutineContext` getter calls with `getContinuation().context` calls.
  moduleLowering(::CoroutineContextGetterLowering)
  // Remove functions that contain unsigned varargs in the signature as a temporary workaround for
  // b/242573966.
  // TODO(b/242573966): Remove this when we can handle unsigned vararg types.
  perFileLowering(::RemoveFunctionsWithUnsignedVarargsLowering)
  // Inline function bodies are inlined in a IrReturnableBlock that can contain a return
  // statement. Lower IrReturnableBlocks as labelled blocks, introduce a temporary variable for
  // keeping the returned value and lower return statements inside the block to break statement.
  perFileLowering(jvmReturnableBlockLoweringFactory)
  // Optimize for loops on arrays and integer like progressions.
  perFileLowering(::ForLoopsLowering)
  // Replace null varargs with empty array calls.
  perFileLowering(::EmptyVarargLowering)
  // Lowers unsigned const values to be instantiations of their boxed value types.
  perFileLowering(::UnsignedConstLowering)
  // Move and/or copy companion object fields to static fields of companion's owner.
  perFileLowering(::MoveOrCopyCompanionObjectFieldsLowering)
  // Inline property accessors where the field can be referenced directly
  // (ex. private properties).
  perFileLowering(jvmPropertiesLoweringFactory)
  // Moves fields and accessors out from its property.
  perFileLowering(::LocalDelegatedPropertiesLowering)
  // Make IrGetField/IrSetField to objects' fields point to the static versions
  perFileLowering(::RemapObjectFieldAccesses)
  perFileLowering(jvmLocalClassPopupLoweringFactory)
  // Adds stub methods to the implementations of the read-only collection types to properly
  // implement the Java collection APIs.
  perFileLowering(::CollectionStubMethodLowering)
  // Add static field to hold singleton object instance.
  perFileLowering(::ObjectClassLowering)
  // Drops field initializers when they initialize the field to it's default value.
  perFileLowering(::RemoveFieldInitializerToDefault)
  // Move code from object init blocks and static field initializers to a new <clinit> function.
  perFileLowering(staticInitializersLoweringFactory)
  // Lower field/block initializers into the primary kotlin constructor.
  // TODO(b/233909092): Only lower classes that contain primary ctors to avoid duplicating
  // code in secondary ctors.
  perFileLowering(::InitializersLowering)
  perFileLowering { j2clBackendContext: J2clBackendContext ->
    InitializersCleanupLowering(j2clBackendContext.jvmBackendContext) {
      // Only remove initializers that are non-constants and non-static.
      // Modified from: org.jetbrains.kotlin.backend.jvm.JvmLower.kt
      it.constantValue() == null &&
        (!it.isStatic || it.correspondingPropertySymbol?.owner?.isConst != true)
    }
  }
  // Synthesize static proxy functions for JvmStatic functions in companion objects.
  perFileLowering(::JvmStaticInCompanionLowering)
  // Generate synthetic stubs for functions with default parameter values
  perFileLowering(::J2clDefaultArgumentStubGenerator)
  // Transform calls with default arguments into calls to stubs
  perFileLowering(::J2clDefaultParameterInjector)
  // Replace default values arguments with stubs
  perFileLowering(::J2clDefaultParameterCleaner)
  // Makes function adapters for default arguments static.
  perFileLowering(::StaticDefaultFunctionLowering)
  // Cleanup IMPLICIT_CAST operations introduced by smart casts that are not needed.
  perFileLowering(::SmartCastCleaner)
  perFileLowering(::BridgeLowering)
  // Lowers calls to functions that return unit into a block of the call and a unit object ref.
  perFileLowering(::KotlinUnitValueLowering)
  // Replaces IrExpressionBody with IrBlockBody returning the expression.
  perFileLowering(::ExpressionBodyTransformer)
  // Place calls that return `Nothing` calls into a synthetic block of `{ foo(); Unit }`.
  perFileLowering(::KotlinNothingValueCallsLowering)
  // Mange the names of functions that are shadowing those in super types.
  perFileLowering(::MangleWellKnownShadowingFunctionsLowering)
  // Implement intrinsic. Must run after function inlining.
  perFileLowering(::IntrinsicFunctionCallsLowering)
  // Make JvmStatic functions in non-companion objects static and replace all call sites in the
  // module. Must run after IntrinsicFunctionCallsLowering and before TypeOperatorLowering.
  moduleLowering(::JvmStaticInObjectLowering)
  // Transforms some cast/instanceof operations.
  perFileLowering(::TypeOperatorLowering)
  // Rewrites numeric conversion calls (toInt(), toShort(), etc) to be simple casts instead.
  perFileLowering(::NumericConversionLowering)
  // Removes enum super constructor calls and cleans up effectively empty constructors.
  perFileLowering(::EnumClassConstructorLowering)
  // Rewrites calls to KFunction.invoke() as FunctionN.invoke().
  perFileLowering(::RewriteKFunctionInvokeLowering)

  // BLOCK DECOMPOSITION
  // Transforms statement-like-expression nodes into pure-statement. This should be the last
  // major modification to the AST before it's cleaned up.
  perFileLowering(::BlockDecomposerLowering.asPostfix())

  // CLEANUP PHASE
  // This is the last set of passes to touchup the AST into a form that the CompilationUnitBuilder
  // is going to expect. Lowerings should have very limited scope.

  // Replace singleton object references with static field references.
  perFileLowering(::SingletonReferencesLowering)
  // Reconstruct `for-loop` node for iterations over array and non-overflowing ranges.
  perFileLowering(::CreateForLoopLowering)
  // Convert some `when` statements to a `switch` java-like statement.
  perFileLowering(::CreateSwitchLowering)
  // Lower Kotlin annotation in a Java-like format expected by the J2CL ast.
  perFileLowering(::AnnotationLowering)
  // Ensures that any top-level referenced members from another compilation unit have an enclosing
  // class. The inliner can introduce references to top-level members from another compilation unit
  // that were not referenced before.
  moduleLowering(::ExternalPackageParentPatcherLowering)
  // Flattens extra blocks that were added from function inlining.
  moduleLowering { FlattenInlinedBlocks() }
  // Cleanup the unreachable code that exist after the statement-like-expression
  // transformation to make the IrTree valid.
  perFileLowering(::CleanupLowering)
  // Generate facade classes for JvmMultifileClass parts.
  moduleLowering(::GenerateMultifileFacades)
}

class LoweringPasses(
  private val state: GenerationState,
  private val compilerConfiguration: CompilerConfiguration,
  private val jvmIrDeserializer: JvmIrDeserializer = JvmIrDeserializerImpl(),
) : IrGenerationExtension {
  lateinit var jvmBackendContext: JvmBackendContext
  lateinit var intrinsics: IntrinsicMethods

  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    jvmBackendContext =
      createJvmBackendContext(state, compilerConfiguration, pluginContext, jvmIrDeserializer)
    intrinsics = IntrinsicMethods(pluginContext)

    val j2clBackendContext = J2clBackendContext(jvmBackendContext, intrinsics)

    loweringPhase.lower(j2clBackendContext, moduleFragment)
  }
}

@OptIn(UnsafeDuringIrConstructionAPI::class, ObsoleteDescriptorBasedAPI::class)
private fun createJvmBackendContext(
  state: GenerationState,
  compilerConfiguration: CompilerConfiguration,
  pluginContext: IrPluginContext,
  jvmIrDeserializer: JvmIrDeserializer,
): JvmBackendContext {
  val symbolTable = pluginContext.symbolTable as SymbolTable

  if (jvmIrDeserializer is J2clIrDeserializer) {
    // TODO(b/374966022): Remove this once we don't rely on IR serialization anymore for inlining.
    // K2 does not populate the symbolTable but it still is used by the IR deserializer to know if
    // the symbols exists or need to be created. We will manually populate the SymbolTable.
    // Note: This step is skipped during stdlib compilation. This is because the standard library
    // does not depend on other Kotlin libraries, so no IR deserialization from JAR files will
    // occur.
    // Additionally, this avoids an issue in when processing builtins, which are loaded from source
    // during stdlib compilation rather than from JAR files.
    if (!compilerConfiguration.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
      symbolTable.populate(pluginContext.irBuiltIns)
    }
    // During IR deserialization, unbound symbols are created for references to external
    // declarations that haven't been loaded yet. In the K1 frontend, a stub IrProvider relied on
    // the descriptor API to load these symbols. However, we cannot reuse this in K2 due to the
    // removal of the descriptor API. Therefore, we utilize this custom IrProvider, which rely on
    // the public signature of the nbound symbols and the IR plugin API to resolve IR nodes linked
    // to the symbols.
    jvmIrDeserializer.defaultIrProvider = IrProviderFromPublicSignature(pluginContext)
  }
  return JvmBackendContext(
    state,
    pluginContext.irBuiltIns,
    symbolTable,
    JvmGeneratorExtensionsImpl(compilerConfiguration),
    JvmBackendExtension.Default,
    irSerializer = null,
    irDeserializer = jvmIrDeserializer,
    irProviders = listOf(),
    irPluginContext = pluginContext,
    evaluatorData = null,
  )
}

private fun <T : CommonBackendContext> ((T) -> BodyLoweringPass).asPostfix():
  (T) -> FileLoweringPass = { context: T -> invoke(context).asPostfix() }

private fun BodyLoweringPass.asPostfix(): FileLoweringPass =
  object : FileLoweringPass {
    override fun lower(irFile: IrFile) {
      this@asPostfix.runOnFilePostfix(irFile, withLocalDeclarations = true)
    }
  }
