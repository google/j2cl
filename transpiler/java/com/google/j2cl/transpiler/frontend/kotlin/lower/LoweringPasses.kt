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

import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DefaultParameterCleaner
import org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationsRemoveLowering
import org.jetbrains.kotlin.backend.common.lower.ExpressionBodyTransformer
import org.jetbrains.kotlin.backend.common.lower.InitializersCleanupLowering
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.backend.common.lower.ReturnableBlockLowering
import org.jetbrains.kotlin.backend.common.lower.StripTypeAliasDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.WrapInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.wrapWithCompilationException
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.backend.jvm.lower.JvmDefaultParameterInjector
import org.jetbrains.kotlin.backend.jvm.lower.JvmInventNamesForLocalClasses
import org.jetbrains.kotlin.backend.jvm.lower.JvmLateinitLowering
import org.jetbrains.kotlin.backend.jvm.lower.JvmLocalClassPopupLowering
import org.jetbrains.kotlin.backend.jvm.lower.JvmPropertiesLowering
import org.jetbrains.kotlin.backend.jvm.lower.StaticInitializersLowering
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.lower.cleanup.CleanupLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineDeclarationsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

/** The list of lowering passes to run in execution order. */
private val loweringPassFactories: List<J2clLoweringPassFactory> = buildList {
  // Remove expect declarations, the actuals will be used instead.
  add(::ExpectDeclarationsRemoveLowering)
  // Remove assignments to definedExternally.
  add(::DefinedExternallyLowering)
  // Remove typealias declarations from the IR.
  add(::StripTypeAliasDeclarationsLowering)
  // Put file level function and property declaration into a class.
  add(::FileClassLowering)
  // Make JvmStatic functions in non-companion objects static and replace all call sites in the
  // module.
  add(::JvmStaticInObjectLowering)
  // Invent names for local classes and anonymous objects. Later passes may require all classes
  // to have a name for computing function signature.
  add(::JvmInventNamesForLocalClasses)
  // Rewrites `Array(size) { index -> value }` using type-specific initializer lambdas.
  add(::ArrayConstructorLowering)
  // Create nullable backing fields and insert nullability checks for lateinit properties and
  // variables.
  add(::JvmLateinitLowering)
  // Patch calls to `throwUninitializedPropertyAccessException()` to match our definition of this
  // function.
  add(::PatchThrowUninitializedPropertyExceptionCalls)
  // Extract anonymous classes from inline lambdas.
  add(::LocalClassesInInlineLambdasLowering)
  // Resolve captures for anonymous class defined in inline functions.
  add(::LocalClassesInInlineFunctionsLowering)
  // Move the anonymous classes from inline functions into the nearest declaration container.
  add(::LocalClassesExtractionFromInlineFunctionsLowering.asPostfix())
  // Create public bridge for private top level function called from inline functions.
  add(::SyntheticAccessorLowering)
  // Replace reference to inline function with reified parameter with reference to a synthetic
  // non-inline function where types parameters have been substituted so there is no reference to
  // inline functions anymore.
  // ex:
  // inline fun <reified T> castTo(param: Any): T  = param as T
  // reference like `::castTo<String>` is replaced with `::castTo$wrap` where
  // fun castTo$wrap(param: Any) String = castTo<String>(param)
  add(::WrapInlineDeclarationsWithReifiedTypeParametersLowering)
  // Perform function inlining.
  add(::FunctionInlining)
  // Remove inline functions with reified type parameters as these functions cannot be called from
  // Java
  add(::RemoveInlineDeclarationsWithReifiedTypeParametersLowering)
  // Remove functions that contain unsigned varargs in the signature as a temporary workaround for
  // b/242573966.
  // TODO(b/242573966): Remove this when we can handle unsigned vararg types.
  add(::RemoveFunctionsWithUnsignedVarargsLowering)
  // Inline function bodies are inlined in a IrReturnableBlock that can contain a return
  // statement. Lower IrReturnableBlocks as labelled blocks, introduce a temporary variable for
  // keeping the returned value and lower return statements inside the block to break statement.
  add(::ReturnableBlockLowering)
  // Optimize for loops on arrays and integer like progressions.

  add(::ForLoopsLowering)
  // Replace null varargs with empty array calls.
  add(::EmptyVarargLowering)
  // Replace constant property accessors with a direct reference to the constant value.
  add(::ConstLowering)
  // Lowers unsigned const values to be instantiations of their boxed value types.
  add(::UnsignedConstLowering)
  // Move and/or copy companion object fields to static fields of companion's owner.
  add(::MoveOrCopyCompanionObjectFieldsLowering)
  // Inline property accessors where the field can be referenced directly
  // (ex. private properties).
  add(::JvmPropertiesLowering)
  // Make IrGetField/IrSetField to objects' fields point to the static versions
  add(::RemapObjectFieldAccesses)
  // Extract local functions and move them up to the closest enclosing type. Rewrite local
  // function calls accordingly.
  add(::LocalFunctionLowering)
  add(::JvmLocalClassPopupLowering)
  // Adds stub methods to the implementations of the read-only collection types to properly
  // implement the Java collection APIs.
  add(::CollectionStubMethodLowering)
  // Add static field to hold singleton object instance.
  add(::ObjectClassLowering)
  // Copies static fields from companion objects onto the enclosing interface if a const property
  // is present.
  // add(::CopyInterfaceCompanionFieldsLowering)
  // Drops field initializers when they initialize the field to it's default value.
  add(::RemoveFieldInitializerToDefault)
  // Move code from object init blocks and static field initializers to a new <clinit> function.
  add(::StaticInitializersLowering)
  // Lower field/block initializers into the primary kotlin constructor.
  // TODO(b/233909092): Only lower classes that contain primary ctors to avoid duplicating
  // code in secondary ctors.
  add(::InitializersLowering)
  add { j2clBackendContext ->
    InitializersCleanupLowering(j2clBackendContext.jvmBackendContext) {
      // Only remove initializers that are non-constants and non-static.
      // Modified from: org.jetbrains.kotlin.backend.jvm.JvmLower.kt
      it.constantValue() == null &&
        (!it.isStatic || it.correspondingPropertySymbol?.owner?.isConst != true)
    }
  }
  // Synthesize static proxy functions for JvmStatic functions in companion objects.
  add(::JvmStaticInCompanionLowering)
  // Generate synthetic stubs for functions with default parameter values
  add(::JvmDefaultArgumentStubGenerator)
  // Transform calls with default arguments into calls to stubs
  add(::JvmDefaultParameterInjector)
  // Replace default values arguments with stubs
  add { j2clBackendContext ->
    DefaultParameterCleaner(
      j2clBackendContext.jvmBackendContext,
      replaceDefaultValuesWithStubs = true,
    )
  }
  add(::BridgeLowering)
  // Transforms some cast/instanceof operations.
  add(::TypeOperatorLowering)
  // Rewrites numeric conversion calls (toInt(), toShort(), etc) to be simple casts instead.
  add(::NumericConversionLowering)
  // Lowers calls to functions that return unit into a block of the call and a unit object ref.
  add(::KotlinUnitValueLowering)
  // Replaces IrExpressionBody with IrBlockBody returning the expression.
  add(::ExpressionBodyTransformer)
  // Place calls that return `Nothing` calls into a synthetic block of `{ foo(); Unit }`.
  add(::KotlinNothingValueCallsLowering)
  // Mange the names of functions that are shadowing those in super types.
  add(::MangleWellKnownShadowingFunctionsLowering)
  // Removes enum super constructor calls and cleans up effectively empty constructors.
  add(::EnumClassConstructorLowering)

  // BLOCK DECOMPOSITION
  // Transforms statement-like-expression nodes into pure-statement. This should be the last
  // major modification to the AST before it's cleaned up.
  add(::BlockDecomposerLowering.asPostfix())

  // CLEANUP PHASE
  // This is the last set of passes to touchup the AST into a form that the CompilationUnitBuilder
  // is going to expect. Lowerings should have very limited scope.

  // Replace singleton object references with static field references.
  add(::SingletonReferencesLowering)
  // Reconstruct `for-loop` node for iterations over array and non-overflowing ranges.
  add(::CreateForLoopLowering)
  // Convert some `when` statements to a `switch` java-like statement.
  add(::CreateSwitchLowering)
  // Cleanup the unreachable code that exist after the statement-like-expression
  // transformation to make the IrTree valid.
  add(::CleanupLowering)
}

