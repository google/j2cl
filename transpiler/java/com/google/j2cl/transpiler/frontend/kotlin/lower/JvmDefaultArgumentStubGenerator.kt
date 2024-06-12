/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_CONSTRUCTOR_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_METHOD_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_PROPERTY_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.kotlin.ir.copyAnnotationsWhen
import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentStubGenerator
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.getJvmVisibilityOfDefaultArgumentStub
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.FqName

/**
 * A pass that generates function overload stubs for threading default arguments.
 *
 * Copied and modified from
 * org.jetbrains.kotlin.backend.jvm.lower.JvmDefaultArgumentStubGenerator.kt
 */
class JvmDefaultArgumentStubGenerator(context: JvmBackendContext) :
  DefaultArgumentStubGenerator<JvmBackendContext>(
    context = context,
    factory = JvmDefaultArgumentFunctionFactory(context),
    skipInlineMethods = false,
    skipExternalMethods = false,
  ) {
  override fun defaultArgumentStubVisibility(function: IrFunction) =
    function.getJvmVisibilityOfDefaultArgumentStub()

  override fun useConstructorMarker(function: IrFunction): Boolean =
    function is IrConstructor ||
      function.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR ||
      function.origin == JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR

  // MODIFIED BY GOOGLE
  // Don't copy JsInterop member annotations over to these methods as they will conflict with  the
  // original function that was annotated.
  override fun IrFunction.resolveAnnotations(): List<IrConstructorCall> = copyAnnotationsWhen {
    !(isAnnotation(FqName(JS_METHOD_ANNOTATION_NAME)) ||
      isAnnotation(FqName(JS_CONSTRUCTOR_ANNOTATION_NAME)) ||
      isAnnotation(FqName(JS_PROPERTY_ANNOTATION_NAME)))
  }

  // END OF MODIFICATIONS

  // MODIFIED BY GOOGLE
  // Removed IrBlockBodyBuilder.generateSuperCallHandlerCheckIfNeeded as we don't need the runtime
  // enforcement. Ensuring that you don't call a super method with defaults is checked at compile
  // time.
  // END OF MODIFICATIONS

  // Since the call to the underlying implementation in a default stub has different inlining
  // behavior we need to mark it.
  override fun getOriginForCallToImplementation() =
    JvmLoweredStatementOrigin.DEFAULT_STUB_CALL_TO_IMPLEMENTATION
}
