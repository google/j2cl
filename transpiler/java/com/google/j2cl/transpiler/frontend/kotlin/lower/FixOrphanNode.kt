/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE", "KotlincFE10G3")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.kotlin.ir.getValueArgumentAsConst
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmFileFacadeClass
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.explicitParameters
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunction
import org.jetbrains.kotlin.ir.descriptors.IrBasedPropertyDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBasedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrBindablePublicSymbolBase
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

/**
 * Detect and fix deserialized orphaned IR nodes due to b/264284345.
 *
 * The issue occurs when the user calls an inline function from another module that calls another
 * top-level function from the same file that is not referenced elsewhere.
 *
 * See: https://youtrack.jetbrains.com/issue/KT-55678
 */
// TODO(b/264284345): remove this pass when the bug is fixed.
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class FixOrphanNode(val context: IrPluginContext) :
  IrElementTransformerVoidWithContext(), FileLoweringPass {
  private val visitedInlineFunction = HashSet<IrFunction>()
  private val enclosingInlineFunctionStack = mutableListOf<IrFunction>()

  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid()
  }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val callee = expression.symbol.owner

    if (enclosingInlineFunctionStack.isNotEmpty()) {
      // We are visiting a function call made inside an inline function body. We need to
      // ensure that the parent of the callee is initialized to avoid error in the future
      // passes. If it's accessor of a property, we need to ensure that the corresponding property
      // is attached.
      val correspondingProperty = (callee as? IrSimpleFunction)?.correspondingPropertySymbol?.owner
      if (correspondingProperty != null) {
        maybeFixParent(correspondingProperty)
      } else {
        maybeFixParent(callee)
      }
    }

    if (callee.isInline && visitedInlineFunction.add(callee)) {
      enclosingInlineFunctionStack.add(callee)
      // We are visiting a call to an inline function. Let's visit the function declaration
      // to fix any potential call to functions without parent.
      callee.transform(this, null)
      enclosingInlineFunctionStack.removeLast()
    }

    return super.visitFunctionAccess(expression)
  }

  private fun maybeFixParent(expression: IrDeclarationWithName) {
    try {
      expression.parent
    } catch (e: UninitializedPropertyAccessException) {
      // We reach a top level function defined in another compilation unit and that is only
      // referenced by another inline function in the same file. Due to a bug in IR serialization,
      // the `parent` field of this function is not initialized.
      // We will unbind the symbol and regenerate the IR node from the bytecode.
      val symbol =
        expression.symbol as? IrBindablePublicSymbolBase<*, *>
          ?: throw IllegalStateException("Unexpected symbol type.")
      val isProperty = expression is IrProperty

      val memberDescriptorFromByteCode =
        getFunctionDescriptorFromBytecode(symbol, expression.name, isProperty)

      unbindSymbol(symbol)

      // Regenerate The IR node from the descriptor coming from the bytecode. This will also attach
      // the new node to the unbound symbol and fix the issue.
      val newOwner =
        if (isProperty) {
          getLazyIrProvider()
            .generatePropertyStub(memberDescriptorFromByteCode as PropertyDescriptor)
        } else {
          getLazyIrProvider()
            .generateFunctionStub(memberDescriptorFromByteCode as FunctionDescriptor)
        }

      if (newOwner is IrLazyFunction) {
        // As we regenerated the IrLazyFunction node, it will populate its different properties
        // on demand from the IR. The process is done by asking the parent to load the IR. Because
        // the parent has already been visited by this pass, the IR has already been loaded and the
        // different properties of the new nodes will never be populated.  We will force the
        // reloading of the IR by resetting the private properties `irLoaded` of the parent.
        forceIrReloading(newOwner.parent as JvmFileFacadeClass)
      }
      check(symbol.owner == newOwner)
    }
  }

  private fun getLazyIrProvider() =
    checkNotNull((context.symbolTable as SymbolTable).lazyWrapper.stubGenerator)

  private fun getFunctionDescriptorFromBytecode(
    symbol: IrBindablePublicSymbolBase<*, *>,
    memberName: Name,
    isProperty: Boolean
  ): CallableMemberDescriptor {
    val packageDescriptor = context.moduleDescriptor.getPackage(symbol.signature.packageFqName())

    // find the package fragment coming from the JVM byte code that contains the member and get
    // the descriptor from there.
    for (packageFragment in packageDescriptor.fragments) {
      if (packageFragment !is LazyJavaPackageFragment) {
        continue
      }
      val memberScope = packageFragment.getMemberScope()
      val memberDescriptors =
        if (isProperty) {
          memberScope.getContributedVariables(memberName, NoLookupLocation.FROM_BACKEND)
        } else {
          memberScope.getContributedFunctions(memberName, NoLookupLocation.FROM_BACKEND)
        }

      if (memberDescriptors.isEmpty()) {
        // Member is not defined in this jar.
        continue
      }

      if (memberDescriptors.size == 1) {
        return memberDescriptors.single()
      }

      if (isProperty) {
        // We have several descriptors for one property. It means they are top level extension
        // property defined on different classes. Find the right descriptor by looking at the
        // `extensionReceiver`
        val originalPropertyDescriptor = symbol.descriptor as IrBasedPropertyDescriptor
        val originalExtensionReceiverType =
          checkNotNull(originalPropertyDescriptor.owner.getter?.extensionReceiverParameter) {
              "Multiple descriptors found for a non-extension property [$memberName]"
            }
            .type

        return memberDescriptors.single {
          isSameType(
            originalExtensionReceiverType,
            (it as PropertyDescriptor).extensionReceiverParameter?.type
          )
        }
      }

      val originalFunctionDescriptor = symbol.descriptor as IrBasedSimpleFunctionDescriptor

      return memberDescriptors.single {
        isSameFunctionDescriptor(originalFunctionDescriptor, it as FunctionDescriptor)
      }
    }

    throw IllegalStateException(
      "The member ${symbol.signature} is not found in any dependency jars"
    )
  }

  private fun isSameFunctionDescriptor(
    irFunctionDesc: IrBasedSimpleFunctionDescriptor,
    binaryFunctionDesc: FunctionDescriptor
  ): Boolean {
    if (getJvmName(irFunctionDesc) != getJvmName(binaryFunctionDesc)) {
      return false
    }

    if (!isSameType(irFunctionDesc.owner.returnType, binaryFunctionDesc.returnType)) {
      return false
    }

    val function1Params = irFunctionDesc.owner.explicitParameters
    val function2Params = binaryFunctionDesc.explicitParameters

    if (function1Params.size != function2Params.size) {
      return false
    }

    for ((index, function1Param) in function1Params.withIndex()) {
      if (!isSameType(function1Param.type, function2Params[index].type)) {
        return false
      }
    }
    return true
  }

  private fun getJvmName(descriptor: FunctionDescriptor): String? {
    // Do not read the annotation from the descriptor if this is the descriptor of the function we
    // try to fix the parent. Reading the annotation fro the descriptor will trigger the issue we
    // are working around.
    if (descriptor is IrBasedSimpleFunctionDescriptor) {
      return descriptor.owner.annotations
        .findAnnotation(DescriptorUtils.JVM_NAME)
        ?.getValueArgumentAsConst(Name.identifier("name"), IrConstKind.String)
    }
    return descriptor.annotations
      .findAnnotation(DescriptorUtils.JVM_NAME)
      ?.allValueArguments
      ?.get(Name.identifier("name"))
      ?.value as? String
  }

  private fun isSameType(typeFromIr: IrType, typeFromBinary: KotlinType?): Boolean {
    // if `typeFromIr` is a type parameter, trying to resolve it will trigger the issue we are
    // working around because it requires to load the IR of the function declaring the type
    // parameter. To avoid that, we will just check if `typeFromBinary` is also a type parameter to
    // consider them as being the same type. If a function has two signatures that just differs from
    // the order of its type parameters, we will find two equivalent type descriptors and fail
    // later. That's most unlikely that a user will write that kind of code.
    if (typeFromIr.isTypeParameter()) {
      return typeFromBinary?.isTypeParameter() == true
    }

    // Parametrized type can refer to a type parameter defined on the function and we can trigger
    // the issue we are working around for the same reason than above. Just comparing the fqn of the
    // type without taking into account the type arguments is enough to disambiguate the overloads.
    if (typeFromIr is IrSimpleType && typeFromIr.arguments.isNotEmpty()) {
      return typeFromIr.classFqName?.asString() ==
        typeFromBinary?.getKotlinTypeFqName(printTypeArguments = false)
    }

    // There is no more risk to resolve the IrType to a KotlinType and just compare them
    return typeFromIr.toKotlinType() == typeFromBinary
  }

  private fun unbindSymbol(symbol: IrBindablePublicSymbolBase<*, *>) {
    // This is the first hacky part of this pass. There is no public API to unbind a symbol, so we
    // use reflection for resetting the private backing field `_owner` so the symbol is considered
    // as unbound and the new IR node that will be generated from byte code will be assigned to him.
    val ownerProperty =
      IrBindablePublicSymbolBase::class.memberProperties.first { it.name == "_owner" }
        as KMutableProperty1<IrBindablePublicSymbolBase<*, *>, Any?>
    ownerProperty.isAccessible = true
    ownerProperty.set(symbol, null)
  }

  private fun forceIrReloading(fileClass: JvmFileFacadeClass) {
    val irLoadedProperty =
      JvmFileFacadeClass::class.memberProperties.first { it.name == "irLoaded" }
        as KMutableProperty1<JvmFileFacadeClass, Boolean?>
    irLoadedProperty.isAccessible = true
    irLoadedProperty.set(fileClass, null)
  }
}