private fun MutableList<J2clLoweringPassFactory>.add(
  factory: (JvmBackendContext) -> FileLoweringPass
) {
  add(factory.toJ2clLoweringPassFactory())
}

private fun MutableList<J2clLoweringPassFactory>.add(factory: () -> FileLoweringPass) {
  add(factory.toJ2clLoweringPassFactory())
}

class LoweringPasses(
  private val state: GenerationState,
  private val compilerConfiguration: CompilerConfiguration,
) : IrGenerationExtension {
  lateinit var jvmBackendContext: JvmBackendContext

  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    preloadWellKnownSymbols(pluginContext)

    jvmBackendContext =
      createJvmBackendContext(state, compilerConfiguration, moduleFragment, pluginContext)
    val j2clBackendContext = J2clBackendContext(jvmBackendContext)

    // TODO(b/264284345): remove this pass when the bug is fixed.
    // Detect deserialized orphaned IR nodes due to b/264284345 and assign them a parent.
    FixOrphanNode(pluginContext).apply { moduleFragment.files.forEach(this::lower) }

    loweringPassFactories.forEach { moduleFragment.lower(j2clBackendContext, it) }

    // Generate facade classes for JvmMultifileClass parts.
    GenerateMultifileFacadesLowering(jvmBackendContext).lower(moduleFragment)
  }
}

