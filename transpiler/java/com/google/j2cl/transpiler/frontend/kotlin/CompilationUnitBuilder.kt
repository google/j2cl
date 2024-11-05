/*
 * Copyright 2021 Google Inc.
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

package com.google.j2cl.transpiler.frontend.kotlin

import com.google.common.collect.ImmutableList
import com.google.j2cl.common.SourcePosition
import com.google.j2cl.transpiler.ast.ArrayAccess
import com.google.j2cl.transpiler.ast.ArrayLength
import com.google.j2cl.transpiler.ast.ArrayLiteral
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
import com.google.j2cl.transpiler.ast.AstUtils
import com.google.j2cl.transpiler.ast.BinaryExpression
import com.google.j2cl.transpiler.ast.BinaryOperator
import com.google.j2cl.transpiler.ast.Block
import com.google.j2cl.transpiler.ast.BooleanLiteral
import com.google.j2cl.transpiler.ast.BreakStatement
import com.google.j2cl.transpiler.ast.CastExpression
import com.google.j2cl.transpiler.ast.CatchClause
import com.google.j2cl.transpiler.ast.CompilationUnit
import com.google.j2cl.transpiler.ast.ConditionalExpression
import com.google.j2cl.transpiler.ast.ContinueStatement
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.DoWhileStatement
import com.google.j2cl.transpiler.ast.Expression
import com.google.j2cl.transpiler.ast.Field
import com.google.j2cl.transpiler.ast.FieldAccess
import com.google.j2cl.transpiler.ast.ForEachStatement
import com.google.j2cl.transpiler.ast.ForStatement
import com.google.j2cl.transpiler.ast.FunctionExpression
import com.google.j2cl.transpiler.ast.IfStatement
import com.google.j2cl.transpiler.ast.InitializerBlock
import com.google.j2cl.transpiler.ast.InstanceOfExpression
import com.google.j2cl.transpiler.ast.JsDocCastExpression
import com.google.j2cl.transpiler.ast.Label
import com.google.j2cl.transpiler.ast.LabeledStatement
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.LocalClassDeclarationStatement
import com.google.j2cl.transpiler.ast.Member
import com.google.j2cl.transpiler.ast.Method
import com.google.j2cl.transpiler.ast.MethodCall
import com.google.j2cl.transpiler.ast.MethodDescriptor
import com.google.j2cl.transpiler.ast.MethodReference
import com.google.j2cl.transpiler.ast.MultiExpression
import com.google.j2cl.transpiler.ast.NewArray
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.NullLiteral
import com.google.j2cl.transpiler.ast.NumberLiteral
import com.google.j2cl.transpiler.ast.PrefixExpression
import com.google.j2cl.transpiler.ast.PrefixOperator
import com.google.j2cl.transpiler.ast.ReturnStatement
import com.google.j2cl.transpiler.ast.RuntimeMethods
import com.google.j2cl.transpiler.ast.Statement
import com.google.j2cl.transpiler.ast.Statement.createNoopStatement
import com.google.j2cl.transpiler.ast.StringLiteral
import com.google.j2cl.transpiler.ast.SuperReference
import com.google.j2cl.transpiler.ast.SwitchCase
import com.google.j2cl.transpiler.ast.SwitchStatement
import com.google.j2cl.transpiler.ast.ThisReference
import com.google.j2cl.transpiler.ast.ThrowStatement
import com.google.j2cl.transpiler.ast.TryStatement
import com.google.j2cl.transpiler.ast.Type
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors
import com.google.j2cl.transpiler.ast.TypeLiteral
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment
import com.google.j2cl.transpiler.ast.WhileStatement
import com.google.j2cl.transpiler.frontend.common.AbstractCompilationUnitBuilder
import com.google.j2cl.transpiler.frontend.kotlin.ir.IntrinsicMethods
import com.google.j2cl.transpiler.frontend.kotlin.ir.findFunctionByName
import com.google.j2cl.transpiler.frontend.kotlin.ir.getArguments
import com.google.j2cl.transpiler.frontend.kotlin.ir.getNameSourcePosition
import com.google.j2cl.transpiler.frontend.kotlin.ir.getParameters
import com.google.j2cl.transpiler.frontend.kotlin.ir.getSourcePosition
import com.google.j2cl.transpiler.frontend.kotlin.ir.getTypeSubstitutionMap
import com.google.j2cl.transpiler.frontend.kotlin.ir.hasVoidReturn
import com.google.j2cl.transpiler.frontend.kotlin.ir.isAdaptedFunctionReference
import com.google.j2cl.transpiler.frontend.kotlin.ir.isClinit
import com.google.j2cl.transpiler.frontend.kotlin.ir.isSuperCall
import com.google.j2cl.transpiler.frontend.kotlin.ir.isSynthetic
import com.google.j2cl.transpiler.frontend.kotlin.ir.isUnitInstanceReference
import com.google.j2cl.transpiler.frontend.kotlin.ir.javaName
import com.google.j2cl.transpiler.frontend.kotlin.ir.resolveLabel
import com.google.j2cl.transpiler.frontend.kotlin.ir.typeSubstitutionMap
import com.google.j2cl.transpiler.frontend.kotlin.ir.unfoldExpression
import com.google.j2cl.transpiler.frontend.kotlin.lower.IrForInLoop
import com.google.j2cl.transpiler.frontend.kotlin.lower.IrForLoop
import com.google.j2cl.transpiler.frontend.kotlin.lower.IrSwitch
import com.google.j2cl.transpiler.frontend.kotlin.lower.IrSwitchBreak
import com.google.j2cl.transpiler.frontend.kotlin.lower.IrSwitchCase
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.jvm.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.isMultifileBridge
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrVarargElement
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.SpecialNames

/** Creates a J2CL Java AST from Kotlin IR. */
class CompilationUnitBuilder(
  private val environment: KotlinEnvironment,
  private val intrinsicMethods: IntrinsicMethods,
) : AbstractCompilationUnitBuilder() {
  private val variableBySymbol: MutableMap<IrValueSymbol, Variable> = mutableMapOf()
  private lateinit var currentIrFile: IrFile
  private val labelsInScope: MutableMap<String, ArrayDeque<Label>> = mutableMapOf()

  fun convert(irModuleFragment: IrModuleFragment): List<CompilationUnit> =
    irModuleFragment.files.map(::convertFile)

  private fun convertFile(irFile: IrFile): CompilationUnit {
    currentIrFile = irFile
    val compilationUnit =
      if (irFile.fileEntry is MultifileFacadeFileEntry)
        CompilationUnit.createSynthetic(irFile.packageFqName.asString())
      else CompilationUnit.createForFile(irFile.fileEntry.name, irFile.packageFqName.asString())

    require(irFile.declarations.all { it is IrClass }) {
      "IrFile nodes should only contain IrClass members"
    }

    irFile.declarations
      .filterIsInstance<IrClass>()
      .filterNot(IrClass::isAnnotationClass)
      .map(::convertClass)
      .forEach(compilationUnit::addType)

    return compilationUnit
  }

  private fun convertClass(irClass: IrClass): Type {
    val type = Type(getNameSourcePosition(irClass), environment.getDeclarationForType(irClass))
    processEnclosedBy(type) {
      // Skip synthetic declarations. Kotlinc adds synthetic declarations like (fake) override
      // members
      // to help with bridge synthesis and the resolution phase.
      // These members may be used as fake placeholder to expose to Koltin java apis that exist at
      // runtime.
      val declarations = irClass.declarations.filter { !it.isSynthetic }

      declarations
        .filter { it !is IrClass && !it.isClinit }
        .mapNotNull(::convertDeclaration)
        .forEach(type::addMembers)

      declarations
        .filterIsInstance<IrClass>()
        .filterNot(IrClass::isAnnotationClass)
        .mapNotNull(::convertClass)
        .forEach(type::addType)

      declarations
        .find { it.isClinit }
        ?.apply { type.addStaticInitializerBlock(convertBody((this as IrFunction).body!!)) }
    }
    return type
  }

  private fun convertDeclaration(irDeclaration: IrDeclaration): List<Member> =
    when (irDeclaration) {
      is IrEnumEntry -> listOf(convertEnumEntry(irDeclaration))
      is IrProperty -> convertProperty(irDeclaration)
      // Lowering passes can add field on object classes.
      is IrField -> listOf(convertField(irDeclaration))
      is IrFunction -> listOf(convertFunction(irDeclaration))
      is IrAnonymousInitializer -> listOf(convertAnonymousInitializer(irDeclaration))
      else -> throw NotImplementedError("Declaration not yet supported: $irDeclaration")
    }

  private fun convertEnumEntry(irEnumEntry: IrEnumEntry): Field {
    val initializerExpression = requireNotNull(irEnumEntry.initializerExpression).expression
    // When statement expressions are used in the enum constructor call, they are decomposed and
    // extracted into a function that executes the statements and calls the constructor.
    val initializer =
      if (initializerExpression is IrEnumConstructorCall) {
        convertConstructorCall(
          initializerExpression,
          anonymousInnerClass = irEnumEntry.correspondingClass,
        )
      } else {
        check(initializerExpression is IrCall)
        // We cannot have a call to a function when the entry subclasses the enum because the
        // decomposer will always decompose the block expression into the constructor of the
        // anonymous class.
        check(irEnumEntry.correspondingClass == null)
        convertExpression(initializerExpression)
      }

    return Field.Builder.from(environment.getDeclaredFieldDescriptor(irEnumEntry))
      .setSourcePosition(getSourcePosition(irEnumEntry))
      .setNameSourcePosition(getNameSourcePosition(irEnumEntry))
      .setInitializer(initializer)
      .build()
  }

  private fun convertProperty(irProperty: IrProperty): List<Member> {
    val members = arrayListOf<Member>()
    // TODO(b/228313275): Ignore the backing field if it is unused.
    irProperty.backingField?.let { members.add(convertField(it)) }
    irProperty.getter?.let { members.add(convertFunction(it)) }
    irProperty.setter?.let { members.add(convertFunction(it)) }
    return members
  }

  private fun convertField(irField: IrField): Field {
    val declaredFieldDescriptor = environment.getDeclaredFieldDescriptor(irField)
    val initializer: Expression? =
      if (declaredFieldDescriptor.isCompileTimeConstant) {
        declaredFieldDescriptor.constantValue
      } else {
        irField.initializer?.let { convertExpression(it.expression) }
      }
    return Field.Builder.from(declaredFieldDescriptor)
      .setSourcePosition(getSourcePosition(irField))
      .setNameSourcePosition(getNameSourcePosition(irField))
      .setInitializer(initializer)
      .build()
  }

  private fun convertFunction(irFunction: IrFunction): Method {
    val parameters = irFunction.getParameters().map(this::createVariable)
    val methodDescriptor = environment.getDeclaredMethodDescriptor(irFunction)
    val body =
      when {
        // Confusingly external property accessor functions have a defined body. We'll ignore the
        // body of any function that is external and rely on kotlinc to properly enforce this.
        irFunction.body != null && !irFunction.isExternal -> convertBody(irFunction.body!!)
        else -> Block.newBuilder().setSourcePosition(getSourcePosition(irFunction)).build()
      }
    return Method.newBuilder()
      .setMethodDescriptor(methodDescriptor)
      .setSourcePosition(getNameSourcePosition(irFunction))
      .setParameters(parameters)
      .setBodySourcePosition(body.sourcePosition)
      .addStatements(body.statements)
      .build()
  }

  private fun convertBody(body: IrBody): Block =
    Block.newBuilder()
      .setSourcePosition(getSourcePosition(body))
      .addStatements(convertStatements(body.statements))
      .build()

  private fun convertAnonymousInitializer(
    irAnonymousInitializer: IrAnonymousInitializer
  ): InitializerBlock =
    InitializerBlock.newBuilder()
      .setBody(convertBody(irAnonymousInitializer.body))
      .setDescriptor(
        environment
          .getDeclaredTypeDescriptor(irAnonymousInitializer.parentAsClass.defaultType)
          .initMethodDescriptor
      )
      .setSourcePosition(getSourcePosition(irAnonymousInitializer))
      .build()

  private fun convertStatements(statements: List<IrStatement>): List<Statement> =
    statements.map(::convertStatement)

  private fun convertStatement(irStatement: IrStatement): Statement =
    when (irStatement) {
      is IrInstanceInitializerCall ->
        throw IllegalStateException("IrInstanceInitializerCall statements should have been lowered")
      is IrClass -> convertLocalClass(irStatement)
      is IrContainerExpression -> convertContainer(irStatement)
      is IrVariable -> convertVariableStatement(irStatement)
      is IrWhen -> convertWhenStatement(irStatement)
      is IrLoop -> convertLoop(irStatement)
      is IrReturn -> convertReturnStatement(irStatement)
      is IrTry -> convertTryStatement(irStatement)
      is IrThrow -> convertThrowStatement(irStatement)
      is IrBreak -> convertBreakStatement(irStatement)
      is IrContinue -> convertContinueStatement(irStatement)
      is IrSwitch -> convertSwitchCase(irStatement)
      is IrSwitchBreak -> convertSwitchBreakStatement(irStatement)
      is IrExpression -> convertExpressionStatement(irStatement)
      else -> throw IllegalStateException("Unhandled IrStatement:\n${irStatement.dump()}")
    }

  private fun convertLocalClass(irClass: IrClass): Statement =
    LocalClassDeclarationStatement(convertClass(irClass), getSourcePosition(irClass))

  private fun convertContainer(irBlock: IrContainerExpression): Block =
    Block.newBuilder()
      .setSourcePosition(getSourcePosition(irBlock))
      .setStatements(convertStatements(irBlock.statements))
      .build()

  private fun convertVariableStatement(irVariable: IrVariable): Statement =
    convertVariableDeclaration(irVariable).makeStatement(getSourcePosition(irVariable))

  private fun convertWhenStatement(irWhen: IrWhen): Statement {
    return checkNotNull(
      convertBranches(irWhen.branches, ::convertStatement) {
        condition: Expression,
        thenStatement: Statement,
        elseStatement: Statement?,
        position: SourcePosition ->
        IfStatement.newBuilder()
          .setSourcePosition(position)
          .setConditionExpression(condition)
          .setThenStatement(thenStatement)
          .setElseStatement(elseStatement)
          .build()
      }
    ) {
      "Unexpected when without branches."
    }
  }

  /** Convert a list of `when` branches to a succession of conditional expressions or statements. */
  private fun <T> convertBranches(
    branches: List<IrBranch>,
    subExpressionConverterFn: (IrExpression) -> T,
    conditionalConverterFn: (Expression, T, T?, SourcePosition) -> T,
  ): T? =
    when {
      branches.isEmpty() -> null
      branches[0] is IrElseBranch -> subExpressionConverterFn(branches[0].result)
      else ->
        conditionalConverterFn(
          convertExpression(branches[0].condition),
          subExpressionConverterFn(branches[0].result),
          convertBranches(
            branches.subList(1, branches.size),
            subExpressionConverterFn,
            conditionalConverterFn,
          ),
          getSourcePosition(branches[0]),
        )
    }

  private fun convertLoop(irLoop: IrLoop): Statement {
    fun createLoopStatement() =
      when (irLoop) {
        is IrWhileLoop -> convertWhileLoop(irLoop)
        is IrDoWhileLoop -> convertDoWhileLoop(irLoop)
        is IrForLoop -> convertForLoop(irLoop)
        is IrForInLoop -> convertForInLoop(irLoop)
        else ->
          throw IllegalStateException("IrLoop type not recognized ${irLoop::class.simpleName}")
      }

    val label = irLoop.label?.let { Label.newBuilder().setName(it).build() }

    if (label != null) {
      // Labeled loop. Add the label to scope before creating the loop statement.
      labelsInScope.computeIfAbsent(label.name) { ArrayDeque() }.addFirst(label)
      val loopStatement = createLoopStatement()
      labelsInScope[label.name]!!.removeFirst()

      return LabeledStatement.newBuilder()
        .setSourcePosition(getSourcePosition(irLoop))
        .setLabel(label)
        .setStatement(loopStatement)
        .build()
    }

    // Unlabeled, just return the loop statement itself.
    return createLoopStatement()
  }

  private fun convertWhileLoop(irWhileLoop: IrWhileLoop): Statement =
    WhileStatement.newBuilder()
      .setSourcePosition(getSourcePosition(irWhileLoop))
      .setConditionExpression(convertExpression(irWhileLoop.condition))
      .setBody(convertStatement(irWhileLoop.body!!))
      .build()

  private fun convertDoWhileLoop(irDoWhileLoop: IrDoWhileLoop): Statement =
    DoWhileStatement.newBuilder()
      .setSourcePosition(getSourcePosition(irDoWhileLoop))
      // Order matters here. We need to convert the body before the condition because the condition
      // can refer to variable created in the body.
      .setBody(irDoWhileLoop.body?.let { convertStatement(it) } ?: createNoopStatement())
      .setConditionExpression(convertExpression(irDoWhileLoop.condition))
      .build()

  private fun convertForLoop(irForLoop: IrForLoop): Statement =
    ForStatement.newBuilder()
      .setInitializers(convertVariableDeclarations(irForLoop.initializers))
      .setConditionExpression(convertExpression(irForLoop.condition))
      .setUpdates(convertExpressions(irForLoop.updates))
      .setBody(convertStatement(checkNotNull(irForLoop.body) { "Body cannot not be null." }))
      .setSourcePosition(getSourcePosition(irForLoop))
      .build()

  private fun convertForInLoop(irForInLoop: IrForInLoop): Statement =
    ForEachStatement.newBuilder()
      .setLoopVariable(createVariable(irForInLoop.variable))
      .setIterableExpression(
        convertExpression(irForInLoop.condition).also {
          check(it.typeDescriptor.isAssignableTo(TypeDescriptors.get().javaLangIterable)) {
            "ForEach must loop over an iterable expression"
          }
        }
      )
      .setBody(convertStatement(checkNotNull(irForInLoop.body) { "Body cannot not be null." }))
      .setSourcePosition(getSourcePosition(irForInLoop))
      .build()

  private fun convertReturnStatement(irReturn: IrReturn): Statement {
    val returnTarget = (irReturn.returnTargetSymbol as IrFunctionSymbol).owner
    val value =
      // If the method return type should be represented as a void, omit the return value of Unit.
      // (Except muti-file bridge methods which are not lowered and it is not safe to drop their
      // return value.)
      if (returnTarget.hasVoidReturn && !returnTarget.isMultifileBridge()) {
        check(irReturn.value.isUnitInstanceReference) {
          "Methods with a void return type should have been lowered to only return Unit.INSTANCE"
        }
        null
      } else {
        irReturn.value
      }
    return ReturnStatement.newBuilder()
      .setExpression(value?.let { convertExpression(it) })
      .setSourcePosition(getSourcePosition(irReturn))
      .build()
  }

  private fun convertTryStatement(irTry: IrTry): Statement =
    TryStatement.newBuilder()
      .setBody(convertContainer(irTry.tryResult as IrContainerExpression))
      .setCatchClauses(irTry.catches.map(::convertCatch))
      .setFinallyBlock(
        irTry.finallyExpression?.let { convertContainer(it as IrContainerExpression) }
      )
      .setSourcePosition(getSourcePosition(irTry))
      .build()

  private fun convertCatch(irCatch: IrCatch): CatchClause =
    CatchClause.newBuilder()
      .setExceptionVariable(createVariable(irCatch.catchParameter))
      .setBody(convertContainer(irCatch.result as IrContainerExpression))
      .build()

  private fun convertThrowStatement(irThrow: IrThrow): Statement =
    ThrowStatement.newBuilder()
      .setSourcePosition(getSourcePosition(irThrow))
      .setExpression(convertExpression(irThrow.value))
      .build()

  private fun convertBreakStatement(irBreak: IrBreak): Statement =
    BreakStatement.newBuilder()
      .setSourcePosition(getSourcePosition(irBreak))
      .setLabelReference(
        irBreak.resolveLabel()?.let { labelsInScope[it]!!.first().createReference() }
      )
      .build()

  private fun convertContinueStatement(irContinue: IrContinue): Statement =
    ContinueStatement.newBuilder()
      .setSourcePosition(getSourcePosition(irContinue))
      .setLabelReference(
        irContinue.resolveLabel()?.let { labelsInScope[it]!!.first().createReference() }
      )
      .build()

  private fun convertSwitchCase(irSwitch: IrSwitch) =
    SwitchStatement.newBuilder()
      .setExpression(convertExpression(irSwitch.expression))
      .setSourcePosition(getSourcePosition(irSwitch))
      .setCases(irSwitch.cases.map { convertCaseStatement(it) })
      .build()

  private fun convertCaseStatement(irSwitchCase: IrSwitchCase): SwitchCase {
    val statements = mutableListOf<Statement>()
    if (irSwitchCase.body != null) {
      statements.add(convertStatement(irSwitchCase.body!!))
    }

    return SwitchCase.newBuilder()
      .setCaseExpressions(
        irSwitchCase.caseExpression?.let { ImmutableList.of(convertExpression(it)) }
          ?: ImmutableList.of()
      )
      .setStatements(statements)
      .build()
  }

  private fun convertSwitchBreakStatement(irSwitchBreak: IrSwitchBreak) =
    BreakStatement.newBuilder().setSourcePosition(getSourcePosition(irSwitchBreak)).build()

  private fun convertExpressionStatement(irExpression: IrExpression): Statement =
    convertExpression(irExpression).makeStatement(getSourcePosition(irExpression))

  private fun convertExpressions(expressions: List<IrExpression>): List<Expression> =
    expressions.map { convertExpression(it) }

  private fun convertExpression(irExpression: IrExpression): Expression =
    when (irExpression) {
      is IrBlock -> convertBlockExpression(irExpression)
      is IrStringConcatenation -> convertStringConcatenation(irExpression)
      is IrWhen -> convertWhenExpression(irExpression)
      is IrCall -> convertCall(irExpression)
      is IrConstructorCall -> convertConstructorCall(irExpression)
      is IrEnumConstructorCall -> convertEnumConstructorCall(irExpression)
      is IrGetValue -> convertGetValue(irExpression)
      is IrSetValue -> convertSetValue(irExpression)
      is IrSetField -> convertSetField(irExpression)
      is IrGetField -> convertGetField(irExpression)
      is IrConst<*> -> convertConstant(irExpression)
      is IrTypeOperatorCall -> convertTypeOperatorCall(irExpression)
      is IrGetEnumValue -> convertGetEnumValue(irExpression)
      is IrFunctionAccessExpression -> convertFunctionAccessExpression(irExpression)
      is IrVararg -> convertVararg(irExpression)
      is IrFunctionReference -> convertFunctionReference(irExpression)
      is IrFunctionExpression -> convertFunctionExpression(irExpression)
      is IrPropertyReference -> convertPropertyReference(irExpression)
      is IrLocalDelegatedPropertyReference -> convertLocalDelegatedPropertyReference(irExpression)
      is IrClassReference -> convertClassReference(irExpression)
      is IrGetClass -> convertGetClass(irExpression)
      else -> throw IllegalStateException("Unhandled IrExpression:\n${irExpression.dump()}")
    }

  private fun convertBlockExpression(irBlock: IrBlock) =
    if (irBlock.origin == IrStatementOrigin.OBJECT_LITERAL) {
      convertObjectExpression(irBlock)
    } else {
      throw IllegalStateException(
        "Only IrBlocks corresponding to object literals should be present after lowering."
      )
    }

  private fun convertObjectExpression(irBlock: IrBlock): Expression {
    check(irBlock.origin == IrStatementOrigin.OBJECT_LITERAL)
    // When the object expression is inside an inline function, the class has been moved from the
    // inline functions to the nearest declaration container (for avoiding redefining the anonymous
    // class at each call site) and the block contains only the ctor call.
    if (irBlock.statements.size == 1) {
      return convertConstructorCall(
        checkNotNull(irBlock.statements[0] as? IrFunctionAccessExpression)
      )
    }

    // In all other cases, the block expression contains the class declaration and the call to the
    // ctor of this class.
    check(irBlock.statements.size == 2) { "Invalid number of statements." }
    val anonymousClass = checkNotNull(irBlock.statements[0] as? IrClass)
    val ctorCall = checkNotNull(irBlock.statements[1] as? IrFunctionAccessExpression)
    return convertConstructorCall(ctorCall, anonymousClass)
  }

  private fun convertStringConcatenation(irExpression: IrStringConcatenation) =
    // String concatenation is left associative.
    irExpression.arguments.fold<IrExpression, Expression>(
      // Ensure the first item of concatenation chain is a string to avoid interpreting the binary
      // operation as an arithmetic operation.
      StringLiteral("")
    ) { accumulatedExpression, argument ->
      BinaryExpression.newBuilder()
        .setLeftOperand(accumulatedExpression)
        .setOperator(BinaryOperator.PLUS)
        .setRightOperand(convertExpression(argument))
        .build()
    }

  private fun convertWhenExpression(irWhen: IrWhen): Expression =
    // `When` expressions that contain statements have already been lowered. At this point when
    // `irWhen` is expected as an expression we can assume that its branches are also expressions.
    checkNotNull(
      convertBranches(irWhen.branches, ::convertExpression) {
        condition: Expression,
        trueExpression: Expression,
        falseExpression: Expression?,
        _: SourcePosition ->
        // Kotlinc will always provide an else branch when `when` is used as an expression.
        requireNotNull(falseExpression)

        ConditionalExpression.newBuilder()
          .setTypeDescriptor(environment.getTypeDescriptor(irWhen.type))
          .setConditionExpression(condition)
          .setTrueExpression(trueExpression)
          .setFalseExpression(falseExpression)
          .build()
      }
    ) {
      "Unexpected When without branches"
    }

  private fun convertCall(irCall: IrCall): Expression =
    when {
      irCall.isArraySizeCall -> convertArraySizeCall(irCall)
      irCall.isArrayGetCall -> convertArrayGetCall(irCall)
      irCall.isArraySetCall -> convertArraySetCall(irCall)
      irCall.isArrayOfCall -> convertArrayOfCall(irCall)
      irCall.isArrayOfNullCall -> createNewArray(irCall)
      irCall.isIsArrayOfCall -> convertIsArrayOfCall(irCall)
      irCall.isDataClassArrayMemberHashCode -> convertDataClassArrayMemberCall(irCall, "hashCode")
      irCall.isDataClassArrayMemberToString -> convertDataClassArrayMemberCall(irCall, "toString")
      irCall.isAnyToString -> convertAnyToStringCall(irCall)
      irCall.isCheckNotNullCall -> convertCheckNotNullCall(irCall)
      irCall.isNoWhenBranchMatchedException -> convertNoWhenBranchMatchedException(irCall)
      irCall.isJavaClassPropertyReference -> convertJavaClassPropertyReference(irCall)
      irCall.isKClassJavaPropertyReference ->
        convertKClassJavaPropertyReference(irCall, wrapPrimitives = false)
      irCall.isKClassJavaObjectTypePropertyReference ->
        convertKClassJavaPropertyReference(irCall, wrapPrimitives = true)
      irCall.isRangeToCall -> convertRangeToCall(irCall)
      irCall.isBinaryOperation -> convertBinaryOperation(irCall)
      irCall.isPrefixOperation -> convertPrefixOperation(irCall)
      irCall.isEqualsOperator -> convertEqualsOperator(irCall)
      irCall.isReferenceEqualsOperator -> convertReferenceEqualsOperator(irCall)
      irCall.isIeee754EqualsOperator -> convertIeee754EqualsOperator(irCall)
      else -> convertFunctionCall(irCall)
    }

  private fun convertJavaClassPropertyReference(irCall: IrCall): Expression =
    convertToGetClass(
      requireNotNull(irCall.extensionReceiver),
      getSourcePosition(irCall),
      wrapPrimitives = false,
    )

  private fun convertKClassJavaPropertyReference(
    irCall: IrCall,
    wrapPrimitives: Boolean,
  ): Expression {
    val receiver = irCall.extensionReceiver
    return when (receiver) {
      // CLASS_REFERENCE is a literal class reference on a type, ex: Foo::class.
      is IrClassReference ->
        toTypeLiteral(receiver.classType, getSourcePosition(irCall), wrapPrimitives)
      // GET_CLASS is a literal class reference on an instance, ex: Foo()::class.
      is IrGetClass ->
        convertToGetClass(receiver.argument, getSourcePosition(irCall), wrapPrimitives)
      // Any other receiver cannot be statically analyzed so we'll fall back to a normal function
      // call and let the runtime handle the behavior.
      else -> convertFunctionCall(irCall)
    }
  }

  private fun convertToGetClass(
    receiver: IrExpression,
    sourcePosition: SourcePosition,
    wrapPrimitives: Boolean,
  ): Expression {
    val convertedReceiver = convertExpression(receiver)
    if (convertedReceiver.typeDescriptor.isPrimitive) {
      return MultiExpression.newBuilder()
        .addExpressions(
          convertedReceiver,
          toTypeLiteral(receiver.type, sourcePosition, wrapPrimitives),
        )
        .build()
    }
    return RuntimeMethods.createGetClassMethodCall(convertedReceiver)
  }

  private fun toTypeLiteral(
    irType: IrType,
    sourcePosition: SourcePosition,
    wrapPrimitives: Boolean,
  ): TypeLiteral {
    val typeDescriptor =
      environment.getReferenceTypeDescriptor(irType).run {
        // "Nothing" is a special case as we thread it through the J2CL AST as a stubbed type. In
        // the context of Nothing::class it should be treated Void.
        if (TypeDescriptors.isKotlinNothing(this)) {
          TypeDescriptors.get().javaLangVoid
        } else if (TypeDescriptors.isBoxedType(this) && irType.isPrimitiveType(nullable = false)) {
          if (wrapPrimitives) toBoxedType() else toUnboxedType()
        } else {
          this
        }
      }
    return TypeLiteral(sourcePosition, typeDescriptor)
  }

  private fun convertClassReference(expression: IrClassReference): Expression {
    val sourcePosition = getSourcePosition(expression)
    return RuntimeMethods.createKClassCall(
      toTypeLiteral(expression.classType, sourcePosition, wrapPrimitives = false)
    )
  }

  private fun convertGetClass(expression: IrGetClass): Expression {
    val argument = convertExpression(expression.argument)
    val sourcePosition = getSourcePosition(expression)

    // For primitive argument types we need to create a KClass that wraps a primitive class obj,
    // ex. int.class.
    if (argument.typeDescriptor.isPrimitive) {
      val createKClassCall =
        RuntimeMethods.createKClassCall(
          toTypeLiteral(expression.argument.type, sourcePosition, wrapPrimitives = false)
        )
      // Since we're not getting the class from the result of the argument, construct a
      // MultiExpression that executes the argument. This is to ensure any side effects still occur.
      return MultiExpression.newBuilder().addExpressions(argument, createKClassCall).build()
    }

    return RuntimeMethods.createKClassCall(argument)
  }

  private fun convertArraySizeCall(irCall: IrCall): Expression =
    ArrayLength.newBuilder().setArrayExpression(convertQualifier(irCall)).build()

  private fun convertArrayGetCall(irCall: IrCall): Expression =
    ArrayAccess.newBuilder()
      .setArrayExpression(convertQualifier(irCall))
      .setIndexExpression(convertExpression(irCall.getValueArgument(0)!!))
      .build()

  private fun convertArraySetCall(irCall: IrCall): Expression =
    BinaryExpression.newBuilder()
      .setLeftOperand(
        // the index argument position of Array.get or Array.set is the same. We can reuse
        // convertArrayGetCall() to create the ArrayAccess.
        convertArrayGetCall(irCall)
      )
      .setOperator(BinaryOperator.ASSIGN)
      .setRightOperand(convertExpression(irCall.getValueArgument(1)!!))
      .build()

  private fun convertArrayOfCall(irCall: IrCall): Expression {
    // arrayOf method takes a vararg argument. the vararg argument will be converted to an array
    // literal, so we can replace the call to the arrayOf by the array argument itself.
    check(irCall.valueArgumentsCount == 1)
    return convertExpression(irCall.getArguments()[0])
  }

  private fun convertIsArrayOfCall(irCall: IrCall): Expression =
    // Transforms `array.isArrayOf<String>()` to `array instanceof String[]`
    InstanceOfExpression.newBuilder()
      // isArrayOf is defined as an extension method. The qualifier is the extension receiver.
      .setExpression(convertExpression(requireNotNull(irCall.extensionReceiver)))
      .setTestTypeDescriptor(
        // Type argument of the isArrayOf call is the component type of the array:
        ArrayTypeDescriptor.newBuilder()
          .setComponentTypeDescriptor(
            environment.getTypeDescriptor(requireNotNull(irCall.getTypeArgument(0)))
          )
          .build()
      )
      .setSourcePosition(getSourcePosition(irCall))
      .build()

  private fun convertDataClassArrayMemberCall(irCall: IrCall, methodName: String): Expression {
    val arrayArgument = requireNotNull(irCall.getValueArgument(0))
    val arrayTypeDescriptor =
      if (arrayArgument.type.isPrimitiveArray()) environment.getTypeDescriptor(arrayArgument.type)
      else TypeDescriptors.get().javaLangObjectArray
    val methodDescriptor =
      TypeDescriptors.get().javaUtilArrays.getMethodDescriptor(methodName, arrayTypeDescriptor)
    return MethodCall.Builder.from(methodDescriptor)
      .setArguments(convertExpression(arrayArgument))
      .setSourcePosition(getSourcePosition(irCall))
      .build()
  }

  private fun convertAnyToStringCall(irCall: IrCall) =
    MethodCall.Builder.from(
        TypeDescriptors.get()
          .javaLangString
          .getMethodDescriptor("valueOf", TypeDescriptors.get().javaLangObject)
      )
      .setArguments(convertExpression(requireNotNull(irCall.extensionReceiver)))
      .setSourcePosition(getSourcePosition(irCall))
      .build()

  /** Converts a `a.rangeTo(b)` or `a..b` call. */
  private fun convertRangeToCall(irCall: IrCall): Expression {
    require(irCall.valueArgumentsCount == 1) { "invalid number of arguments" }
    val constructorSymbol = intrinsicMethods.getRangeToConstructor(irCall)
    val methodDescriptor =
      environment.getMethodDescriptor(constructorSymbol.owner, irCall.typeSubstitutionMap)
    return NewInstance.Builder.from(methodDescriptor)
      .setArguments(
        listOf(
          checkNotNull(convertQualifier(irCall)),
          convertExpression(checkNotNull(irCall.getValueArgument(0))),
        )
      )
      .build()
  }

  private fun convertEqualsOperator(irCall: IrCall): Expression {
    val lhs = convertExpression(irCall.getValueArgument(0)!!)
    val rhs = convertExpression(irCall.getValueArgument(1)!!)

    // Kotlin .equals() operator (==) is a null-safe comparison based on "Object.equals". It has the
    // same semantics as the j.u.Objects.equals which we can delegate to. However if we know the
    // "equals" of the compared type has identity semantics, we can short circuit this and use
    // J2CL's `==` directly which is more efficient.
    // TODO(b/235278098): handle primitive == object and primitive == null scenarios.
    if (
      (lhs.typeDescriptor.isPrimitive && rhs.typeDescriptor.isPrimitive) ||
        (!lhs.typeDescriptor.isPrimitive && rhs is NullLiteral) ||
        (!rhs.typeDescriptor.isPrimitive && lhs is NullLiteral) ||
        (hasIdentityEquals(lhs.typeDescriptor) && hasIdentityEquals(rhs.typeDescriptor))
    ) {
      return lhs.infixEquals(rhs)
    }

    return RuntimeMethods.createObjectsEqualsMethodCall(lhs, rhs)
  }

  // There are couple of known types that has equals based on identity equality (==) in J2CL.
  // 1. Any isBoxedTypeAsJsPrimitives cannot override "equals" so they are guaranteed to have
  // correct J2CL '==' implementation for their instances.
  // 2. All enums guaranteed to have identity equality based on JLS.
  private fun hasIdentityEquals(type: TypeDescriptor) =
    TypeDescriptors.isBoxedTypeAsJsPrimitives(type) || type.isEnum

  private fun convertIeee754EqualsOperator(irCall: IrCall): Expression {
    var lhs = convertExpression(irCall.getValueArgument(0)!!)
    var rhs = convertExpression(irCall.getValueArgument(1)!!)

    val floatToNumberMethodDescriptor =
      TypeDescriptors.get()
        .javaLangFloat
        .getMethodDescriptor("toDouble", TypeDescriptors.get().javaLangFloat)
    // This operation is only applicable to floats and doubles, and we take advantage that
    // float, double and Double have exactly the same representation as a number. Therefore, it is
    // only necessary to handle Float, and for that we use a utility method that returns its
    // floating point value or null.
    if (TypeDescriptors.isJavaLangFloat(lhs.typeDescriptor)) {
      lhs = MethodCall.Builder.from(floatToNumberMethodDescriptor).setArguments(lhs).build()
    }

    if (TypeDescriptors.isJavaLangFloat(rhs.typeDescriptor)) {
      rhs = MethodCall.Builder.from(floatToNumberMethodDescriptor).setArguments(rhs).build()
    }

    return RuntimeMethods.createEqualityMethodCall("\$sameNumber", lhs, rhs)
  }

  private fun convertReferenceEqualsOperator(irCall: IrCall): Expression {
    val lhs = convertExpression(irCall.getValueArgument(0)!!)
    var rhs = convertExpression(irCall.getValueArgument(1)!!)
    // In Kotlin === is almost equivalent to Java ==. The only difference is that if the lhs
    // is a reference type, then the rhs is boxed if it is a primitive type. In Java, however, the
    // operation unboxes if either side is a primitive.

    if (!lhs.typeDescriptor.isPrimitive && rhs.typeDescriptor.isPrimitive) {
      // Boxing semantics due to the lhs being a reference type, so force the boxing using a cast
      // to j.l.Object.
      rhs =
        CastExpression.newBuilder()
          .setCastTypeDescriptor(TypeDescriptors.get().javaLangObject)
          .setExpression(rhs)
          .build()
    }

    return lhs.infixEquals(rhs)
  }

  private fun convertCheckNotNullCall(irCall: IrCall): Expression {
    require(irCall.getArguments().size == 1)

    val argumentExpression = convertExpression(irCall.getArguments()[0])

    return if (argumentExpression.typeDescriptor.isPrimitive) {
      // Do not insert a checkNotNull call on primitives.
      argumentExpression
    } else {
      argumentExpression.postfixNotNullAssertion()
    }
  }

  private fun convertNoWhenBranchMatchedException(irCall: IrCall): Expression {
    require(irCall.valueArgumentsCount == 0) {
      "throwNoWhenBranchMatchedException should have no arguments"
    }
    return MethodCall.Builder.from(
        TypeDescriptors.get()
          .kotlinJvmInternalIntrinsics!!
          .getMethodDescriptor("throwNoWhenBranchMatchedException")
      )
      .setSourcePosition(getSourcePosition(irCall))
      .build()
  }

  private fun convertPrefixOperation(irCall: IrCall): Expression {
    val prefixOperator = requireNotNull(intrinsicMethods.getPrefixOperator(irCall.symbol))

    // Intrinsic prefix operators are functions that might come in two flavors, either the
    // operand is the receiver of a function with no arguments, or it is static function with
    // just one argument.
    require(irCall.valueArgumentsCount in 0..1) { "invalid number of arguments" }
    require(irCall.valueArgumentsCount == 1 || irCall.dispatchReceiver != null)

    val operand = convertQualifier(irCall) ?: convertExpression(irCall.getValueArgument(0)!!)

    // Create the appropriate expression with the same semantic of the intrinsic call.
    if (prefixOperator.hasSideEffect()) {
      // Side effect operators needs to be converted as their side-effect free versions to be
      // consistent with the treatment of the operator overriding semantics.
      // Note that binary expressions have widening semantics for some types (e.g. addition on
      // bytes returns int) hence cast the result to the operand's primitive type to avoid these
      // widening semantics, and preserve the original meaning.

      val primitiveType = operand.typeDescriptor.toUnboxedType()
      return CastExpression.newBuilder()
        .setExpression(
          BinaryExpression.newBuilder()
            .setLeftOperand(operand)
            .setOperator(prefixOperator.underlyingBinaryOperator)
            .setRightOperand(NumberLiteral(primitiveType, 1))
            .build()
        )
        .setCastTypeDescriptor(primitiveType)
        .build()
    }
    return PrefixExpression.newBuilder().setOperand(operand).setOperator(prefixOperator).build()
  }

  private fun convertBinaryOperation(irCall: IrCall): Expression {
    val binaryOperator = requireNotNull(intrinsicMethods.getBinaryOperator(irCall.symbol))

    // Intrinsic binary operators come in two flavors; either the lhs is the receiver and the rhs
    // is the only argument of the function or the function has no receiver and the lhs and rhs
    // are its arguments (e.g. comparison operators).
    require(irCall.valueArgumentsCount in 1..2) { "invalid number of arguments" }
    val receiver: IrExpression? = irCall.dispatchReceiver ?: irCall.extensionReceiver
    require(irCall.valueArgumentsCount == 2 || receiver != null)

    var argumentIndex = 0

    val lhs = convertExpression(receiver ?: irCall.getValueArgument(argumentIndex++)!!)
    val rhs = convertExpression(irCall.getValueArgument(argumentIndex)!!)

    // Create the appropriate expression with the same semantic of the intrinsic call.
    return BinaryExpression.newBuilder()
      .setLeftOperand(lhs)
      .setOperator(binaryOperator)
      .setRightOperand(rhs)
      .build()
  }

  private fun convertFunctionCall(irCall: IrCall): Expression {
    return convertFunctionAccessExpression(irCall)
  }

  private fun convertConstructorCall(
    irCall: IrFunctionAccessExpression,
    anonymousInnerClass: IrClass? = null,
  ): Expression {
    if (irCall is IrConstructorCall && irCall.isNewArrayCall) {
      return createNewArray(irCall)
    }
    return NewInstance.Builder.from(
        environment.getMethodDescriptor(irCall.symbol.owner, irCall.typeSubstitutionMap)
      )
      .setQualifier(convertQualifier(irCall))
      .setArguments(convertExpressions(irCall.getArguments()))
      .setAnonymousInnerClass(anonymousInnerClass?.let { convertClass(it) })
      .build()
  }

  private fun convertEnumConstructorCall(irEnumConstructorCall: IrEnumConstructorCall): Expression =
    // IrEnumConstructorCall nodes can represent an instantiation or a super call depending on
    // whether they appear as the initializer of the enum field or inside the constructor of an
    // enum subclass.
    if (currentType.isEnum) {
      // The initializer of the entry is a direct instantiation using the constructor.
      convertConstructorCall(irEnumConstructorCall)
    } else {
      // This is the super call in the constructor of the anonymous subclass; it needs to be treated
      // as a call and not an instantiation.
      convertFunctionAccessExpression(irEnumConstructorCall)
    }

  private fun createNewArray(irCall: IrFunctionAccessExpression): Expression {
    val arrayTypeDescriptor = environment.getTypeDescriptor(irCall.type) as ArrayTypeDescriptor
    val size = convertExpression(requireNotNull(irCall.getValueArgument(0)))
    // In Kotlin, there is no way to init all the dimensions of a multi-dimensional array in the
    // same expression.
    // Ex: in java you can do
    //    Object[][] foo = new Object[1][2]
    // in kotlin, it requires two array ctor calls:
    //    val foo: Array<Array<Object>> = Array(1) { Array(2) }
    // Our AST expects NewArray nodes to provide an expression for each dimension in the array type,
    // hence the missing dimensions are padded with null.
    val dimensionExpressions =
      mutableListOf(size).also {
        it.addAll(AstUtils.createListOfNullValues(arrayTypeDescriptor.dimensions - 1))
      }

    return NewArray.newBuilder()
      .setDimensionExpressions(dimensionExpressions)
      .setTypeDescriptor(arrayTypeDescriptor)
      .apply {
        if (irCall.valueArgumentsCount == 2) {
          setInitializer(convertExpression(irCall.getValueArgument(1)!!))
        }
      }
      .build()
  }

  private fun convertGetField(irGetField: IrGetField): FieldAccess =
    convertFieldAccessExpression(irGetField)

  private fun convertSetField(irSetField: IrSetField): Expression =
    BinaryExpression.newBuilder()
      .setOperator(BinaryOperator.ASSIGN)
      .setLeftOperand(convertFieldAccessExpression(irSetField))
      .setRightOperand(convertExpression(irSetField.value))
      .build()

  private fun convertFieldAccessExpression(fieldAccess: IrFieldAccessExpression): FieldAccess =
    FieldAccess.Builder.from(
        environment.getFieldDescriptor(
          fieldAccess.symbol.owner,
          fieldAccess.receiver?.type?.typeSubstitutionMap ?: mapOf(),
        )
      )
      .setQualifier(convertQualifier(fieldAccess))
      .build()

  private fun convertFunctionAccessExpression(
    functionAccess: IrFunctionAccessExpression
  ): MethodCall {
    val callee = functionAccess.symbol.owner
    var typeSubstitutionMap = functionAccess.typeSubstitutionMap

    // `JsArrays.create()` is coming from Java code. Any call to this method is seen by the Kotlin
    // compiler as returning a `JsArray` of nullable type. That brings javascript type mismatch
    // errors later on.
    // Normally, J2CL JS is not nullability checked. `JsArray` is special in the sense that it's a
    // native type targeting JavaScript Array. In this case, it seems JSC does not skip the
    // nullability check.
    // This code can be removed if gwt/corp/collection is transpiled to Koltin or if JSC is fixed
    // to not nullability check this case for J2CL.
    // TODO(b/316883718): Remove this code when gwt/corp/collection is transpiled to Kotlin.
    // TODO(b/317390851): Remove this hack if JSC does not trigger nullability check errors on
    // Array.
    if (
      callee.fqNameWhenAvailable?.asString() == "com.google.gwt.corp.collections.JsArrays.create"
    ) {
      check(callee.typeParameters.size == 1)
      val typeParameter = callee.typeParameters[0].symbol
      val typeArgument = typeSubstitutionMap[typeParameter]
      if (typeArgument is IrTypeProjection) {
        typeSubstitutionMap =
          mapOf(
            typeParameter to
              makeTypeProjection(typeArgument.type.makeNotNull(), typeArgument.variance)
          )
      }
    }

    val qualifier = convertQualifier(functionAccess)
    val isStaticDispatch = qualifier !is SuperReference && functionAccess.isSuperCall
    return MethodCall.Builder.from(
        adjustEnumConstructorDescriptor(
          environment.getMethodDescriptor(callee, typeSubstitutionMap),
          functionAccess,
        )
      )
      .setQualifier(qualifier)
      .setArguments(convertExpressions(functionAccess.getArguments()))
      .setStaticDispatch(isStaticDispatch)
      .setSourcePosition(getSourcePosition(functionAccess))
      .build()
  }

  private fun adjustEnumConstructorDescriptor(
    methodDescriptor: MethodDescriptor,
    functionAccess: IrFunctionAccessExpression,
  ): MethodDescriptor {
    if (
      !methodDescriptor.isConstructor ||
        !methodDescriptor.isMemberOf(TypeDescriptors.get().javaLangEnum)
    ) {
      return methodDescriptor
    }
    // Fix inconsistencies in calls to java.lang.Enum constructor calls. Enum constructor has 2
    // implicit parameters (name and ordinal) that are added by a normalization pass. This removes
    // the parameter definition from the descriptor so that they are consistent.
    check(functionAccess.getValueArgument(0) == null && functionAccess.getValueArgument(1) == null)

    return MethodDescriptor.Builder.from(methodDescriptor)
      .setParameterDescriptors(listOf())
      .makeDeclaration()
      .build()
  }

  private fun convertGetValue(irGetValue: IrGetValue): Expression =
    convertValueAccessExpression(irGetValue)

  private fun convertSetValue(irSetValue: IrSetValue): Expression {
    val variableReference = convertValueAccessExpression(irSetValue)

    return BinaryExpression.newBuilder()
      .setOperator(BinaryOperator.ASSIGN)
      .setLeftOperand(variableReference)
      .setRightOperand(convertExpression(irSetValue.value))
      .build()
  }

  private fun convertValueAccessExpression(irValueAccess: IrValueAccessExpression): Expression {
    val target = irValueAccess.symbol.owner

    // Check first if this is a reference to a known defined parameter or variable.
    val existingVariable = variableBySymbol[target.symbol]
    if (existingVariable != null) {
      return existingVariable.createReference()
    }

    // This check on the name is only safe after the previous check because extension method
    // receiver parameters have 'this' as name but their symbol are set in the variableBySymbol map.
    // Kotlinc isn't always consistent with how they name the "this" receiver unforunately so
    // we check for multiple variants here.
    if (target.name === SpecialNames.THIS || target.name == "this".synthesizedName) {
      return ThisReference(environment.getDeclaredTypeDescriptor(irValueAccess.type))
    }

    throw IllegalStateException("Unknown value ${target.render()}")
  }

  private fun convertConstant(irConst: IrConst<*>): Expression =
    environment.getTypeDescriptor(irConst.type).let { typeDescriptor ->
      if (irConst.kind == IrConstKind.Null) typeDescriptor.nullValue
      else Literal.fromValue(irConst.value, typeDescriptor)
    }

  private fun convertTypeOperatorCall(irTypeOperatorCall: IrTypeOperatorCall): Expression {
    return when (irTypeOperatorCall.operator) {
      // SAM_CONVERSIONS are the operation that give the actual functional interface type
      // to function expressions (and objects that can be typed as functions).
      IrTypeOperator.SAM_CONVERSION -> convertSamConversion(irTypeOperatorCall)
      IrTypeOperator.INSTANCEOF ->
        createInstanceOfExpression(
          irTypeOperatorCall.argument,
          irTypeOperatorCall.typeOperand,
          getSourcePosition(irTypeOperatorCall),
        )
      IrTypeOperator.CAST,
      IrTypeOperator.IMPLICIT_CAST -> {
        val expression = convertExpression(irTypeOperatorCall.argument)
        val testTypeDescriptor = environment.getTypeDescriptor(irTypeOperatorCall.typeOperand)
        if (
          irTypeOperatorCall.operator == IrTypeOperator.IMPLICIT_CAST &&
            !testTypeDescriptor.isPrimitive &&
            !expression.typeDescriptor.isPrimitive
        ) {
          // An implicit cast guarantees that the type of the expression is already checked.
          // However, the boxing/unboxing conversion has not happened yet and in those cases
          // the cast can not be replaced by a JsDocCastExpression.
          JsDocCastExpression.newBuilder()
            .setExpression(expression)
            .setCastTypeDescriptor(testTypeDescriptor)
            .build()
        } else {
          CastExpression.newBuilder()
            .setExpression(expression)
            .setCastTypeDescriptor(testTypeDescriptor)
            .build()
        }
      }
      IrTypeOperator.SAFE_CAST ->
        throw IllegalStateException("rTypeOperator.SAFE_CAST expressions should have been lowered")
      // TODO(b/274450717): Implement missing types and make this when statement exhaustive.
      else ->
        throw IllegalStateException(
          "Unhandled IrTypeOperator.${irTypeOperatorCall.operator.name}:\n${irTypeOperatorCall.dump()}"
        )
    }
  }

  private fun createInstanceOfExpression(
    expression: IrExpression,
    testType: IrType,
    sourcePosition: SourcePosition,
  ): Expression {
    check(!testType.isNullable())
    val expressionTypeDescriptor = environment.getTypeDescriptor(expression.type)
    // For consistency our InstanceOfExpression always has a nullable typedescriptor.
    // TODO(b/273768176):  Only the first type in a type intersection is currently honored.
    val typeDescriptor = environment.getReferenceTypeDescriptor(testType.makeNullable())
    val testTypeDescriptor =
      if (typeDescriptor.isTypeVariable) typeDescriptor.toRawTypeDescriptor() else typeDescriptor

    if (expressionTypeDescriptor.isPrimitive) {
      // Kotlin only allows non-nullable primitive instanceOf type if the type is assignable, hence
      // only if the instanceOf is true. `1 is Int` is valid but `1 is Double` is rejected by the
      // compiler. However after inlining inline function with reified type parameters, the program
      // can contain this kind of instanceOf expression.
      // Ex:
      // inline fun <reified T> instanceOf(o: Any) = o is T
      // The following call site:
      // val b = instanceOf<String>(1)
      // is inlined as:
      // val b = 1 is String
      return BooleanLiteral.get(
        expressionTypeDescriptor.toBoxedType().isAssignableTo(testTypeDescriptor)
      )
    }
    return InstanceOfExpression.newBuilder()
      .setExpression(convertExpression(expression))
      .setTestTypeDescriptor(testTypeDescriptor)
      .setSourcePosition(sourcePosition)
      .build()
  }

  private fun convertSamConversion(irTypeOperatorCall: IrTypeOperatorCall): Expression {
    val expression = irTypeOperatorCall.unfoldExpression()
    val functionalTypeDescriptor = environment.getDeclaredTypeDescriptor(irTypeOperatorCall.type)
    return when (expression) {
      is IrFunctionReference -> createFunctionExpression(functionalTypeDescriptor, expression)
      is IrPropertyReference ->
        createAccessorReference(
          functionalTypeDescriptor,
          expression,
          convertQualifier(expression),
          expression.getter,
        )
      is IrFunctionExpression -> createFunctionExpression(functionalTypeDescriptor, expression)
      else ->
        // TODO(b/225955286): Implement conversion functionality from things that are not lambdas.
        throw IllegalStateException("Unsupported SAM conversion ${irTypeOperatorCall.dump()}")
    }
  }

  private fun createFunctionExpression(
    functionalTypeDescriptor: DeclaredTypeDescriptor,
    irFunctionReference: IrFunctionReference,
  ): Expression {
    val referencedMethodDescriptor =
      environment.getMethodDescriptor(
        irFunctionReference.symbol.owner,
        irFunctionReference.typeSubstitutionMap,
      )

    return MethodReference.newBuilder()
      .setTypeDescriptor(functionalTypeDescriptor)
      .setReferencedMethodDescriptor(referencedMethodDescriptor)
      .setInterfaceMethodDescriptor(functionalTypeDescriptor.getSingleAbstractMethodDescriptor())
      .setQualifier(convertQualifier(irFunctionReference))
      .setSourcePosition(getSourcePosition(irFunctionReference))
      .build()
  }

  private fun createAccessorReference(
    functionalTypeDescriptor: DeclaredTypeDescriptor,
    irPropertyReference: IrCallableReference<*>,
    propertyReferenceQualifier: Expression?,
    accessorFunctionSymbol: IrFunctionSymbol?,
  ): Expression {
    // Immutable properties do not have setter.
    if (accessorFunctionSymbol == null) {
      return functionalTypeDescriptor.nullValue
    }
    val accessorFunction = accessorFunctionSymbol.owner

    return MethodReference.newBuilder()
      .setTypeDescriptor(functionalTypeDescriptor)
      .setReferencedMethodDescriptor(
        environment.getMethodDescriptor(
          accessorFunction,
          irPropertyReference.getTypeSubstitutionMap(accessorFunction),
        )
      )
      .setInterfaceMethodDescriptor(functionalTypeDescriptor.singleAbstractMethodDescriptor)
      .setQualifier(propertyReferenceQualifier)
      .setSourcePosition(getSourcePosition(irPropertyReference))
      .build()
  }

  private fun createFunctionExpression(
    typeDescriptor: TypeDescriptor,
    irFunctionExpression: IrFunctionExpression,
  ): FunctionExpression {
    val irFunction = irFunctionExpression.function
    val parameters = irFunction.getParameters().map(this::createVariable)
    val body =
      irFunction.body?.let { convertBody(it) }
        ?: Block.newBuilder().setSourcePosition(getSourcePosition(irFunction)).build()

    return FunctionExpression.newBuilder()
      .setTypeDescriptor(typeDescriptor)
      .setParameters(parameters)
      .setStatements(body.statements)
      .setSourcePosition(getSourcePosition(irFunction))
      .build()
  }

  private fun convertGetEnumValue(irGetEnumValue: IrGetEnumValue): Expression =
    FieldAccess.newBuilder()
      .setSourcePosition(getSourcePosition(irGetEnumValue))
      .setTarget(environment.getDeclaredFieldDescriptor(irGetEnumValue.symbol.owner))
      .build()

  private fun convertVararg(vararg: IrVararg): Expression =
    ArrayLiteral(
      environment.getTypeDescriptor(vararg.type) as ArrayTypeDescriptor,
      vararg.elements.map(::convertVarargElement),
    )

  private fun convertVarargElement(varargElement: IrVarargElement): Expression =
    when (varargElement) {
      is IrSpreadElement ->
        PrefixExpression.newBuilder()
          .setOperator(PrefixOperator.SPREAD)
          .setOperand(convertExpression(varargElement.expression))
          .build()
      is IrExpression -> convertExpression(varargElement)
      else ->
        throw IllegalStateException(
          "Invalid vararg element after lowering: ${varargElement.render()}"
        )
    }

  private fun convertFunctionReference(irExpression: IrFunctionReference): Expression {
    // Function references are resolved in the IR as fictitious `KFunction{N}` interfaces that do
    // not exist at runtime (`N` being the arity of the referenced function). Kotlin/JVM maps any
    // `KFunction{N}` type to the existing interface `KFunction`. Then it introduces a cast to
    // `Function{N}` at `invoke()` function call sites as this function is specific to that
    // interface.  For more information, please refer to
    // https://github.com/JetBrains/kotlin/blob/master/spec-docs/function-types.md#how-this-will-help-reflection
    //
    // In J2CL, we are only supporting the api of `Function{N}` for now. To avoid casts at invoke()
    // function call sites, we will type our MethodReference as `Function{N}`. Any call to the
    // `KFunction` API will be rejected by the compiler.
    //
    // Note about varargs and function reference: A function with varargs can be used in a context
    // of a function type without varargs. In this case, Kotlin compiler creates an extra function
    // adapter and the reference we see here is the reference to the adapter function. The type
    // of the `IrFunctionReference` is directly `Function{N}`
    // ex:
    // ```
    //  fun foo(varargs String s): String = s.joinToString()
    //  fun acceptFoo(foo: (String, String) -> String): String {...}
    //  var fooRef = ::foo // The type of the reference is KFunction1<Array<String>, String>>
    //  acceptFoo(::foo) // the type of reference in this ctx is Function2<String, String, String>
    //  ```
    val functionNType: IrSimpleType
    if (irExpression.isAdaptedFunctionReference) {
      check(irExpression.type.isFunction())
      functionNType = irExpression.type as IrSimpleType
    } else {
      check(irExpression.type.isKFunction())
      val kFunctionNType = irExpression.type as IrSimpleType

      // In the Kotlin type system, `KFunction{N}` does not directly extend `Function{N}` but the
      // `KFunction{N}.invoke()` is declared as overriding `Function{N}.invoke()`. This is possible
      // because `KFunction{N}` is a synthetic interfaces.
      // The simplest way to find the Function{N} type is to look at the enclosing class of the
      // single overridden function of `KFunction{N}.invoke` that must be `Function{N}.invoke`.
      functionNType =
        checkNotNull(kFunctionNType.classOrNull)
          .owner
          .findFunctionByName("invoke")
          .overriddenSymbols
          .single()
          .owner
          .parentAsClass
          .symbol
          // specialize Function{N} with the arguments of KFunction{N}
          .typeWithArguments(kFunctionNType.arguments)
      check(functionNType.isFunction())
    }

    val functionNTypeDescriptor =
      environment.getReferenceTypeDescriptorForFunctionReference(functionNType)

    return MethodReference.newBuilder()
      .setTypeDescriptor(functionNTypeDescriptor)
      .setReferencedMethodDescriptor(
        environment.getMethodDescriptor(irExpression.symbol.owner, irExpression.typeSubstitutionMap)
      )
      .setInterfaceMethodDescriptor(
        // `Function{N}` are considered as fun interfaces by J2CL and Lambda adaptor are generated.
        functionNTypeDescriptor.singleAbstractMethodDescriptor
      )
      .setQualifier(convertQualifier(irExpression))
      .setSourcePosition(getSourcePosition(irExpression))
      .build()
  }

  private fun convertFunctionExpression(irExpression: IrFunctionExpression) =
    createFunctionExpression(environment.getDeclaredTypeDescriptor(irExpression.type), irExpression)

  private fun convertPropertyReference(irExpression: IrPropertyReference): Expression {
    val propertyReferenceType = irExpression.type as IrSimpleType
    // We support two different kinds of property references:
    // - reference to a property without receiver (top level property) or has the receiver bound to
    //   it (a value property reference: `aReference::aProperty`).
    // - reference to a property which take the receiver as a parameter (class property reference
    //   `MyClass::aProperty`)
    val propertyReferenceTypeDescriptor =
      when (propertyReferenceType.arguments.size) {
        1 -> TypeDescriptors.get().kotlinJvmInternalMutableKProperty0Impl!!
        2 -> TypeDescriptors.get().kotlinJvmInternalMutableKProperty1Impl!!
        // Note: There is a KProperty2<D, E, V> interface used to represent a reference to a
        // property which takes two receivers as parameters (e.g: an extension property defined in
        // another class). This kind of property cannot be directly referenced in a user code
        // through the `::` operator. The user needs to use some reflection api on the class itself
        // to get a reference to these properties. These reflection apis not being supported by
        // J2CL, we don't need to support that case.
        else -> throw IllegalStateException("Unsupported property reference.")
      }

    // As we mapped a Kotlin type to our own implementation, we need to ensure to specialize the
    // right type variable.
    check(
      propertyReferenceTypeDescriptor.typeDeclaration.typeParameterDescriptors.size ==
        propertyReferenceType.arguments.size
    )
    val j2clSubstitutionMap =
      propertyReferenceType.arguments
        .mapIndexed { index, typeArgument ->
          propertyReferenceTypeDescriptor.typeDeclaration.typeParameterDescriptors[index] to
            environment.getReferenceTypeDescriptorForTypeArgument(typeArgument)
        }
        .toMap()
    val kMutablePropertyCtor =
      propertyReferenceTypeDescriptor.singleConstructor.specializeTypeVariables(j2clSubstitutionMap)

    check(kMutablePropertyCtor.parameterTypeDescriptors.size == 2)

    val expressions = mutableListOf<Expression>()

    val propertyQualifierExpression = convertQualifier(irExpression)
    // If there is a qualifier, we need to evaluate it once, store it into a variable to reuse it
    // in the setter and getter.
    val propertyQualifierVariable =
      propertyQualifierExpression?.let { qualifierExpression ->
        Variable.newBuilder()
          .setName("\$propertyReferenceQualifier")
          .setTypeDescriptor(qualifierExpression.typeDescriptor)
          .setFinal(true)
          .build()
      }

    if (propertyQualifierExpression != null) {
      expressions.add(
        VariableDeclarationExpression.newBuilder()
          .addVariableDeclaration(propertyQualifierVariable, propertyQualifierExpression)
          .build()
      )
    }

    expressions.add(
      // new MutableKPropertyXImpl(nameOfTheProperty, getterAsLamdba, setterAsLambdaOrNull)
      NewInstance.newBuilder()
        .setTarget(kMutablePropertyCtor)
        .setArguments(
          createAccessorReference(
            kMutablePropertyCtor.parameterTypeDescriptors[0] as DeclaredTypeDescriptor,
            irExpression,
            propertyQualifierVariable?.createReference(),
            irExpression.getter,
          ),
          createAccessorReference(
            kMutablePropertyCtor.parameterTypeDescriptors[1] as DeclaredTypeDescriptor,
            irExpression,
            propertyQualifierVariable?.createReference(),
            irExpression.setter,
          ),
        )
        .build()
    )

    return MultiExpression.newBuilder().addExpressions(expressions).build()
  }

  private fun convertLocalDelegatedPropertyReference(
    irExpression: IrLocalDelegatedPropertyReference
  ): Expression {
    val variableReferenceType = irExpression.type as IrSimpleType
    val localVariableKPropertyDescriptor =
      TypeDescriptors.get().kotlinJvmInternalLocalVariableKPropertyImpl!!
    val j2clSubstitutionMap =
      variableReferenceType.arguments
        .mapIndexed { index, typeArgument ->
          localVariableKPropertyDescriptor.typeDeclaration.typeParameterDescriptors[index] to
            environment.getReferenceTypeDescriptorForTypeArgument(typeArgument)
        }
        .toMap()
    val kMutablePropertyCtor =
      localVariableKPropertyDescriptor.singleConstructor.specializeTypeVariables(
        j2clSubstitutionMap
      )
    return NewInstance.newBuilder().setTarget(kMutablePropertyCtor).build()
  }

  private fun convertQualifier(fieldAccess: IrFieldAccessExpression): Expression? =
    convertQualifier(fieldAccess.receiver, fieldAccess.superQualifierSymbol)

  private fun convertQualifier(functionAccess: IrFunctionAccessExpression): Expression? =
    convertQualifier(
      functionAccess.dispatchReceiver,
      (functionAccess as? IrCall)?.superQualifierSymbol,
    )

  private fun convertQualifier(memberAccess: IrMemberAccessExpression<*>): Expression? =
    convertQualifier(
      memberAccess.dispatchReceiver ?: memberAccess.extensionReceiver,
      superQualifierSymbol = null,
    )

  private fun convertQualifier(
    receiver: IrExpression?,
    superQualifierSymbol: IrClassSymbol?,
  ): Expression? {
    val qualifier = receiver?.let { convertExpression(receiver) }

    // Kotlin has 2 properties in a call to model the receiver. One is the dispatchReceiver which is
    // a regular expression that acts as the qualifier and the second is the superQualifierSymbol
    // that represents a super class declaration. When superQualifierSymbol is set and the receiver
    // is `this`, it means that we invoke the member defined on this super class.
    return if (qualifier is ThisReference && superQualifierSymbol != null) {
      SuperReference(qualifier.typeDescriptor, /* isQualified= */ true)
    } else {
      qualifier
    }
  }

  private fun convertVariableDeclaration(irVariable: IrVariable): VariableDeclarationExpression =
    convertVariableDeclarations(listOf(irVariable))

  private fun convertVariableDeclarations(
    irVariables: List<IrVariable>
  ): VariableDeclarationExpression {

    return VariableDeclarationExpression.newBuilder()
      .addVariableDeclarationFragments(
        irVariables.map { irVariable ->
          val initializer = irVariable.initializer
          val initializerExpression =
            if (initializer != null) {
              convertExpression(initializer)
            } else null
          VariableDeclarationFragment.newBuilder()
            .setVariable(createVariable(irVariable))
            .setInitializer(initializerExpression)
            .build()
        }
      )
      .build()
  }

  private fun createVariable(irValueDeclaration: IrValueDeclaration): Variable {
    val variable =
      Variable.newBuilder()
        .setName(irValueDeclaration.javaName)
        .setTypeDescriptor(environment.getTypeDescriptor(irValueDeclaration.type))
        .setParameter(irValueDeclaration is IrValueParameter)
        .setFinal(irValueDeclaration is IrVariable && !irValueDeclaration.isVar)
        .setSourcePosition(
          getNameSourcePosition(irValueDeclaration, irValueDeclaration.name.toString())
        )
        .build()
    variableBySymbol[irValueDeclaration.symbol] = variable
    return variable
  }

  private fun getNameSourcePosition(irElement: IrElement, name: String? = null): SourcePosition =
    irElement.getNameSourcePosition(currentIrFile, name)

  private fun getSourcePosition(irElement: IrElement): SourcePosition =
    irElement.getSourcePosition(currentIrFile)

  private val IrCall.isBinaryOperation: Boolean
    get() = intrinsicMethods.isBinaryOperation(this)

  private val IrCall.isPrefixOperation: Boolean
    get() = intrinsicMethods.isPrefixOperation(this)

  private val IrConstructorCall.isNewArrayCall: Boolean
    get() = intrinsicMethods.isNewArray(this)

  private val IrCall.isArraySizeCall: Boolean
    get() = intrinsicMethods.isArraySize(this)

  private val IrCall.isArrayGetCall: Boolean
    get() = intrinsicMethods.isArrayGet(this)

  private val IrCall.isArraySetCall: Boolean
    get() = intrinsicMethods.isArraySet(this)

  private val IrCall.isArrayOfNullCall: Boolean
    get() = intrinsicMethods.isArrayOfNull(this)

  private val IrCall.isArrayOfCall: Boolean
    get() = intrinsicMethods.isArrayOf(this)

  private val IrCall.isIsArrayOfCall: Boolean
    get() = intrinsicMethods.isIsArrayOf(this)

  private val IrCall.isDataClassArrayMemberToString: Boolean
    get() = intrinsicMethods.isDataClassArrayMemberToString(this)

  private val IrCall.isDataClassArrayMemberHashCode: Boolean
    get() = intrinsicMethods.isDataClassArrayMemberHashCode(this)

  private val IrCall.isAnyToString: Boolean
    get() = intrinsicMethods.isAnyToString(this)

  private val IrCall.isRangeToCall: Boolean
    get() = intrinsicMethods.isRangeTo(this)

  private val IrCall.isEqualsOperator: Boolean
    get() = intrinsicMethods.isEqualsOperator(this)

  private val IrCall.isReferenceEqualsOperator: Boolean
    get() = intrinsicMethods.isReferenceEqualsOperator(this)

  private val IrCall.isIeee754EqualsOperator: Boolean
    get() = intrinsicMethods.isIeee754EqualsOperator(this)

  private val IrCall.isCheckNotNullCall: Boolean
    get() = intrinsicMethods.isCheckNotNull(this)

  private val IrCall.isNoWhenBranchMatchedException: Boolean
    get() = intrinsicMethods.isNoWhenBranchMatchedException(this)

  private val IrCall.isJavaClassPropertyReference: Boolean
    get() = intrinsicMethods.isJavaClassProperty(this)

  private val IrCall.isKClassJavaPropertyReference: Boolean
    get() = intrinsicMethods.isKClassJavaProperty(this)

  private val IrCall.isKClassJavaObjectTypePropertyReference: Boolean
    get() = intrinsicMethods.isKClassJavaObjectTypeProperty(this)
}
