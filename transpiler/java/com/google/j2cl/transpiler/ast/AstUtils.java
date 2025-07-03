/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.FieldDescriptor.FieldOrigin;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** Utility functions to manipulate J2CL AST. */
public final class AstUtils {

  /** Returns the loadModules method descriptor for a particular type */
  public static MethodDescriptor getLoadModulesDescriptor(DeclaredTypeDescriptor typeDescriptor) {
    return MethodDescriptor.newBuilder()
        .setStatic(true)
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setName(MethodDescriptor.LOAD_MODULES_METHOD_NAME)
        .setOrigin(MethodOrigin.SYNTHETIC_CLASS_INITIALIZER)
        .setOriginalJsInfo(typeDescriptor.isNative() ? JsInfo.RAW_OVERLAY : JsInfo.RAW)
        .build();
  }

  /** Create default constructor MethodDescriptor. */
  public static MethodDescriptor createImplicitConstructorDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor) {
    JsInfo jsInfo =
        isImplicitJsConstructor(enclosingTypeDescriptor.getTypeDeclaration())
            ? JsInfo.newBuilder().setJsMemberType(JsMemberType.CONSTRUCTOR).build()
            : JsInfo.NONE;
    return MethodDescriptor.newBuilder()
        .setVisibility(
            getImplicitConstructorVisibility(enclosingTypeDescriptor.getTypeDeclaration()))
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setConstructor(true)
        .setOriginalJsInfo(jsInfo)
        .build();
  }

  private static Visibility getImplicitConstructorVisibility(TypeDeclaration typeDeclaration) {
    return typeDeclaration.isEnum() ? Visibility.PRIVATE : typeDeclaration.getVisibility();
  }

  /** Return true if the synthetic implicit default constructor is a JsConstructor. */
  private static boolean isImplicitJsConstructor(TypeDeclaration typeDeclaration) {
    return typeDeclaration.isJsType()
        && (typeDeclaration.isNative()
            || getImplicitConstructorVisibility(typeDeclaration).isPublic());
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor, or
   * returns null if it does not have one.
   */
  @Nullable
  public static ExpressionStatement getConstructorInvocationStatement(Method method) {
    for (Statement statement : method.getBody().getStatements()) {
      if (isConstructorInvocationStatement(statement)) {
        return (ExpressionStatement) statement;
      }
    }
    return null;
  }

  /** Returns true if {@code statement} is a constructor invocation statement. */
  public static boolean isConstructorInvocationStatement(Statement statement) {
    return statement instanceof ExpressionStatement expressionStatement
        && expressionStatement.getExpression() instanceof MethodCall methodCall
        && methodCall.getTarget().isConstructor();
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor, or
   * returns null if it does not have one.
   */
  @Nullable
  public static MethodCall getConstructorInvocation(Method method) {
    ExpressionStatement statement = getConstructorInvocationStatement(method);
    if (statement == null) {
      return null;
    }
    checkArgument(statement.getExpression() instanceof MethodCall);
    MethodCall methodCall = (MethodCall) statement.getExpression();
    checkArgument(methodCall.getTarget().isConstructor());
    return methodCall;
  }

  /** Returns whether the specified constructor has a this() call. */
  public static boolean hasThisCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && constructorInvocation.getTarget().inSameTypeAs(method.getDescriptor());
  }

  /** Returns whether the specified constructor has a super() call. */
  public static boolean hasSuperCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && !constructorInvocation.getTarget().inSameTypeAs(method.getDescriptor());
  }

  /** Returns whether the specified constructor has a this() or a super() call. */
  public static boolean hasConstructorInvocation(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null;
  }

  /**
   * Returns the superclass constructor method descriptor this constructor delegates to if any or
   * {@code null} otherwise.
   */
  @Nullable
  public static MethodDescriptor getDelegatedSuperConstructorDescriptor(Method method) {
    checkArgument(method.isConstructor());
    if (!hasConstructorInvocation(method)) {
      return method
          .getDescriptor()
          .getEnclosingTypeDescriptor()
          .getSuperTypeDescriptor()
          .getDefaultConstructorMethodDescriptor();
    }
    MethodDescriptor delegatedMethodDescriptor = getConstructorInvocation(method).getTarget();

    return delegatedMethodDescriptor.inSameTypeAs(method.getDescriptor())
        ? null
        : delegatedMethodDescriptor;
  }

  /**
   * Creates forwarding method {@code fromMethodDescriptor} that delegates to {@code
   * toMethodDescriptor}, e.g.
   *
   * <p>fromMethodDescriptor (args) { return this.toMethodDescriptor(args); }
   */
  public static Method createForwardingMethod(
      SourcePosition sourcePosition,
      Expression qualifier,
      MethodDescriptor fromMethodDescriptor,
      MethodDescriptor toMethodDescriptor,
      String jsDocDescription) {
    List<Variable> parameters =
        createParameterVariables(fromMethodDescriptor.getParameterTypeDescriptors());

    Statement statement =
        createForwardingStatement(
            sourcePosition,
            qualifier,
            toMethodDescriptor,
            /* isStaticDispatch= */ false,
            parameters.stream().map(Variable::createReference).collect(toImmutableList()),
            fromMethodDescriptor.getReturnTypeDescriptor());
    return Method.newBuilder()
        .setMethodDescriptor(fromMethodDescriptor)
        .setParameters(parameters)
        .addStatements(statement)
        .setJsDocDescription(jsDocDescription)
        .setSourcePosition(sourcePosition)
        .build();
  }

  public static List<Variable> createParameterVariables(List<TypeDescriptor> parameterTypes) {
    List<Variable> parameters = new ArrayList<>();
    for (int i = 0; i < parameterTypes.size(); i++) {
      parameters.add(
          Variable.newBuilder()
              .setName("arg" + i)
              .setTypeDescriptor(parameterTypes.get(i))
              .setParameter(true)
              .build());
    }
    return parameters;
  }

  public static Statement createForwardingStatement(
      SourcePosition sourcePosition,
      Expression qualifier,
      MethodDescriptor toMethodDescriptor,
      boolean isStaticDispatch,
      List<Expression> arguments,
      TypeDescriptor returnTypeDescriptor) {
    // TODO(rluble): Casts are probably needed on arguments if the types differ between the
    // targetMethodDescriptor and its declarationMethodDescriptor.
    Expression forwardingMethodCall =
        MethodCall.Builder.from(toMethodDescriptor)
            .setQualifier(qualifier)
            .setArguments(maybePackageVarargs(toMethodDescriptor, arguments))
            .setStaticDispatch(isStaticDispatch)
            .setSourcePosition(sourcePosition)
            .build();

    return createReturnOrExpressionStatement(
        sourcePosition, forwardingMethodCall, returnTypeDescriptor);
  }

  /** Returns {@code true} if the expression result is used by the parent. */
  public static boolean isExpressionResultUsed(Expression expression, Object parent) {
    return switch (parent) {
      case ExpressionStatement expressionStatement -> false;
      case TryStatement tryStatement -> false;
      case MultiExpression multiExpression ->
          expression == Iterables.getLast(multiExpression.getExpressions());
      case ForStatement forStatement -> expression == forStatement.getConditionExpression();
      case BinaryExpression binaryExpression ->
          // The value of the lhs of an assignment is overwritten and not used.
          !binaryExpression.isSimpleAssignment()
              || expression == binaryExpression.getRightOperand();
      default -> true;
    };
  }

  /**
   * Returns true if {@code expression} is left operand of simple or compound assignment expression.
   */
  public static boolean isAssignmentTarget(Expression expression, Object parent) {
    return switch (parent) {
      case BinaryExpression parentBinaryExpression ->
          parentBinaryExpression.isSimpleOrCompoundAssignment()
              && expression == parentBinaryExpression.getLeftOperand();
      case UnaryExpression unaryExpression ->
          unaryExpression.isSimpleOrCompoundAssignment()
              && expression == unaryExpression.getOperand();
      default -> false;
    };
  }

  /**
   * See JLS 5.2.
   *
   * <p>Note that compound assignments are excluded here. The assignment context arising from
   * compound assignments requires the expression to be rewritten into a plain assignment.
   */
  public static boolean matchesAssignmentContext(BinaryExpression expression) {
    return expression.isSimpleAssignment();
  }

  /** See JLS 5.4. */
  public static boolean matchesStringContext(BinaryExpression binaryExpression) {
    BinaryOperator operator = binaryExpression.getOperator();
    return (operator == BinaryOperator.PLUS_ASSIGN || operator == BinaryOperator.PLUS)
        && TypeDescriptors.isJavaLangString(binaryExpression.getTypeDescriptor());
  }

  /** See JLS 5.6.1. */
  public static boolean matchesUnaryNumericPromotionContext(PrefixExpression prefixExpression) {
    PrefixOperator operator = prefixExpression.getOperator();
    return TypeDescriptors.isBoxedOrPrimitiveType(prefixExpression.getTypeDescriptor())
        && (operator == PrefixOperator.PLUS
            || operator == PrefixOperator.MINUS
            || operator == PrefixOperator.COMPLEMENT);
  }

  /** See JLS 5.6.1. */
  public static boolean matchesUnaryNumericPromotionContext(PostfixExpression postfixExpression) {
    // There are no postfix expressions that match a unary numeric promotion context.
    return false;
  }

  /** See JLS 5.6.2. */
  public static boolean matchesBinaryNumericPromotionContext(BinaryExpression binaryExpression) {
    // Both operands must be boxed or primitive.
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();
    BinaryOperator operator = binaryExpression.getOperator();

    if (!TypeDescriptors.isBoxedOrPrimitiveType(leftOperand.getTypeDescriptor())
        || !TypeDescriptors.isBoxedOrPrimitiveType(rightOperand.getTypeDescriptor())) {
      return false;
    }

    boolean leftIsPrimitive = leftOperand.getTypeDescriptor().isPrimitive();
    boolean rightIsPrimitive = rightOperand.getTypeDescriptor().isPrimitive();

    BinaryOperator binaryOperator =
        operator.isCompoundAssignment() ? operator.getUnderlyingBinaryOperator() : operator;

    return switch (binaryOperator) {
      case TIMES,
          DIVIDE,
          REMAINDER,
          PLUS,
          MINUS,
          LESS,
          GREATER,
          LESS_EQUALS,
          GREATER_EQUALS,
          BIT_XOR,
          BIT_AND,
          BIT_OR ->
          true; // Both numerics and booleans get these operators.
      case EQUALS, NOT_EQUALS ->
          leftIsPrimitive || rightIsPrimitive; // Equality is sometimes instance comparison.
      default -> false;
    };
  }

  /** See JLS 5.1. */
  public static boolean matchesBooleanConversionContext(BinaryOperator operator) {
    return switch (operator) {
      case CONDITIONAL_AND, CONDITIONAL_OR -> true; // Booleans get these operators.
      default -> false;
    };
  }

  /** See JLS 5.1. */
  public static boolean matchesBooleanConversionContext(PrefixOperator operator) {
    return operator == PrefixOperator.NOT;
  }

  /** JsEnum comparison context. */
  public static boolean matchesJsEnumBoxingConversionContext(BinaryExpression binaryExpression) {
    BinaryOperator operator = binaryExpression.getOperator();
    // Use the declaration descriptor to ignore the specialization of type variables to JsEnums
    // to understand whether the (returned) enum was boxed or unboxed.
    TypeDescriptor lhsTypeDescriptor =
        binaryExpression.getLeftOperand().getDeclaredTypeDescriptor();
    TypeDescriptor rhsTypeDescriptor =
        binaryExpression.getRightOperand().getDeclaredTypeDescriptor();

    return operator.isRelationalOperator()
        && (isNonNativeJsEnum(lhsTypeDescriptor) || isNonNativeJsEnum(rhsTypeDescriptor))
        // DO NOT use hasSameRawType, since intersections that have JsEnums have the same raw type
        // as the JsEnum but behave differently w.r.t JsEnum boxing conversions. Intersection types
        // are always boxed.
        && !lhsTypeDescriptor.isSameBaseType(rhsTypeDescriptor);
  }

  /** JsEnum comparison context. */
  public static boolean matchesJsEnumBoxingConversionContext(
      InstanceOfExpression instanceOfExpression) {
    TypeDescriptor expressionTypeDescriptor =
        instanceOfExpression.getExpression().getTypeDescriptor();
    TypeDescriptor testTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();
    return isNonNativeJsEnum(expressionTypeDescriptor)
        // DO NOT use hasSameRawType, since intersections that have JsEnums have the same raw type
        // as the JsEnum but behave differently w.r.t JsEnum boxing conversions. Intersection types
        // are always boxed.
        && !expressionTypeDescriptor.isSameBaseType(testTypeDescriptor);
  }

  /**
   * Returns the type of the numeric binary expression, which is the wider type of its operands, or
   * int if it is wider.
   *
   * <p>See JLS 5.6.2.
   */
  public static PrimitiveTypeDescriptor getNumericBinaryExpressionTypeDescriptor(
      PrimitiveTypeDescriptor thisTypeDescriptor, PrimitiveTypeDescriptor thatTypeDescriptor) {
    if (TypeDescriptors.isPrimitiveDouble(thisTypeDescriptor)
        || TypeDescriptors.isPrimitiveDouble(thatTypeDescriptor)) {
      return PrimitiveTypes.DOUBLE;
    }

    if (TypeDescriptors.isPrimitiveFloat(thisTypeDescriptor)
        || TypeDescriptors.isPrimitiveFloat(thatTypeDescriptor)) {
      return PrimitiveTypes.FLOAT;
    }

    if (TypeDescriptors.isPrimitiveLong(thisTypeDescriptor)
        || TypeDescriptors.isPrimitiveLong(thatTypeDescriptor)) {
      return PrimitiveTypes.LONG;
    }

    return PrimitiveTypes.INT;
  }

  /**
   * Returns the type of the numeric unary expression, which is the type of its operand, or int if
   * it is wider.
   *
   * <p>See JLS 5.6.1.
   */
  public static PrimitiveTypeDescriptor getNumericUnaryExpressionTypeDescriptor(
      PrimitiveTypeDescriptor thisTypeDescriptor) {
    if (PrimitiveTypes.INT.isWiderThan(thisTypeDescriptor)) {
      return PrimitiveTypes.INT;
    }

    return thisTypeDescriptor;
  }

  /**
   * Generates the following code:
   *
   * <p>$Util.$makeLambdaFunction(Type.prototype.m_equal, $instance, Type.$copy);
   */
  public static MethodCall createLambdaInstance(
      DeclaredTypeDescriptor lambdaType, Expression instance) {
    checkArgument(lambdaType.isJsFunctionImplementation());

    // Use the method from the interface instead of the implementation method, since it is the
    // appropriate semantic behaviour. The function might be called (directly as a function) from
    // as a @JsFunction which might require dispatch through the bridge.
    MethodDescriptor jsFunctionMethodDescriptor = lambdaType.getJsFunctionMethodDescriptor();

    // Class.prototype.apply
    String functionalMethodMangledName = jsFunctionMethodDescriptor.getMangledName();

    FieldAccess applyFunctionFieldAccess =
        FieldAccess.Builder.from(
                FieldDescriptor.newBuilder()
                    .setEnclosingTypeDescriptor(lambdaType)
                    .setName(functionalMethodMangledName)
                    .setTypeDescriptor(TypeDescriptors.get().nativeFunction)
                    .setOriginalJsInfo(JsInfo.RAW_FIELD)
                    .build())
            .setQualifier(
                new JavaScriptConstructorReference(lambdaType.getTypeDeclaration())
                    .getPrototypeFieldAccess())
            .build();

    FieldAccess copyFunctionFieldAccess =
        FieldAccess.Builder.from(
                FieldDescriptor.newBuilder()
                    .setEnclosingTypeDescriptor(lambdaType)
                    .setName("$copy")
                    .setTypeDescriptor(TypeDescriptors.get().nativeFunction)
                    .setOriginalJsInfo(JsInfo.RAW_FIELD)
                    .build())
            .setQualifier(new JavaScriptConstructorReference(lambdaType.getTypeDeclaration()))
            .build();

    return RuntimeMethods.createUtilMethodCall(
        "$makeLambdaFunction", applyFunctionFieldAccess, instance, copyFunctionFieldAccess);
  }

  /** Returns a field declaration statement. */
  public static Statement declarationStatement(Field field, SourcePosition sourcePosition) {
    FieldDescriptor fieldDescriptor = field.getDescriptor();
    boolean isPublic = fieldDescriptor.getOrigin() != FieldOrigin.SYNTHETIC_BACKING_FIELD;

    Expression declarationExpression =
        FieldAccess.newBuilder()
            .setTarget(fieldDescriptor)
            .setDefaultInstanceQualifier()
            .setSourcePosition(field.getNameSourcePosition())
            .build();

    if (field.getInitializer() != null) {
      declarationExpression =
          BinaryExpression.Builder.asAssignmentTo(declarationExpression)
              .setRightOperand(field.getInitializer())
              .build();
    }

    return FieldDeclarationStatement.newBuilder()
        .setExpression(declarationExpression)
        .setFieldDescriptor(fieldDescriptor)
        .setPublic(isPublic)
        .setSourcePosition(
            field.isCompileTimeConstant() ? field.getSourcePosition() : sourcePosition)
        .build();
  }

  public static String getSimpleSourceName(List<String> classComponents) {
    String simpleName = Iterables.getLast(classComponents);
    // Prefix anonymous numbered classes with a string to to make their names less odd.
    return startsWithNumber(simpleName) ? "$" + simpleName : simpleName;
  }

  private static boolean startsWithNumber(String string) {
    return !string.isEmpty() && Character.isDigit(string.charAt(0));
  }

  /** Clones a list of expressions */
  @SuppressWarnings("unchecked")
  public static <T extends Cloneable<?>> ImmutableList<T> clone(List<T> nodes) {
    return nodes.stream().map(node -> (T) node.clone()).collect(toImmutableList());
  }

  /** Clones a cloneable or returns null */
  @SuppressWarnings("unchecked")
  public static <T extends Cloneable<?>> T clone(T node) {
    return node != null ? (T) node.clone() : null;
  }

  /**
   * Replaces references to {@code fromDeclarations} by references to {@code toDeclarations} in
   * {@code nodes}.
   */
  public static <T extends Node> List<T> replaceDeclarations(
      List<? extends NameDeclaration> fromDeclarations,
      List<? extends NameDeclaration> toDeclarations,
      List<T> nodes) {
    List<T> result = new ArrayList<>();
    for (T node : nodes) {
      result.add(replaceDeclarations(fromDeclarations, toDeclarations, node));
    }
    return result;
  }

  /**
   * Replaces references to {@code fromDeclarations} by references to {@code toDeclarations} in
   * {@code node}.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Node> T replaceDeclarations(
      List<? extends NameDeclaration> fromDeclarations,
      List<? extends NameDeclaration> toDeclarations,
      T node) {
    checkArgument(fromDeclarations.size() == toDeclarations.size());

    Map<NameDeclaration, NameDeclaration> toDeclarationByFromDeclaration = new HashMap<>();
    for (int i = 0; i < fromDeclarations.size(); i++) {
      toDeclarationByFromDeclaration.put(fromDeclarations.get(i), toDeclarations.get(i));
    }

    return (T)
        node.acceptInternal(
            new AbstractRewriter() {
              @Override
              public NameDeclaration rewriteNameDeclaration(NameDeclaration nameDeclaration) {
                NameDeclaration toNameDeclaration =
                    toDeclarationByFromDeclaration.get(nameDeclaration);
                return toNameDeclaration == null ? nameDeclaration : toNameDeclaration;
              }

              @Override
              public LabelReference rewriteLabelReference(LabelReference labelReference) {
                return (LabelReference) replaceReference(labelReference);
              }

              @Override
              public VariableReference rewriteVariableReference(
                  VariableReference variableReference) {
                return (VariableReference) replaceReference(variableReference);
              }

              private Reference<?> replaceReference(Reference<?> reference) {
                NameDeclaration nameDeclaration =
                    toDeclarationByFromDeclaration.get(reference.getTarget());
                if (nameDeclaration != null) {
                  return nameDeclaration.createReference();
                }
                return reference;
              }
            });
  }

  /** Get a list of references for {@code variables}. */
  public static ImmutableList<Expression> getReferences(List<Variable> variables) {
    return variables.stream().map(Variable::createReference).collect(toImmutableList());
  }

  /** Creates an implicit constructor that forwards to a specific super constructor. */
  public static Method createImplicitAnonymousClassConstructor(
      SourcePosition sourcePosition,
      MethodDescriptor constructorDescriptor,
      MethodDescriptor superConstructorDescriptor,
      Expression superCallQualifier) {

    List<Variable> constructorParameters = new ArrayList<>();
    int index = 0;
    for (TypeDescriptor parameterTypeDescriptor :
        constructorDescriptor.getParameterTypeDescriptors()) {
      String parameterName = "$_" + index;
      constructorParameters.add(
          Variable.newBuilder()
              .setName(parameterName)
              .setTypeDescriptor(parameterTypeDescriptor)
              .setParameter(true)
              .build());
      index++;
    }

    ImmutableList<Expression> superConstructorArguments =
        constructorParameters.stream().map(Variable::createReference).collect(toImmutableList());

    return Method.newBuilder()
        .setMethodDescriptor(constructorDescriptor)
        .setParameters(constructorParameters)
        .addStatements(
            MethodCall.Builder.from(superConstructorDescriptor)
                .setQualifier(superCallQualifier)
                .setArguments(superConstructorArguments)
                .build()
                .makeStatement(sourcePosition))
        .setSourcePosition(sourcePosition)
        .build();
  }

  public static Expression removeJsDocCastIfPresent(Expression expression) {
    if (expression instanceof JsDocCastExpression jsDocCastExpression) {
      return jsDocCastExpression.getExpression();
    }
    return expression;
  }

  /**
   * Create a return statement if the return type is not void; otherwise create an expression
   * statement
   */
  public static Statement createReturnOrExpressionStatement(
      SourcePosition sourcePosition,
      Expression expression,
      TypeDescriptor methodReturnTypeDescriptor) {

    if (TypeDescriptors.isPrimitiveVoid(methodReturnTypeDescriptor)) {
      return expression.makeStatement(sourcePosition);
    }

    return ReturnStatement.newBuilder()
        .setExpression(expression)
        .setSourcePosition(sourcePosition)
        .build();
  }

  /**
   * Returns a TypeDescriptor to refer to the enclosing name that represents the member namespace;
   * this will transform a namespace=a.b.c on an JsMethod into a fictitious TypeDescriptor for
   * namespace=a.b and name=c.
   */
  public static DeclaredTypeDescriptor getNamespaceAsTypeDescriptor(
      MemberDescriptor memberDescriptor) {
    String memberJsNamespace = memberDescriptor.getJsNamespace();
    if (JsUtils.isGlobal(memberJsNamespace)) {
      return TypeDescriptors.get().globalNamespace;
    }

    List<String> components = Splitter.on('.').omitEmptyStrings().splitToList(memberJsNamespace);

    String jsNamespace = buildQualifiedName(components.subList(0, components.size() - 1).stream());
    String jsName = Iterables.getLast(components);
    return TypeDescriptors.createNativeTypeDescriptor(jsNamespace, jsName);
  }

  /** Returns a qualified name, ignoring empty and {@code null} {@code parts}. */
  public static String buildQualifiedName(String... parts) {
    return buildQualifiedName(Arrays.stream(parts));
  }

  /** Returns a qualified name, ignoring empty and {@code null} {@code parts}. */
  public static String buildQualifiedName(Stream<String> parts) {
    return parts
        .filter(Predicates.notNull())
        .filter(Predicates.not(String::isEmpty))
        .collect(Collectors.joining("."));
  }

  /**
   * Creates a static method in {@code targetTypeDescriptor} with the same signature and body as
   * {@code method}.
   */
  public static Method createStaticOverlayMethod(
      Method method, DeclaredTypeDescriptor targetTypeDescriptor) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    checkArgument(methodDescriptor.isStatic());

    return Method.Builder.from(method)
        .setMethodDescriptor(
            MethodDescriptor.Builder.from(methodDescriptor)
                .setOriginalJsInfo(methodDescriptor.isJsAsync() ? JsInfo.NONE_ASYNC : JsInfo.NONE)
                .setEnclosingTypeDescriptor(targetTypeDescriptor)
                .removeParameterOptionality()
                .build())
        .build();
  }

  /**
   * Devirtualizes {@code Method} by making {@code this} into an explicit argument and placing the
   * resulting method in {@code enclosingTypeDescriptor}.
   */
  public static Method devirtualizeMethod(
      Method method, DeclaredTypeDescriptor enclosingTypeDescriptor) {
    return devirtualizeMethod(method, enclosingTypeDescriptor, null);
  }

  /**
   * Devirtualizes {@code Method} by making {@code this} into an explicit argument and placing the
   * resulting method in {@code enclosingTypeDescriptor} and adding {@code postfix} to its name to
   * avoid possible name collisions with already existing static methods.
   */
  public static Method devirtualizeMethod(Method method, String postfix) {
    checkArgument(!postfix.isEmpty());
    return devirtualizeMethod(method, method.getDescriptor().getEnclosingTypeDescriptor(), postfix);
  }

  /**
   * Devirtualizes {@code Method} by making {@code this} into an explicit argument and placing the
   * resulting method in according to {@code devirtualizedMethodDescriptor}.
   */
  public static Method devirtualizeMethod(
      Method method, DeclaredTypeDescriptor enclosingTypeDescriptor, String postfix) {
    checkArgument(method.getDescriptor().isInstanceMember());
    checkArgument(!method.getDescriptor().isInitMethod(), "Do not devirtualize init().");

    MethodDescriptor devirtualizedMethodDescriptor =
        devirtualizeMethodDescriptor(
            method.getDescriptor(), enclosingTypeDescriptor, Optional.ofNullable(postfix));

    final Variable thisArg =
        Variable.newBuilder()
            .setName("$thisArg")
            .setTypeDescriptor(method.getDescriptor().getEnclosingTypeDescriptor().toNonNullable())
            .setParameter(true)
            .setFinal(true)
            .build();

    // Replace all 'this' and 'super' references in the method with a reference to the newly
    // introduced parameter.
    method.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteThisReference(ThisReference thisReference) {
            return thisArg.createReference();
          }

          @Override
          public Node rewriteSuperReference(SuperReference superReference) {
            return thisArg.createReference();
          }
        });

    // Add the static method to current type.
    return Method.newBuilder()
        .setMethodDescriptor(devirtualizedMethodDescriptor)
        .setParameters(
            ImmutableList.<Variable>builder().add(thisArg).addAll(method.getParameters()).build())
        .addStatements(method.getBody().getStatements())
        .setSourcePosition(method.getSourcePosition())
        .build();
  }

  /**
   * Creates devirtualized method call of {@code methodCall} as method call to the static method in
   * {@code targetTypeDescriptor} with the {@code instanceTypeDescriptor} as the first parameter
   * type.
   *
   * <p>instance.instanceMethod(a, b) => TargetType.staticMethod(instance, a, b)
   */
  public static MethodCall devirtualizeMethodCall(
      MethodCall methodCall, DeclaredTypeDescriptor targetTypeDescriptor) {
    return devirtualizeMethodCall(methodCall, targetTypeDescriptor, null);
  }

  /**
   * Creates devirtualized method call of {@code methodCall} as method call to the corresponding
   * static method in the same type with the {@code instanceTypeDescriptor} as the first parameter
   * type. Since the devirtualized method is in the same type a {@code postfix} is used to make sure
   * there is no collision with an already existing static method.
   *
   * <p>instance.instanceMethod(a, b) => staticMethod_postfix(instance, a, b)
   */
  public static MethodCall devirtualizeMethodCall(MethodCall methodCall, String postfix) {
    checkArgument(!postfix.isEmpty());
    return devirtualizeMethodCall(
        methodCall, methodCall.getTarget().getEnclosingTypeDescriptor(), postfix);
  }

  public static MethodCall devirtualizeMethodCall(
      MethodCall methodCall, DeclaredTypeDescriptor targetTypeDescriptor, String postfix) {
    MethodDescriptor devirtualizedMethodDescriptor =
        devirtualizeMethodDescriptor(
            methodCall.getTarget(), targetTypeDescriptor, Optional.ofNullable(postfix));

    Expression qualifier = checkNotNull(methodCall.getQualifier());
    if (qualifier instanceof SuperReference superReference) {
      // A 'super' qualifier is used to resolve to the correct method to dispatch, once the method
      // is devirutalized and receives the qualifier as its first parameter, 'super' must be turned
      // into 'this' since both evaluate to the implicit instance parameter but 'super' is not
      // valid as a general expression.
      qualifier = new ThisReference(superReference.getTypeDescriptor());
    }
    // Call the method like Objects.foo(instance, ...)
    ImmutableList<Expression> arguments =
        ImmutableList.<Expression>builder()
            // Turn the instance into the first parameter to the devirtualized method.
            .add(qualifier)
            .addAll(methodCall.getArguments())
            .build();
    return MethodCall.Builder.from(devirtualizedMethodDescriptor).setArguments(arguments).build();
  }

  /**
   * Creates a devirtualized static MethodDescriptor from an instance MethodDescriptor.
   *
   * <p>The static MethodDescriptor has an extra parameter as its first parameter whose type is the
   * enclosing class of {@code methodDescriptor}.
   */
  private static MethodDescriptor devirtualizeMethodDescriptor(
      MethodDescriptor methodDescriptor,
      DeclaredTypeDescriptor targetTypeDescriptor,
      Optional<String> postfix) {

    DeclaredTypeDescriptor enclosingTypeDescriptor = methodDescriptor.getEnclosingTypeDescriptor();

    String devirtualizedMethodName = methodDescriptor.getName() + postfix.orElse("");
    return methodDescriptor.transform(
        builder ->
            builder
                .setName(devirtualizedMethodName)
                .setEnclosingTypeDescriptor(targetTypeDescriptor)
                // The instance ($thisArg) parameter is assumed non nullable for
                // the typing perspective.
                .addParameterTypeDescriptors(0, enclosingTypeDescriptor.toNonNullable())
                .addTypeParameterTypeDescriptors(
                    0, enclosingTypeDescriptor.getTypeDeclaration().getTypeParameterDescriptors())
                .setStatic(true)
                .setConstructor(false)
                .setAbstract(false)
                .setDefaultMethod(false)
                .setOriginalJsInfo(methodDescriptor.isJsAsync() ? JsInfo.NONE_ASYNC : JsInfo.NONE)
                .removeParameterOptionality());
  }

  /** Returns an Optional.empty() if optionalSourcePosition is empty or unnamed */
  public static SourcePosition removeUnnamedSourcePosition(SourcePosition sourcePosition) {
    return sourcePosition.getName() == null ? SourcePosition.NONE : sourcePosition;
  }

  /** Returns the arguments with the vararg arguments packaged together as an array if necessary. */
  public static List<Expression> maybePackageVarargs(
      MethodDescriptor methodDescriptor, List<Expression> arguments) {
    if (!shouldPackageVarargs(methodDescriptor, arguments)) {
      return arguments;
    }

    int parameterLength = methodDescriptor.getParameterDescriptors().size();
    Expression packagedVarargs = getPackagedVarargs(methodDescriptor, arguments);
    List<Expression> result = new ArrayList<>(arguments.subList(0, parameterLength - 1));
    result.add(packagedVarargs);
    return result;
  }

  /** Returns if a method call is invoked with varargs that are not in an explicit array format. */
  private static boolean shouldPackageVarargs(
      MethodDescriptor methodDescriptor, List<Expression> arguments) {
    int parametersLength = methodDescriptor.getParameterDescriptors().size();
    if (!methodDescriptor.isVarargs()) {
      return false;
    }
    if (arguments.size() != parametersLength) {
      return true;
    }
    Expression lastArgument = arguments.get(parametersLength - 1);
    ParameterDescriptor lastParameter =
        methodDescriptor.getParameterDescriptors().get(parametersLength - 1);

    return !(lastArgument instanceof NullLiteral)
        && !lastArgument.getTypeDescriptor().isAssignableTo(lastParameter.getTypeDescriptor());
  }

  /** Returns the package the varargs argument as an array. */
  private static Expression getPackagedVarargs(
      MethodDescriptor methodDescriptor, List<Expression> arguments) {
    checkArgument(methodDescriptor.isVarargs());
    int varargsParameterIndex = methodDescriptor.getParameterDescriptors().size() - 1;
    TypeDescriptor varargsTypeDescriptor =
        methodDescriptor.getParameterTypeDescriptors().get(varargsParameterIndex);
    return ArrayLiteral.newBuilder()
        .setTypeDescriptor((ArrayTypeDescriptor) varargsTypeDescriptor)
        .setValueExpressions(arguments.subList(varargsParameterIndex, arguments.size()))
        .build();
  }

  // TODO(b/182341814): This is a temporary hack to be able to disable DoNotAutobox annotations
  // on wasm
  private static final ThreadLocal<Boolean> ignoreDoNotAutoboxAnnotations =
      ThreadLocal.withInitial(() -> false);

  public static void setIgnoreDoNotAutoboxAnnotations() {
    ignoreDoNotAutoboxAnnotations.set(true);
  }

  /** Returns whether the parameter is annotated with {@code @DoNotAutobox}. */
  public static boolean isAnnotatedWithDoNotAutobox(
      MethodDescriptor.ParameterDescriptor parameter) {
    if (ignoreDoNotAutoboxAnnotations.get()) {
      return false;
    }
    return parameter.hasAnnotation("javaemul.internal.annotations.DoNotAutobox");
  }

  /** Whether the function is the identity function. */
  public static boolean isIdentityFunction(Function<?, ?> function) {
    return function == Function.identity();
  }

  /** Creates a synthetic property setter descriptor for the specified field. */
  public static MethodDescriptor getSetterMethodDescriptor(FieldDescriptor fieldDescriptor) {
    return createMethodDescriptorBuilderFrom(fieldDescriptor)
        .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_PROPERTY_SETTER)
        .setParameterTypeDescriptors(fieldDescriptor.getTypeDescriptor())
        .setReturnTypeDescriptor(PrimitiveTypes.VOID)
        .setOriginalJsInfo(
            fieldDescriptor.isJsProperty()
                ? JsInfo.Builder.from(fieldDescriptor.getJsInfo())
                    .setJsMemberType(JsMemberType.SETTER)
                    .setJsName(fieldDescriptor.getSimpleJsName())
                    .build()
                : JsInfo.NONE)
        .build();
  }

  /** Creates a synthetic property getter descriptor for the specified field. */
  public static MethodDescriptor getGetterMethodDescriptor(FieldDescriptor fieldDescriptor) {
    return createMethodDescriptorBuilderFrom(fieldDescriptor)
        .setOrigin(MethodDescriptor.MethodOrigin.SYNTHETIC_PROPERTY_GETTER)
        .setReturnTypeDescriptor(fieldDescriptor.getTypeDescriptor())
        .setOriginalJsInfo(
            fieldDescriptor.isJsProperty()
                ? JsInfo.Builder.from(fieldDescriptor.getJsInfo())
                    .setJsMemberType(JsMemberType.GETTER)
                    .setJsName(fieldDescriptor.getSimpleJsName())
                    .build()
                : JsInfo.NONE)
        .build();
  }

  /** Returns the type of the iterable element for a type might appear in a foreach statement. */
  public static TypeDescriptor getIterableElement(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isArray()) {
      return ((ArrayTypeDescriptor) typeDescriptor).getComponentTypeDescriptor();
    }

    TypeDescriptor iteratorTypeDescriptor =
        typeDescriptor.getMethodDescriptor("iterator").getReturnTypeDescriptor();

    return iteratorTypeDescriptor.getMethodDescriptor("next").getReturnTypeDescriptor();
  }

  private static MethodDescriptor.Builder createMethodDescriptorBuilderFrom(
      FieldDescriptor fieldDescriptor) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(fieldDescriptor.getEnclosingTypeDescriptor())
        .setName(fieldDescriptor.getName())
        .setVisibility(fieldDescriptor.getVisibility())
        .setStatic(fieldDescriptor.isStatic())
        // TODO(b/402181063): Determine if we need to propagate annotations here.
        .setAnnotations(
            fieldDescriptor.getAnnotations().stream()
                .filter(AstUtils::isDeprecatedAnnotation)
                .collect(toImmutableList()))
        .setNative(fieldDescriptor.isNative());
  }

  private static boolean isDeprecatedAnnotation(Annotation annotation) {
    return annotation.getTypeDescriptor().getQualifiedSourceName().equals("java.lang.Deprecated")
        || annotation.getTypeDescriptor().getQualifiedSourceName().equals("kotlin.Deprecated");
  }

  /** Returns the field descriptor for the const field that holds the ordinal value. */
  public static FieldDescriptor getEnumOrdinalConstantFieldDescriptor(
      FieldDescriptor fieldDescriptor) {
    checkArgument(fieldDescriptor.isStatic());
    return FieldDescriptor.Builder.from(fieldDescriptor)
        .setName("$ordinal_" + fieldDescriptor.getName())
        .setEnumConstant(false)
        .setCompileTimeConstant(true)
        .setConstantValue(fieldDescriptor.getEnumOrdinalValue())
        .setSynthetic(true)
        .setTypeDescriptor(PrimitiveTypes.INT)
        .setOriginalJsInfo(JsInfo.NONE)
        .setOrigin(FieldOrigin.SYNTHETIC_ORDINAL_FIELD)
        .build();
  }

  /** Returns the value field descriptor for a JsEnum */
  @Nullable
  private static FieldDescriptor getJsEnumValueFieldDescriptor(TypeDeclaration typeDeclaration) {
    checkState(typeDeclaration.isJsEnum());
    return typeDeclaration.getDeclaredFieldDescriptors().stream()
        .filter(AstUtils::isJsEnumCustomValueField)
        .findFirst()
        .orElse(null);
  }

  public static boolean isJsEnumCustomValueField(MemberDescriptor memberDescriptor) {
    return memberDescriptor.isField()
        && memberDescriptor.getName().equals("value")
        && memberDescriptor.getEnclosingTypeDescriptor().isJsEnum();
  }

  public static boolean isPrimitiveNonNativeJsEnum(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isJsEnum()
        && getJsEnumValueFieldType(((DeclaredTypeDescriptor) typeDescriptor).getTypeDeclaration())
            .isPrimitive();
  }

  public static FieldDescriptor createJsEnumConstantFieldDescriptor(
      FieldDescriptor fieldDescriptor) {
    TypeDescriptor enumValueType =
        getJsEnumValueFieldType(fieldDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration());
    return FieldDescriptor.Builder.from(fieldDescriptor)
        .setTypeDescriptor(enumValueType)
        .setFinal(true)
        .setCompileTimeConstant(true)
        .setEnumConstant(true)
        .build();
  }

  public static TypeDescriptor getJsEnumValueFieldType(TypeDescriptor typeDescriptor) {
    checkState(typeDescriptor.isJsEnum());
    return getJsEnumValueFieldType(((DeclaredTypeDescriptor) typeDescriptor).getTypeDeclaration());
  }

  /** Returns the value field for a JsEnum. */
  public static TypeDescriptor getJsEnumValueFieldType(TypeDeclaration typeDeclaration) {
    FieldDescriptor valueFieldDescriptor = getJsEnumValueFieldDescriptor(typeDeclaration);
    // This method should be used carefully. A properly declared JsEnum will always have
    // a value field if it has a custom value (after validation); but due to header compilation
    // removing private fields, the field might not be visible across compilation boundaries and
    // and this method could give incorrect information in that case.
    checkState(valueFieldDescriptor != null || !typeDeclaration.getJsEnumInfo().hasCustomValue());
    return valueFieldDescriptor == null
        ? PrimitiveTypes.INT
        : valueFieldDescriptor.getTypeDescriptor();
  }

  /** Returns the type to use for instanceof checking for a native JsEnum. */
  public static DeclaredTypeDescriptor getJsEnumValueFieldInstanceCheckType(
      TypeDeclaration typeDeclaration) {
    TypeDescriptor valueTypeDescriptor =
        getJsEnumValueFieldDescriptor(typeDeclaration).getTypeDescriptor();
    // The JavaScript types are either number, boolean or string. As a shortcut we use the
    // instanceof checks for Double, Boolean and String respectively, instead of duplicating the
    // code.
    // Note that this is not equivalent to use .toBoxedType since for example [int].toBoxedType()
    // will actually be java.lang.Integer not the runtime type for an unboxed number enum.
    if (TypeDescriptors.isNumericPrimitive(valueTypeDescriptor)) {
      return TypeDescriptors.get().javaLangDouble;
    } else if (TypeDescriptors.isPrimitiveBoolean(valueTypeDescriptor)) {
      return TypeDescriptors.get().javaLangBoolean;
    } else {
      return (DeclaredTypeDescriptor) valueTypeDescriptor;
    }
  }

  /** Return the static field that will hold the value for a system property. */
  public static MethodDescriptor getSystemGetPropertyGetter(
      String systemPropertyString, boolean requiredProperty) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(getSystemPropertyHolder().toDescriptor())
        // TODO(rluble): Sanitize the system property string.
        .setName(systemPropertyString)
        .setReturnTypeDescriptor(TypeDescriptors.get().javaLangString)
        .setOrigin(
            requiredProperty
                ? MethodOrigin.SYNTHETIC_SYSTEM_PROPERTY_GETTER_REQUIRED
                : MethodOrigin.SYNTHETIC_SYSTEM_PROPERTY_GETTER_OPTIONAL)
        .setStatic(true)
        .setSynthetic(true)
        .setVisibility(Visibility.PRIVATE)
        .build();
  }

  /** Return the type descriptor for the holder of system properties. */
  public static TypeDeclaration getSystemPropertyHolder() {
    return TypeDeclaration.newBuilder()
        .setQualifiedSourceName("javaemul.internal.SystemPropertyPool")
        .setKind(Kind.INTERFACE)
        .build();
  }

  /** Returns true if {@code methodCall} is a call to {@link System#getProperty}. */
  public static boolean isSystemGetPropertyCall(MethodCall methodCall) {
    return "java.lang.System.getProperty".equals(methodCall.getTarget().getQualifiedBinaryName());
  }

  /**
   * Returns true if {@code typeDescriptor} is a non native JsEnum, i.e. a JsEnum that requires
   * boxing.
   */
  public static boolean isNonNativeJsEnum(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isJsEnum() && !typeDescriptor.isNative();
  }

  /** Returns true {@code typeDescriptor} requires bridges be generated for boxing/unboxing. */
  public static boolean isBoxableJsEnumType(TypeDescriptor typeDescriptor) {
    return isJsEnumBoxingSupported() && isNonNativeJsEnum(typeDescriptor);
  }

  /** Returns true if JsEnums undergo boxing/unboxing in this backend in general. */
  public static boolean isJsEnumBoxingSupported() {
    // Check if the JsEnum boxed type exists for the current backend. Not all backends utilize
    // JsEnum semantics.
    return TypeDescriptors.get().javaemulInternalBoxedLightEnum != null;
  }

  /** Returns true if {@code typeDescriptor} is a non native JsEnum array. */
  public static boolean isNonNativeJsEnumArray(TypeDescriptor typeDescriptor) {
    return typeDescriptor.isArray()
        && isNonNativeJsEnum(((ArrayTypeDescriptor) typeDescriptor).getLeafTypeDescriptor());
  }

  /** Gets the initial value for a field or variable for the be assigned to a Wasm variable. */
  public static Expression getInitialValue(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getDefaultValue();
  }

  /** Inserts type-consistency casts for JsEnum values when used as their value type. */
  public static Expression castJsEnumToValue(
      Expression jsEnumExpression, TypeDescriptor valueTypeDescriptor) {
    checkArgument(jsEnumExpression.getTypeDescriptor().isJsEnum());
    // In order to preserve type consistency, expressions like
    //
    //     getEnum()  // where getEnum() returns MyJsEnum.
    //
    // will be rewritten as
    //
    //     /** @type {int} */ ((MyJsEnum) getEnum())
    //
    // The inner Java cast to MyJsEnum guarantees that any conversion for getEnum() is preserved
    // (e.g. erasure casts if getEnum() returned T and was specialized to MyJsEnum in the calling
    // context; this allows the expression to be unboxed if needed).
    // The outer JsDoc cast guarantees that the expression is treated as of being the type of value
    // and conversions such as boxing are correctly preserved (e.g. if the expression was assigned
    // to an Integer variable).
    return JsDocCastExpression.newBuilder()
        .setCastTypeDescriptor(valueTypeDescriptor)
        .setExpression(
            CastExpression.newBuilder()
                .setCastTypeDescriptor(jsEnumExpression.getTypeDescriptor())
                .setExpression(jsEnumExpression)
                .build())
        .build();
  }

  /**
   * Returns {@code true} if the specified array type should be rendered as a native Closure array.
   */
  public static boolean shouldUseUntypedArray(TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isArray());
    ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
    return arrayTypeDescriptor.isUntypedArray()
        || arrayTypeDescriptor.getLeafTypeDescriptor().isJsEnum();
  }

  /** Returns a list of null values. */
  public static List<Expression> createListOfNullValues(int size) {
    return Collections.nCopies(size, TypeDescriptors.get().javaLangObject.getNullValue());
  }

  /** Pads the expression list with null literals to the requested {@code size}. */
  public static void addNullPadding(List<Expression> expressionList, int size) {
    while (expressionList.size() < size) {
      expressionList.add(TypeDescriptors.get().javaLangObject.getNullValue());
    }
  }

  /** Sets the implicit qualifier for a member reference if it needs a qualifier. */
  public static MemberReference resolveImplicitQualifier(
      MemberReference memberReference, DeclaredTypeDescriptor contextTypeDescriptor) {
    MemberDescriptor target = memberReference.getTarget();
    DeclaredTypeDescriptor targetQualifierType = getTargetQualifierTypeDescriptor(target);
    if (target.isLocalFunction()
        || memberReference.getQualifier() != null
        || targetQualifierType == null) {
      return memberReference;
    }

    // Normally unqualified instance references would get the implicit instance of the current type
    // as their qualifier, but this() and super() constructor calls can never be qualified by
    // themselves (they happen in the constructor and the implicit instance is the one being
    // constructed). In this case the enclosing instance of the class is the starting point of the
    // search of for the qualifier.
    contextTypeDescriptor =
        memberReference instanceof MethodCall && target.isConstructor()
            ? contextTypeDescriptor.getEnclosingTypeDescriptor()
            : contextTypeDescriptor;

    return MemberReference.Builder.from(memberReference)
        .setQualifier(createImplicitQualifierExpression(contextTypeDescriptor, targetQualifierType))
        .build();
  }

  @Nullable
  private static DeclaredTypeDescriptor getTargetQualifierTypeDescriptor(
      MemberDescriptor memberDescriptor) {
    if (memberDescriptor.isStatic()) {
      return null;
    }

    DeclaredTypeDescriptor typeContainingMember = memberDescriptor.getEnclosingTypeDescriptor();
    if (memberDescriptor.isConstructor()) {
      if (!typeContainingMember.getTypeDeclaration().isCapturingEnclosingInstance()) {
        return null;
      }
      // Return the type that is captured which is the enclosing instance.
      // E.g.
      //  class A {
      //    class B {
      //      B() {}
      //    }
      //  }
      //
      // The enclosing type of the constructor B() is the type B, but the qualifier that is needed
      // for creating instances (a.new B()) is A, the enclosing type of the class B.
      return typeContainingMember.getEnclosingTypeDescriptor();
    }
    return typeContainingMember;
  }

  /**
   * Given a target type (where the field or method is declared) and a context type, finds the
   * closest enclosing type of the context type that is a subclass of the target type and creates a
   * (qualified) this reference.
   */
  private static Expression createImplicitQualifierExpression(
      DeclaredTypeDescriptor contextTypeDescriptor, DeclaredTypeDescriptor targetTypeDescriptor) {
    // The implicit qualifier is the first class from inner to outer context that has
    // a targetTypeDescriptor supertype.
    for (DeclaredTypeDescriptor qualifierTypeDescriptor = contextTypeDescriptor;
        qualifierTypeDescriptor != null;
        qualifierTypeDescriptor = qualifierTypeDescriptor.getEnclosingTypeDescriptor()) {
      if (qualifierTypeDescriptor.isSubtypeOf(targetTypeDescriptor)) {
        // If the reference is to an enclosing type or to an interface type, then mark it as
        // qualified to make it consistent of how a user should write it in the source code.
        boolean isQualified =
            !qualifierTypeDescriptor.hasSameRawType(contextTypeDescriptor)
                || qualifierTypeDescriptor.isInterface();
        return new ThisReference(qualifierTypeDescriptor, isQualified);
      }
    }

    // Ideally this code path should not be reachable, however for the case of JsEnum and optimized
    // @AutoValue/@AutoValue.Builder, the super type information in the type descriptor is
    // inconsistent with the super type in the type object. This is due to the fact that
    // optimizations that alter the class hierarchy are not reflected in the type model.
    TypeDeclaration contextTypeDeclaration = contextTypeDescriptor.getTypeDeclaration();
    if (contextTypeDeclaration.isJsEnum()
        || isAnnotatedWithAutoValue(contextTypeDeclaration)
        || isAnnotatedWithAutoValueBuilder(contextTypeDeclaration)) {
      return new ThisReference(contextTypeDescriptor);
    }
    // TODO(b/241300930): Remove when anonymous classes defined in inline functions are not marked
    //  as inner classes.
    return NullLiteral.get(targetTypeDescriptor);
  }

  /** Returns true if the specified type is annotated with AutoValue. */
  public static boolean isAnnotatedWithAutoValue(TypeDeclaration type) {
    return type.hasAnnotation("com.google.auto.value.AutoValue")
        || type.hasAnnotation("javaemul.lang.annotations.WasAutoValue");
  }

  /** Returns true if the specified type is annotated with AutoValue.Builder. */
  public static boolean isAnnotatedWithAutoValueBuilder(TypeDeclaration type) {
    return type.hasAnnotation("com.google.auto.value.AutoValue.Builder")
        || type.hasAnnotation("javaemul.lang.annotations.WasAutoValue.Builder");
  }

  /** Returns a list of statements in {@code body} which may be a block or a single statement. */
  public static List<Statement> getBodyStatements(Statement body) {
    return body instanceof Block block ? block.getStatements() : ImmutableList.of(body);
  }

  public static boolean needsVisibilityBridge(MethodDescriptor methodDescriptor) {
    if (!methodDescriptor.getVisibility().isPublicOrProtected()) {
      return false;
    }

    // The method overrides a non-package private method in a super class, no bridge is actually
    // needed even if there are package-private overridden methods up in the hierarchy.
    if (methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
        .anyMatch(
            md ->
                !md.getEnclosingTypeDescriptor().isInterface()
                    && md.getVisibility().isPublicOrProtected())) {
      return false;
    }

    // This method overrides and exposes a package-private method from a superclass.
    return methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
        .anyMatch(
            md ->
                !md.getEnclosingTypeDescriptor().isInterface()
                    && md.getVisibility().isPackagePrivate());
  }

  /** Returns true if the specified type is annotated with Wasm. */
  public static boolean isAnnotatedWithWasm(HasAnnotations node) {
    return node.getAnnotation("javaemul.internal.annotations.Wasm") != null;
  }

  private AstUtils() {}
}
