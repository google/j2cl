/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.* // ktlint-disable
import org.jetbrains.kotlin.ir.expressions.* // ktlint-disable
import org.jetbrains.kotlin.ir.expressions.impl.* // ktlint-disable
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isElseBranch
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.memoryOptimizedMap

/**
 * A pass that transforms statement-like-expression nodes into pure-statement.
 *
 * Copied and modified from org.jetbrains.kotlin.ir.backend.js.lower.BlockDecomposerLowering.kt
 */
// MODIFIED BY GOOGLE
// Replace the reference to JsIrBackendContext by JvmBackendContext. We currently run the koltin
// compiler using the JVM backend. In that context, we cannot create a JsIrBackendContext. This pass
// is mostly backend agnostic and the only part that require a JsIrBackendContext has been modified.
// class BlockDecomposerLowering(val context: JsIrBackendContext) :
class BlockDecomposerLowering(val context: JvmBackendContext) :
  // END OF MODIFICATIONS
  AbstractBlockDecomposerLowering(context) {
  override fun unreachableExpression(): IrExpression {
    // MODIFIED BY GOOGLE
    // Avoid reference to js backend intrinsics:
    // JsIrBuilder.buildCall(context.intrinsics.unreachable, context.irBuiltIns.nothingType)
    //
    // We just need a call to a method that return nothing. This method call will be removed by the
    // cleanup lowering pass.
    return JsIrBuilder.buildCall(
      context.irBuiltIns.illegalArgumentExceptionSymbol,
      context.irBuiltIns.nothingType,
    )
    // END OF MODIFICATIONS
  }
}

abstract class AbstractBlockDecomposerLowering(private val context: JvmBackendContext) :
  BodyLoweringPass {

  private val nothingType = context.irBuiltIns.nothingType

  // Expression with Nothing type to be inserted in places of unreachable expression
  abstract fun unreachableExpression(): IrExpression

  private val decomposerTransformer = BlockDecomposerTransformer(context, ::unreachableExpression)

  override fun lower(irBody: IrBody, container: IrDeclaration) {
    when (container) {
      // MODIFIED BY GOOGLE
      // The JS backend lowers all init blocks into the primary constructor so the original version
      // of this lowering never bothered to process them.
      is IrAnonymousInitializer -> {
        container.accept(decomposerTransformer, null)
        irBody.patchDeclarationParents(container.parent)
      }
      // The JS backend lowers all enum classes so that enum entries are regular fields with the
      // initialization of the enum fields in an initializer block. The original version never
      // bothered to process them.
      is IrEnumEntry -> {
        if (container.correspondingClass != null) {
          // The enum entry is subclassing the enclosing enum class. In this case, any block
          // expression in the constructor call will be decomposed in the body of the constructor of
          // the anonymous class.
          return
        }
        // The logic below is based on the logic used for field initializations.
        container.initializerExpression?.apply {
          val initFunction =
            context.irFactory
              .buildFun {
                name = Name.identifier(container.name.asString() + "\$init\$")
                returnType = container.parentAsClass.defaultType
                visibility = DescriptorVisibilities.PRIVATE
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
              }
              .apply {
                parent = container.parent
                // Make the init function static
                dispatchReceiverParameter = null
              }

          val newBody = this.toBlockBody(initFunction)
          newBody.patchDeclarationParents(initFunction)
          initFunction.body = newBody

          initFunction.accept(decomposerTransformer, null)

          val lastStatement = newBody.statements.last()
          if (
            newBody.statements.size > 1 ||
              lastStatement !is IrReturn ||
              lastStatement.value != expression
          ) {
            expression = JsIrBuilder.buildCall(initFunction.symbol, expression.type)
            (container.parent as IrDeclarationContainer).declarations += initFunction
            patchDeclarationParents(initFunction)
          }
          // We do not need to patch back the parent of the initializerExpression if nothing is
          // decomposed, as we know it's a IrEnumConstructorCall and so not a declaration.
        }
      }
      // END OF MODIFICATIONS
      is IrFunction -> {
        container.accept(decomposerTransformer, null)
        irBody.patchDeclarationParents(container)
      }
      is IrField -> {
        container.initializer?.apply {
          val initFunction =
            context.irFactory
              .buildFun {
                name = Name.identifier(container.name.asString() + "\$init\$")
                returnType = container.type
                visibility = DescriptorVisibilities.PRIVATE
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
              }
              .apply { parent = container.parent }

          val newBody = toBlockBody(initFunction)
          newBody.patchDeclarationParents(initFunction)
          initFunction.body = newBody

          initFunction.accept(decomposerTransformer, null)

          val lastStatement = newBody.statements.last()
          val actualParent =
            if (
              newBody.statements.size > 1 ||
                lastStatement !is IrReturn ||
                lastStatement.value != expression
            ) {
              expression = JsIrBuilder.buildCall(initFunction.symbol, expression.type)
              (container.parent as IrDeclarationContainer).declarations += initFunction
              initFunction
            } else {
              container
            }

          patchDeclarationParents(actualParent)
        }
      }
    }
  }

  private fun IrExpressionBody.toBlockBody(containingFunction: IrFunction): IrBlockBody {
    return context.irFactory.createBlockBody(startOffset, endOffset) {
      expression.patchDeclarationParents(containingFunction)
      val returnStatement =
        JsIrBuilder.buildReturn(containingFunction.symbol, expression, nothingType)
      statements += returnStatement
    }
  }
}