/** Lowers each file in a module, instantiating a new lowering for each file. */
private fun IrModuleFragment.lower(
  context: J2clBackendContext,
  loweringFactory: J2clLoweringPassFactory,
) {
  for (f in files) {
    try {
      loweringFactory(context).lower(f)
    } catch (e: CompilationException) {
      e.initializeFileDetails(f)
      throw e
    } catch (e: Throwable) {
      throw e.wrapWithCompilationException("Internal error in file lowering", f, null)
    }
  }
}

private fun createJvmBackendContext(
  state: GenerationState,
  compilerConfiguration: CompilerConfiguration,
  moduleFragment: IrModuleFragment,
  pluginContext: IrPluginContext,
) =
  JvmBackendContext(
    state,
    moduleFragment.irBuiltins,
    pluginContext.symbolTable as SymbolTable,
    PhaseConfig(
      object : CompilerPhase<JvmBackendContext, Unit, Unit> {
        override fun invoke(
          phaseConfig: PhaseConfigurationService,
          phaserState: PhaserState<Unit>,
          context: JvmBackendContext,
          input: Unit,
        ) {}
      }
    ),
    JvmGeneratorExtensionsImpl(compilerConfiguration),
    JvmBackendExtension.Default,
    irSerializer = null,
    irDeserializer = JvmIrDeserializerImpl(),
    irProviders = emptyList<IrProvider>(),
    irPluginContext = null,
  )

/**
 * Preload symbols that are well-known to lowering passes that may not have been referenced by the
 * input code. This ensures that the symbols are present when we attempt to refer to them.
 */
private fun preloadWellKnownSymbols(pluginContext: IrPluginContext) {
  val classesToPreload =
    listOf(
      // The ForLoopsLowering tries to access the definition of the types below through the
      // BuiltinsSymbolBase class. This class resolves the types by looking into a symbols table
      // If the types are not referenced in the code we are compiling we ends up with an unbound
      // symbol exception. We will simply preload these types coming from stdlib for now but we need
      // to investigate more how Kotlin/JVM does not have the same issue and what they used to
      // preload these types.
      // TODO(b/277283695): Investigate how Kotlin/JVM ends up having these types loaded before
      //  running the ForLoopsLowering pass and remove this method if needed.
      StandardClassIds.UByte,
      StandardClassIds.UShort,
      StandardClassIds.UInt,
      StandardClassIds.ULong,
      StandardClassIds.CharRange,
      StandardClassIds.LongRange,
      StandardClassIds.IntRange,
      ClassId.fromString("kotlin/ranges/UIntRange"),
      ClassId.fromString("kotlin/ranges/ULongRange"),
      ClassId.fromString("kotlin/ranges/CharProgression"),
      ClassId.fromString("kotlin/ranges/IntProgression"),
      ClassId.fromString("kotlin/ranges/LongProgression"),
      ClassId.fromString("kotlin/ranges/UIntProgression"),
      ClassId.fromString("kotlin/ranges/ULongProgression"),
      ClassId.fromString("kotlin/ranges/UIntRange"),

      // Referenced by ArrayConstructorLowering.
      ClassId.fromString("javaemul/internal/ArrayHelper"),
      ClassId.fromString("kotlin/jvm/internal/BooleanArrayInitializer"),
      ClassId.fromString("kotlin/jvm/internal/IntArrayInitializer"),
      ClassId.fromString("kotlin/jvm/internal/LongArrayInitializer"),
      ClassId.fromString("kotlin/jvm/internal/ShortArrayInitializer"),
      ClassId.fromString("kotlin/jvm/internal/ByteArrayInitializer"),
      ClassId.fromString("kotlin/jvm/internal/CharArrayInitializer"),
      ClassId.fromString("kotlin/jvm/internal/DoubleArrayInitializer"),
      ClassId.fromString("kotlin/jvm/internal/FloatArrayInitializer"),
      ClassId.fromString("kotlin/jvm/internal/ArrayInitializer"),
    )
  for (classId in classesToPreload) {
    pluginContext.referenceClass(classId)
  }

  val functionsToPreload =
    listOf(
      // Referenced by ArrayConstructorLowering.
      FqName("kotlin.jvm.internal.toBooleanArrayInitializer"),
      FqName("kotlin.jvm.internal.toIntArrayInitializer"),
      FqName("kotlin.jvm.internal.toLongArrayInitializer"),
      FqName("kotlin.jvm.internal.toShortArrayInitializer"),
      FqName("kotlin.jvm.internal.toByteArrayInitializer"),
      FqName("kotlin.jvm.internal.toCharArrayInitializer"),
      FqName("kotlin.jvm.internal.toDoubleArrayInitializer"),
      FqName("kotlin.jvm.internal.toFloatArrayInitializer"),
      FqName("kotlin.jvm.internal.toArrayInitializer"),
    )
  for (functionName in functionsToPreload) {
    pluginContext.referenceFunctions(CallableId(functionName.parent(), functionName.shortName()))
  }
}
