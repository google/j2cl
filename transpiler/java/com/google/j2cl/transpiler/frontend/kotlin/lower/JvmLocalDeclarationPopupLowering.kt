/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationPopupLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.isEnclosedInConstructor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference

/**
 * Moves local classes from field initializers and anonymous init blocks into the containing class.
 *
 * copied from
 * compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/JvmLocalDeclarationPopupLowering.kt
 */
internal class JvmLocalDeclarationPopupLowering(context: JvmBackendContext) :
  LocalDeclarationPopupLowering(context) {
  private val inlineLambdaToScope = mutableMapOf<IrFunction, IrDeclaration>()

  override fun lower(irFile: IrFile) {
    // MODIFIED BY GOOGLE
    // Only inspect calls and lower bodies that are within a non-static field initializer or a
    // non-static anonymous initializer, as those are the only scopes from which local classes are
    // popped up.
    // original code:
    // irFile.findRichInlineLambdas(context as JvmBackendContext) { argument, _, _, scope ->
    //   inlineLambdaToScope[argument.invokeFunction] = scope
    // }
    irFile.accept(
      object : IrInlineReferenceLocator(context as JvmBackendContext) {
        override fun visitFunctionAccess(
          expression: IrFunctionAccessExpression,
          data: IrDeclaration?,
        ) {
          // Only record the lambdas passed to inline functions that are called within a non-static
          // initializer scope; the others never lead to such a scope, so they are irrelevant to
          // `isInNonStaticInitializerScope`. Note that the lambdas are recorded before visiting
          // the arguments, so the scope of the nested inline lambdas is resolved as well.
          if (data != null && isInNonStaticInitializerScope(data)) {
            super.visitFunctionAccess(expression, data)
          } else {
            expression.acceptChildren(this, data)
          }
        }

        override fun visitInlineLambda(
          argument: IrRichFunctionReference,
          callee: IrFunction,
          parameter: IrValueParameter,
          scope: IrDeclaration,
        ) {
          inlineLambdaToScope[argument.invokeFunction] = scope
        }
      },
      null,
    )
    // END MODIFIED BY GOOGLE
    super.lower(irFile)
    inlineLambdaToScope.clear()
  }

  // MODIFIED BY GOOGLE
  override fun lower(irBody: IrBody, container: IrDeclaration) {
    if (!isInNonStaticInitializerScope(container)) return
    super.lower(irBody, container)
  }

  private fun isInNonStaticInitializerScope(declaration: IrDeclaration): Boolean {
    var parent: IrElement = declaration
    while (parent is IrFunction) {
      parent = inlineLambdaToScope[parent] ?: break
    }
    return parent is IrAnonymousInitializer && !parent.isStatic ||
      parent is IrField && !parent.isStatic
  }

  // END MODIFIED BY GOOGLE

  // On JVM, we only pop up local classes in field initializers and anonymous init blocks, so that
  // InitializersLowering would not copy
  // them to each constructor. (Moving all local classes is not possible because of cases where they
  // use reified type parameters,
  // or capture crossinline lambdas.)
  // Upon moving such class, we record that it used to be in an initializer so that the codegen
  // later sets its EnclosingMethod
  // to the primary constructor.
  override fun shouldPopUp(declaration: IrDeclaration, currentScope: ScopeWithIr?): Boolean {
    if (declaration is IrClass) {
      // On JVM, lambdas have package-private visibility after LocalDeclarationsLowering; see
      // `forClass` in `localDeclarationsPhase`.
      if (!super.shouldPopUp(declaration, currentScope) && !declaration.isGeneratedLambdaClass)
        return false

      // MODIFIED BY GOOGLE
      val scope = currentScope?.irElement as? IrDeclaration ?: return false
      if (isInNonStaticInitializerScope(scope)) {
        // END MODIFIED BY GOOGLE
        declaration.isEnclosedInConstructor = true
        return true
      }
      return false
    } else {
      // MODIFIED BY GOOGLE
      // Do not pop up local functions. Prior to Kotlin compiler version 2.3.0, K/JVM had separate
      // lowerings for popping up local classes and local functions. These were later merged into
      // a single lowering. For J2CL, we do not pop up local functions because they are transpiled
      // to local arrow functions in JavaScript.
      // original code:
      // return super.shouldPopUp(declaration, currentScope)
      return false
      // END MODIFIED BY GOOGLE
    }
  }
}

// copied from
// compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/JvmLocalDeclarationsLowering.kt
// for visibility
internal val IrClass.isGeneratedLambdaClass: Boolean
  get() =
    origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL ||
      origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA ||
      origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL ||
      origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