class BlockDecomposerTransformer(
  private val context: CommonBackendContext,
  private val unreachableExpression: () -> IrExpression,
) : IrElementTransformerVoid() {
  private lateinit var function: IrDeclarationParent
  private var tmpVarCounter: Int = 0

  private val statementTransformer = StatementTransformer()
  private val expressionTransformer = ExpressionTransformer()

  private val constTrue
    get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)

  private val constFalse
    get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, false)

  private val unitType = context.irBuiltIns.unitType
  private val unitValue
    get() = JsIrBuilder.buildGetObjectValue(unitType, context.irBuiltIns.unitClass)

  private val booleanNotSymbol = context.irBuiltIns.booleanNotSymbol

  override fun visitScript(declaration: IrScript): IrStatement {
    function = declaration

    with(declaration) {
      val transformedStatements = mutableListOf<IrStatement>()
      statements.forEach {
        val transformer =
          if (it === statements.last()) expressionTransformer else statementTransformer
        val s = it.transformStatement(transformer)
        if (s is IrComposite) {
          transformedStatements.addAll(s.statements)
        } else {
          transformedStatements.add(s)
        }
      }
      statements.clear()
      statements.addAll(transformedStatements)
    }
    return declaration
  }

  override fun visitFunction(declaration: IrFunction): IrStatement {
    function = declaration
    tmpVarCounter = 0
    return declaration.transformStatement(statementTransformer)
  }

  // MODIFIED BY GOOGLE
  // The JS backend lowers all init blocks into the primary constructor so the original version
  // of this lowering never bothered to process them.
  override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
    function = declaration.parent
    tmpVarCounter = 0
    return declaration.transformStatement(statementTransformer)
  }

  // END OF MODIFICATIONS

  override fun visitElement(element: IrElement) = element.transform(statementTransformer, null)

  private fun processStatements(statements: MutableList<IrStatement>) {
    statements.transformFlat { destructureComposite(it.transformStatement(statementTransformer)) }
  }

  private fun makeTempVar(type: IrType, init: IrExpression? = null) =
    JsIrBuilder.buildVar(type, function, initializer = init, isVar = true)

  private fun makeLoopLabel() = "\$l\$${tmpVarCounter++}"

  private fun IrStatement.asExpression(last: IrExpression): IrExpression {
    val composite = JsIrBuilder.buildComposite(last.type)
    composite.statements += transformStatement(statementTransformer)
    composite.statements += last
    return composite
  }

  private fun materializeLastExpression(
    composite: IrComposite,
    block: (IrExpression) -> IrStatement,
  ): IrComposite {
    val statements = composite.statements
    val expression = statements.lastOrNull() as? IrExpression ?: return composite
    statements[statements.lastIndex] = block(expression)
    return composite
  }

  private fun destructureComposite(expression: IrStatement) =
    (expression as? IrComposite)?.statements ?: listOf(expression)

  private inner class BreakContinueUpdater(val breakLoop: IrLoop, val continueLoop: IrLoop) :
    IrElementTransformer<IrLoop> {
    override fun visitBreak(jump: IrBreak, data: IrLoop) =
      jump.apply { if (loop == data) loop = breakLoop }

    override fun visitContinue(jump: IrContinue, data: IrLoop) =
      jump.apply { if (loop == data) loop = continueLoop }
  }

  private inner class StatementTransformer : IrElementTransformerVoid() {
    override fun visitBlockBody(body: IrBlockBody) = body.apply { processStatements(statements) }

    override fun visitContainerExpression(expression: IrContainerExpression) =
      expression.apply { processStatements(statements) }

    override fun visitExpression(expression: IrExpression) =
      expression.transform(expressionTransformer, null)

    override fun visitReturn(expression: IrReturn): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val composite = expression.value as? IrComposite ?: return expression
      return materializeLastExpression(composite) {
        IrReturnImpl(
          expression.startOffset,
          expression.endOffset,
          expression.type,
          expression.returnTargetSymbol,
          it,
        )
      }
    }

    override fun visitThrow(expression: IrThrow): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val composite = expression.value as? IrComposite ?: return expression
      return materializeLastExpression(composite) {
        IrThrowImpl(expression.startOffset, expression.endOffset, expression.type, it)
      }
    }

    override fun visitBreakContinue(jump: IrBreakContinue) = jump

    override fun visitVariable(declaration: IrVariable): IrStatement {
      declaration.transformChildrenVoid(expressionTransformer)

      val composite = declaration.initializer as? IrComposite ?: return declaration
      return materializeLastExpression(composite) { declaration.apply { initializer = it } }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val receiverResult = expression.receiver as? IrComposite
      val valueResult = expression.value as? IrComposite
      if (receiverResult == null && valueResult == null) return expression

      if (receiverResult != null && valueResult != null) {
        val result = IrCompositeImpl(receiverResult.startOffset, expression.endOffset, unitType)
        val receiverValue = receiverResult.statements.last() as IrExpression
        val irVar = makeTempVar(receiverValue.type, receiverValue)
        val setValue = valueResult.statements.last() as IrExpression
        result.statements += receiverResult.statements.run { subList(0, lastIndex) }
        result.statements += irVar
        result.statements += valueResult.statements.run { subList(0, lastIndex) }
        result.statements +=
          expression.run {
            IrSetFieldImpl(
              startOffset,
              endOffset,
              symbol,
              JsIrBuilder.buildGetValue(irVar.symbol),
              setValue,
              type,
              origin,
              superQualifierSymbol,
            )
          }
        return result
      }

      if (receiverResult != null) {
        return materializeLastExpression(receiverResult) {
          expression.run {
            IrSetFieldImpl(
              startOffset,
              endOffset,
              symbol,
              it,
              value,
              type,
              origin,
              superQualifierSymbol,
            )
          }
        }
      }

      assert(valueResult != null)

      val receiver =
        expression.receiver?.let {
          val irVar = makeTempVar(it.type, it)
          valueResult!!.statements.add(0, irVar)
          JsIrBuilder.buildGetValue(irVar.symbol)
        }

      return materializeLastExpression(valueResult!!) {
        expression.run {
          IrSetFieldImpl(
            startOffset,
            endOffset,
            symbol,
            receiver,
            it,
            type,
            origin,
            superQualifierSymbol,
          )
        }
      }
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val composite = expression.value as? IrComposite ?: return expression

      return materializeLastExpression(composite) {
        expression.run { IrSetValueImpl(startOffset, endOffset, type, symbol, it, origin) }
      }
    }

    // while (c_block {}) {
    //  body {}
    // }
    //
    // is transformed into
    //
    // while (true) {
    //   var cond = c_block {}
    //   if (!cond) break
    //   body {}
    // }
    //
    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
      val newBody = loop.body?.transform(statementTransformer, null)
      val newCondition = loop.condition.transform(expressionTransformer, null)

      if (newCondition is IrComposite) {
        val newLoopBody = IrBlockImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin)

        newLoopBody.statements += newCondition.statements.run { subList(0, lastIndex) }
        val thenBlock =
          JsIrBuilder.buildBlock(unitType, listOf(JsIrBuilder.buildBreak(unitType, loop)))

        val newLoopCondition = newCondition.statements.last() as IrExpression

        val breakCond =
          JsIrBuilder.buildCall(booleanNotSymbol).apply { dispatchReceiver = newLoopCondition }

        newLoopBody.statements += JsIrBuilder.buildIfElse(unitType, breakCond, thenBlock)
        newLoopBody.statements.addIfNotNull(newBody)

        return loop.apply {
          condition = constTrue
          body = newLoopBody
        }
      }

      return loop.apply {
        body = newBody
        condition = newCondition
      }
    }

    // do  {
    //  body {}
    // } while (c_block {})
    //
    // is transformed into
    //
    // do {
    //   do {
    //     body {}
    //   } while (false)
    //   cond = c_block {}
    // } while (cond)
    //
    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression {
      val newBody = loop.body?.transform(statementTransformer, null)
      val newCondition = loop.condition.transform(expressionTransformer, null)

      if (newCondition is IrComposite) {
        val innerLoop =
          IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, unitType, loop.origin).apply {
            condition = constFalse
            body = newBody
            label = makeLoopLabel()
          }

        val newLoopCondition = newCondition.statements.last() as IrExpression
        val newLoopBody =
          IrBlockImpl(
              newBody?.startOffset ?: newCondition.startOffset,
              newBody?.endOffset ?: newCondition.endOffset,
              newBody?.type ?: unitType,
            )
            .apply {
              statements += innerLoop
              statements += newCondition.statements.run { subList(0, lastIndex) }
            }

        val newLoop = IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, unitType, loop.origin)

        return newLoop.apply {
          condition = newLoopCondition
          body = newLoopBody.transform(BreakContinueUpdater(newLoop, innerLoop), loop)
          label = loop.label ?: makeLoopLabel()
        }
      }

      return loop.apply {
        body = newBody
        condition = newCondition
      }
    }

    // when {
    //  c1_block {} -> b1_block {}
    //  ....
    //  cn_block {} -> bn_block {}
    //  else -> else_block {}
    // }
    //
    // transformed into if-else chain
    // c1 = c1_block {}
    // if (c1) {
    //   b1_block {}
    // } else {
    //   c2 = c2_block {}
    //   if (c2) {
    //     b2_block{}
    //   } else {
    //         ...
    //           else {
    //              else_block {}
    //           }
    // }
    override fun visitWhen(expression: IrWhen): IrExpression {

      var compositeCount = 0

      val results =
        expression.branches.map {
          val cond = it.condition.transform(expressionTransformer, null)
          val res = it.result.transform(statementTransformer, null)
          if (cond is IrComposite) compositeCount++
          Triple(cond, res, it)
        }

      if (compositeCount == 0) {
        val branches =
          results.memoryOptimizedMap { (cond, res, orig) ->
            when {
              isElseBranch(orig) -> IrElseBranchImpl(orig.startOffset, orig.endOffset, cond, res)
              else /* IrBranch */ -> IrBranchImpl(orig.startOffset, orig.endOffset, cond, res)
            }
          }
        return expression.run { IrWhenImpl(startOffset, endOffset, type, origin, branches) }
      }

      val block =
        IrBlockImpl(expression.startOffset, expression.endOffset, unitType, expression.origin)

      // TODO: consider decomposing only when it is really required
      results.foldIndexed(block) { i, appendBlock, (cond, res, orig) ->
        val condStatements = destructureComposite(cond)
        val condValue = condStatements.last() as IrExpression

        appendBlock.statements += condStatements.run { subList(0, lastIndex) }

        JsIrBuilder.buildBlock(unitType).also {
          val elseBlock = it.takeIf { results.lastIndex != i }
          val additionalStatements =
            when {
              isElseBranch(orig) -> (res as? IrBlock)?.statements ?: listOf(res)
              else ->
                listOf(
                  JsIrBuilder.buildIfElse(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = unitType,
                    cond = condValue,
                    thenBranch = res,
                    elseBranch = elseBlock,
                    origin = expression.origin,
                    thenBranchStartOffset = orig.startOffset,
                    thenBranchEndOffset = orig.endOffset,
                    elseBranchStartOffset = UNDEFINED_OFFSET,
                    elseBranchEndOffset = UNDEFINED_OFFSET,
                  )
                )
            }

          appendBlock.statements += additionalStatements
        }
      }

      return block
    }

    override fun visitTry(aTry: IrTry) = aTry.also { it.transformChildrenVoid(this) }
  }

  private inner class ExpressionTransformer : IrElementTransformerVoid() {

    override fun visitExpression(expression: IrExpression) =
      expression.apply { transformChildrenVoid() }

    override fun visitGetField(expression: IrGetField): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val composite = expression.receiver as? IrComposite ?: return expression

      return materializeLastExpression(composite) {
        expression.run {
          IrGetFieldImpl(startOffset, endOffset, symbol, type, it, origin, superQualifierSymbol)
        }
      }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val composite = expression.argument as? IrComposite ?: return expression

      return materializeLastExpression(composite) { expression.apply { argument = it } }
    }

    override fun visitGetClass(expression: IrGetClass): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val composite = expression.argument as? IrComposite ?: return expression

      return materializeLastExpression(composite) {
        expression.run { IrGetClassImpl(startOffset, endOffset, type, it) }
      }
    }

    override fun visitLoop(loop: IrLoop) = loop.asExpression(unitValue)

    override fun visitSetValue(expression: IrSetValue) = expression.asExpression(unitValue)

    override fun visitSetField(expression: IrSetField) = expression.asExpression(unitValue)

    override fun visitBreakContinue(jump: IrBreakContinue) =
      jump.asExpression(unreachableExpression())

    override fun visitThrow(expression: IrThrow) = expression.asExpression(unreachableExpression())

    override fun visitReturn(expression: IrReturn) =
      expression.asExpression(unreachableExpression())

    override fun visitVariable(declaration: IrVariable) = declaration.asExpression(unitValue)

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val compositeCount = expression.arguments.count { it is IrComposite }

      if (compositeCount == 0) return expression

      val newStatements = mutableListOf<IrStatement>()
      val arguments = mapArguments(expression.arguments, compositeCount, newStatements)

      newStatements +=
        expression.run {
          IrStringConcatenationImpl(
            startOffset,
            endOffset,
            type,
            arguments.memoryOptimizedMap { it!! },
          )
        }
      return JsIrBuilder.buildComposite(expression.type, newStatements)
    }

    private fun mapArguments(
      oldArguments: Collection<IrExpression?>,
      compositeCount: Int,
      newStatements: MutableList<IrStatement>,
      dontDetachFirstArgument: Boolean = false,
    ): List<IrExpression?> {
      var compositesLeft = compositeCount
      val arguments = mutableListOf<IrExpression?>()

      for ((index, arg) in oldArguments.withIndex()) {
        val value =
          if (arg is IrComposite) {
            compositesLeft--
            newStatements += arg.statements.run { subList(0, lastIndex) }
            arg.statements.last() as IrExpression
          } else arg

        val newArg =
          when {
            compositesLeft == 0 -> value
            index == 0 && dontDetachFirstArgument -> value
            value == null -> value
            value.isPure(anyVariable = false, context = context) -> value
            else -> {
              // TODO: do not wrap if value is pure (const, variable, etc)
              val irVar = makeTempVar(value.type, value)
              newStatements += irVar
              JsIrBuilder.buildGetValue(irVar.symbol)
            }
          }

        arguments += newArg
      }
      return arguments
    }

    // TODO: remove this when vararg is lowered
    override fun visitVararg(expression: IrVararg): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      // MODIFIED BY GOOGLE
      // Original code didn't consider composite expressions within a spread, so they would only get
      // lowered if some other argument was a composite.
      //
      // Original code:
      //  val compositeCount = expression.elements.count { it is IrComposite }
      val compositeCount =
        expression.elements.count {
          it is IrComposite || (it as? IrSpreadElement)?.expression is IrComposite
        }
      // END OF MODIFICATIONS

      if (compositeCount == 0) return expression

      val newStatements = mutableListOf<IrStatement>()
      val argumentsExpressions =
        mapArguments(
          expression.elements.map { (it as? IrSpreadElement)?.expression ?: it as IrExpression },
          compositeCount,
          newStatements,
        )

      val arguments =
        expression.elements.withIndex().map { (i, v) ->
          val expr = argumentsExpressions[i]!!
          (v as? IrSpreadElement)?.run { IrSpreadElementImpl(startOffset, endOffset, expr) } ?: expr
        }

      newStatements +=
        expression.run { IrVarargImpl(startOffset, endOffset, type, varargElementType, arguments) }
      return expression.run { IrCompositeImpl(startOffset, endOffset, type, null, newStatements) }
    }

    // The point here is to keep original evaluation order so (there is the same story for
    // StringConcat)
    // d.foo(p1, p2, block {}, p4, block {}, p6, p7)
    //
    // is transformed into
    //
    // var d_tmp = d
    // var p1_tmp = p1
    // var p2_tmp = p2
    // var p3_tmp = block {}
    // var p4_tmp = p4
    // var p5_tmp = block {}
    // d_tmp.foo(p1_tmp, p2_tmp, p3_tmp, p4_tmp, p5_tmp, p6, p7)
    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val oldArguments = mutableListOf(expression.dispatchReceiver, expression.extensionReceiver)
      for (i in 0 until expression.valueArgumentsCount) oldArguments +=
        expression.getValueArgument(i)
      val compositeCount = oldArguments.count { it is IrComposite }

      if (compositeCount == 0) return expression

      val newStatements = mutableListOf<IrStatement>()
      val newArguments = mapArguments(oldArguments, compositeCount, newStatements)

      expression.dispatchReceiver = newArguments[0]
      expression.extensionReceiver = newArguments[1]

      for (i in 0 until expression.valueArgumentsCount) {
        expression.putValueArgument(i, newArguments[i + 2])
      }

      newStatements += expression

      return expression.run { IrCompositeImpl(startOffset, endOffset, type, origin, newStatements) }
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val composite = expression.receiver as? IrComposite ?: return expression

      return materializeLastExpression(composite) { expression.apply { receiver = it } }
    }

    override fun visitDynamicOperatorExpression(
      expression: IrDynamicOperatorExpression
    ): IrExpression {
      expression.transformChildrenVoid(expressionTransformer)

      val oldArguments = listOf(expression.receiver) + expression.arguments
      val compositeCount = oldArguments.count { it is IrComposite }

      if (compositeCount == 0) return expression

      val newStatements = mutableListOf<IrStatement>()
      val newArguments =
        mapArguments(
          oldArguments,
          compositeCount,
          newStatements,
          dontDetachFirstArgument = expression.isReceiverNonDetachable(),
        )

      expression.receiver =
        newArguments[0]
          ?: compilationException("No new receiver in destructured composite", expression)

      for (i in expression.arguments.indices) {
        expression.arguments[i] =
          newArguments[i + 1]
            ?: compilationException("No argument #$i in destructured composite", expression)
      }

      newStatements.add(expression)

      return expression.run { IrCompositeImpl(startOffset, endOffset, type, null, newStatements) }
    }

    // Return if receiver expression cannot be detached from this expression
    private fun IrDynamicOperatorExpression.isReceiverNonDetachable(): Boolean {
      val receiver =
        when (val r = this.receiver) {
          is IrComposite -> r.statements.lastOrNull() ?: return false
          else -> r
        }

      val receiverIsMemberAccess =
        receiver is IrDynamicMemberExpression ||
          (receiver is IrDynamicOperatorExpression &&
            receiver.operator == IrDynamicOperator.ARRAY_ACCESS)

      val operatorDependsOnMemberAccess = (operator == IrDynamicOperator.INVOKE)

      return operator.isAssignmentOperator ||
        (receiverIsMemberAccess && operatorDependsOnMemberAccess)
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
      // MODIFIED BY GOOGLE
      // Object expression are the equivalent of anonymous classes declaration in Java. They are
      // represented as IrBlock expressions containing the class declaration and constructor call.
      // Typically, this pass would move the class declaration up and replace the block with just
      // the constructor call. However, to simplify J2CL AST creation and preserve information for
      // accurately creating `NewInstance` nodes, we skip this decomposition.
      if (expression.origin == IrStatementOrigin.OBJECT_LITERAL) {
        return expression
      }
      // END OF MODIFICATIONS
      expression.run {
        if (statements.isEmpty())
          return IrCompositeImpl(startOffset, endOffset, type, origin, listOf(unitValue))
      }

      val newStatements = mutableListOf<IrStatement>()

      for (i in 0 until expression.statements.lastIndex) {
        newStatements +=
          destructureComposite(expression.statements[i].transformStatement(statementTransformer))
      }

      newStatements +=
        destructureComposite(expression.statements.last().transformStatement(expressionTransformer))

      return JsIrBuilder.buildComposite(expression.type, newStatements)
    }

    private fun wrap(expression: IrExpression) =
      expression as? IrBlock
        ?: expression.let { IrBlockImpl(it.startOffset, it.endOffset, it.type, null, listOf(it)) }

    private fun wrap(expression: IrExpression, variable: IrVariable) =
      wrap(JsIrBuilder.buildSetVariable(variable.symbol, expression, unitType))

    // try {
    //   try_block {}
    // } catch () {
    //   catch_block {}
    // } finally {}
    //
    // transformed into if-else chain
    //
    // Composite [
    //   var tmp
    //   try {
    //     tmp = try_block {}
    //   } catch () {
    //     tmp = catch_block {}
    //   } finally {}
    //   tmp
    // ]
    override fun visitTry(aTry: IrTry): IrExpression {
      val irVar = makeTempVar(aTry.type)

      val newTryResult = wrap(aTry.tryResult, irVar)
      val newCatches =
        aTry.catches.memoryOptimizedMap {
          val newCatchBody = wrap(it.result, irVar)
          IrCatchImpl(it.startOffset, it.endOffset, it.catchParameter, newCatchBody)
        }

      val newTry =
        aTry.run {
          IrTryImpl(startOffset, endOffset, unitType, newTryResult, newCatches, finallyExpression)
        }
      newTry.transformChildrenVoid(statementTransformer)

      val newStatements = listOf(irVar, newTry, JsIrBuilder.buildGetValue(irVar.symbol))
      return JsIrBuilder.buildComposite(aTry.type, newStatements)
    }

    // when {
    //  c1 -> b1_block {}
    //  ....
    //  cn -> bn_block {}
    //  else -> else_block {}
    // }
    //
    // transformed into if-else chain if anything should be decomposed
    //
    // Composite [
    //   var tmp
    //   when {
    //     c1 -> tmp = b1_block {}
    //     ...
    //     cn -> tmp = bn_block {}
    //     else -> tmp = else_block {}
    //   }
    //   tmp
    // ]
    //
    // kept `as is` otherwise

    override fun visitWhen(expression: IrWhen): IrExpression {

      var hasComposites = false
      val decomposedResults =
        expression.branches.map {
          val transformedCondition = it.condition.transform(expressionTransformer, null)
          val transformedResult = it.result.transform(expressionTransformer, null)
          hasComposites =
            hasComposites || transformedCondition is IrComposite || transformedResult is IrComposite
          Triple(it, transformedCondition, transformedResult)
        }

      // MODIFIED BY GOOGLE.
      // Do not create a temporary variable for if/else statement that does not return anything.
      // That may generate wrong javascript later where the `tmp` variable typed as `Unit` may
      // be assigned to something returning a value.
      // original code:
      // if (hasComposites) {
      if (hasComposites && expression.type != unitType) {
        // END OF MODIFICATIONS
        val irVar = makeTempVar(expression.type)

        val newBranches =
          decomposedResults.memoryOptimizedMap { (branch, condition, result) ->
            val newResult = wrap(result, irVar)
            when {
              isElseBranch(branch) ->
                IrElseBranchImpl(branch.startOffset, branch.endOffset, condition, newResult)
              else /* IrBranch  */ ->
                IrBranchImpl(branch.startOffset, branch.endOffset, condition, newResult)
            }
          }

        val newWhen =
          expression
            .run { IrWhenImpl(startOffset, endOffset, unitType, origin, newBranches) }
            .transform(statementTransformer, null) // deconstruct into `if-else` chain

        return JsIrBuilder.buildComposite(
          expression.type,
          listOf(irVar, newWhen, JsIrBuilder.buildGetValue(irVar.symbol)),
        )
      } else {
        val newBranches =
          decomposedResults.memoryOptimizedMap { (branch, condition, result) ->
            when {
              isElseBranch(branch) ->
                IrElseBranchImpl(branch.startOffset, branch.endOffset, condition, result)
              else /* IrBranch  */ ->
                IrBranchImpl(branch.startOffset, branch.endOffset, condition, result)
            }
          }

        // MODIFIED BY GOOGLE.
        // As we do not decompose branches of `if/else` returning Unit. We need to be sure that the
        // if the `if/else` is used as expression, it is decomposed as:
        // [Composite
        //   When {}
        //   return Unit
        // ]
        // original code:
        // return expression.run { IrWhenImpl(startOffset, endOffset, type, origin, newBranches) }
        val newWhen =
          expression.run { IrWhenImpl(startOffset, endOffset, type, origin, newBranches) }
        return if (hasComposites) {
          check(expression.type == unitType)
          JsIrBuilder.buildComposite(expression.type, listOf(newWhen, unitValue))
        } else {
          newWhen
        }
        // END OF MODIFICATIONS
      }
    }
  }
}
