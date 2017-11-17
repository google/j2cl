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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Utility functions to manipulate J2CL AST. */
public class AstUtils {
  private static final String CAPTURES_PREFIX = "$c_";
  private static final String ENCLOSING_INSTANCE_NAME = "$outer_this";

  /**
   * Whether or not it makes sense to require this type from javascript with a goog.require('xxx');
   * This is used to detect if a goog.module.declareLegacyNamespace should be emitted for a class.
   */
  public static boolean canBeRequiredFromJs(TypeDeclaration typeDescriptor) {
    return typeDescriptor.isJsType() && !typeDescriptor.isAnonymous();
  }

  /** Return the String with first letter capitalized. */
  public static String toProperCase(String string) {
    if (string.isEmpty()) {
      return string;
    }
    return string.substring(0, 1).toUpperCase() + string.substring(1, string.length());
  }

  /** Returns new synthesized class components based on provided {@code simpleNameSynthesizer}. */
  public static List<String> synthesizeClassComponents(
      TypeDescriptor originalType, Function<String, String> simpleNameSynthesizer) {
    List<String> classComponents = Lists.newArrayList(originalType.getClassComponents());
    int simpleNameIndex = classComponents.size() - 1;
    classComponents.set(
        simpleNameIndex, simpleNameSynthesizer.apply(classComponents.get(simpleNameIndex)));
    return classComponents;
  }

  /** Returns new synthesized inner class components. */
  public static List<String> synthesizeInnerClassComponents(
      TypeDescriptor enclosingType, Object... parts) {
    List<String> classComponents = Lists.newArrayList(enclosingType.getClassComponents());
    classComponents.add("$" + Joiner.on("$").skipNulls().join(parts));
    return classComponents;
  }

  /** Returns the class initializer method descriptor for a particular type */
  public static MethodDescriptor getClinitMethodDescriptor(DeclaredTypeDescriptor typeDescriptor) {
    return MethodDescriptor.newBuilder()
        .setStatic(true)
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setName(MethodDescriptor.CLINIT_METHOD_NAME)
        .setOrigin(MethodOrigin.SYNTHETIC_CLASS_INITIALIZER)
        .setJsInfo(JsInfo.RAW)
        .build();
  }

  /** Returns the instance initializer method descriptor for a particular type */
  public static MethodDescriptor getInitMethodDescriptor(DeclaredTypeDescriptor typeDescriptor) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(typeDescriptor)
        .setName(getInitName(typeDescriptor))
        .setVisibility(Visibility.PRIVATE)
        .setOrigin(MethodOrigin.SYNTHETIC_INSTANCE_INITIALIZER)
        .setJsInfo(JsInfo.RAW)
        .build();
  }

  /** Returns the name of $init method for a type. */
  private static String getInitName(DeclaredTypeDescriptor typeDescriptor) {
    // Synthesize a name that is unique per class to avoid property clashes in JS.
    return MethodDescriptor.INIT_METHOD_PREFIX
        + "__"
        + ManglingNameUtils.getMangledName(typeDescriptor);
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
        .setJsInfo(jsInfo)
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
  public static ExpressionStatement getConstructorInvocationStatement(Method method) {
    checkArgument(method.isConstructor());
    for (Statement statement : method.getBody().getStatements()) {
      if (!(statement instanceof ExpressionStatement)) {
        continue;
      }
      Expression expression = ((ExpressionStatement) statement).getExpression();
      if (!(expression instanceof MethodCall)) {
        continue;
      }
      MethodCall methodCall = (MethodCall) expression;
      if (methodCall.getTarget().isConstructor()) {
        return (ExpressionStatement) statement;
      }
    }
    return null;
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor, or
   * returns null if it does not have one.
   */
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

  /** Returns the added field descriptor corresponding to the captured variable. */
  public static FieldDescriptor getFieldDescriptorForCapture(
      DeclaredTypeDescriptor enclosingTypeDescriptor, Variable capturedVariable) {
    return FieldDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setName(CAPTURES_PREFIX + capturedVariable.getName())
        .setVariableCapture(true)
        .setTypeDescriptor(capturedVariable.getTypeDescriptor())
        .setStatic(false)
        .setFinal(true)
        .setSynthetic(true)
        .setJsInfo(JsInfo.RAW_FIELD)
        .build();
  }

  /** Returns the added field corresponding to the enclosing instance. */
  public static FieldDescriptor getFieldDescriptorForEnclosingInstance(
      DeclaredTypeDescriptor enclosingClassDescriptor, TypeDescriptor fieldTypeDescriptor) {
    return FieldDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingClassDescriptor)
        .setName(ENCLOSING_INSTANCE_NAME)
        .setFinal(true)
        .setEnclosingInstanceCapture(true)
        .setSynthetic(true)
        .setTypeDescriptor(fieldTypeDescriptor)
        .build();
  }

  /** Returns the added outer parameter in constructor corresponding to the added field. */
  public static Variable createOuterParamByField(Field field) {
    return Variable.newBuilder()
        .setName(field.getDescriptor().getName())
        .setTypeDescriptor(field.getDescriptor().getTypeDescriptor())
        .setParameter(true)
        .setFinal(true)
        .build();
  }

  /**
   * Creates static forwarding method that has the same signature of {@code targetMethodDescriptor}
   * in type {@code fromTypeDescriptor}, and delegates to {@code targetMethodDescriptor}, e.g.
   *
   * <p>fromTypeDescriptor.method (args) { return Target.prototype.method.call(this,args); } }
   */
  public static Method createStaticForwardingMethod(
      SourcePosition sourcePosition,
      MethodDescriptor targetMethodDescriptor,
      DeclaredTypeDescriptor fromTypeDescriptor,
      String jsDocDescription) {

    /**
     * When synthesizing methods in a class it is never OK to be accidentally referencing type
     * variables from some other class (since the output will reference template variables that are
     * unknown in the current context). So make sure that the descriptor for this new method has
     * been adequately specialized into the context of the class into which it is going to be
     * placed.
     */
    Map<TypeDescriptor, TypeDescriptor> specializedTypeArgumentByTypeParameters =
        fromTypeDescriptor.getSpecializedTypeArgumentByTypeParameters();

    return createForwardingMethod(
        sourcePosition,
        null,
        MethodDescriptor.Builder.from(
                targetMethodDescriptor.specializeTypeVariables(
                    specializedTypeArgumentByTypeParameters))
            .setEnclosingTypeDescriptor(fromTypeDescriptor)
            // TODO(b/35802406): don't synthesize methods with a separate declaration site.
            .setDeclarationMethodDescriptor(targetMethodDescriptor)
            .setSynthetic(true)
            .setBridge(true)
            .setAbstract(false)
            .setNative(false)
            .build(),
        MethodDescriptor.Builder.from(
                targetMethodDescriptor.specializeTypeVariables(
                    specializedTypeArgumentByTypeParameters))
            .setDeclarationMethodDescriptor(targetMethodDescriptor)
            .build(),
        jsDocDescription,
        true,
        true);
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
      String jsDocDescription,
      boolean isOverride) {
    return createForwardingMethod(
        sourcePosition,
        qualifier,
        fromMethodDescriptor,
        toMethodDescriptor,
        jsDocDescription,
        false,
        isOverride);
  }

  private static Method createForwardingMethod(
      SourcePosition sourcePosition,
      Expression qualifier,
      MethodDescriptor fromMethodDescriptor,
      MethodDescriptor toMethodDescriptor,
      String jsDocDescription,
      boolean isStaticDispatch,
      boolean isOverride) {
    checkArgument(!fromMethodDescriptor.getEnclosingTypeDescriptor().isInterface());

    List<Variable> parameters =
        createParameterVariables(fromMethodDescriptor.getParameterTypeDescriptors());

    Statement statement =
        createForwardingStatement(
            sourcePosition,
            qualifier,
            toMethodDescriptor,
            isStaticDispatch,
            parameters,
            fromMethodDescriptor.getReturnTypeDescriptor());
    return Method.newBuilder()
        .setMethodDescriptor(fromMethodDescriptor)
        .setParameters(parameters)
        .addStatements(statement)
        .setOverride(isOverride)
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
      List<Variable> parameters,
      TypeDescriptor returnTypeDescriptor) {

    List<Expression> arguments = parameters.stream().map(Variable::getReference).collect(toList());

    AstUtils.maybePackageVarargs(toMethodDescriptor, arguments);

    // TODO(rluble): Casts are probably needed on arguments if the types differ between the
    // targetMethodDescriptor and its declarationMethodDescriptor.
    Expression forwardingMethodCall =
        MethodCall.Builder.from(toMethodDescriptor)
            .setQualifier(qualifier)
            .setArguments(arguments)
            .setStaticDispatch(isStaticDispatch)
            .build();

    return createReturnOrExpressionStatement(
        sourcePosition, forwardingMethodCall, returnTypeDescriptor);
  }

  /**
   * Creates devirtualized method call of {@code methodCall} as method call to the static method in
   * {@code targetTypeDescriptor} with the {@code instanceTypeDescriptor} as the first parameter
   * type.
   *
   * <p>instance.instanceMethod(a, b) => staticMethod(instance, a, b)
   */
  public static MethodCall createDevirtualizedMethodCall(
      MethodCall methodCall,
      DeclaredTypeDescriptor targetTypeDescriptor,
      TypeDescriptor sourceTypeDescriptor) {
    MethodDescriptor targetMethodDescriptor = methodCall.getTarget();
    checkArgument(!targetMethodDescriptor.isConstructor());
    checkArgument(!targetMethodDescriptor.isStatic());

    MethodDescriptor declarationMethodDescriptor =
        MethodDescriptor.Builder.from(targetMethodDescriptor.getDeclarationDescriptor())
            .setEnclosingTypeDescriptor(targetTypeDescriptor)
            .setStatic(true)
            .setAbstract(false)
            .setJsInfo(JsInfo.NONE)
            .build();

    MethodDescriptor methodDescriptor =
        MethodDescriptor.Builder.from(targetMethodDescriptor)
            .setDeclarationMethodDescriptor(declarationMethodDescriptor)
            .setEnclosingTypeDescriptor(targetTypeDescriptor)
            .addParameterTypeDescriptors(0, sourceTypeDescriptor)
            .setStatic(true)
            .setAbstract(false)
            .setJsInfo(JsInfo.NONE)
            .build();

    List<Expression> arguments = methodCall.getArguments();
    // Turn the instance into now a first parameter to the devirtualized method.
    Expression instance = checkNotNull(methodCall.getQualifier());
    arguments.add(0, instance);
    // Call the method like Objects.foo(instance, ...)
    return MethodCall.Builder.from(methodDescriptor).setArguments(arguments).build();
  }

  public static MethodCall createDevirtualizedMethodCall(
      MethodCall methodCall, DeclaredTypeDescriptor targetTypeDescriptor) {
    return createDevirtualizedMethodCall(
        methodCall, targetTypeDescriptor, methodCall.getTarget().getEnclosingTypeDescriptor());
  }
  
  /**
   * Boxes {@code expression} using the valueOf() method of the corresponding boxed type. e.g.
   * expression => Integer.valueOf(expression).
   */
  public static Expression box(Expression expression) {
    PrimitiveTypeDescriptor primitiveType =
        (PrimitiveTypeDescriptor) expression.getTypeDescriptor();
    checkArgument(!TypeDescriptors.isPrimitiveVoid(primitiveType));
    checkArgument(!TypeDescriptors.isPrimitiveBooleanOrDouble(primitiveType));
    DeclaredTypeDescriptor boxType = TypeDescriptors.getBoxTypeFromPrimitiveType(primitiveType);

    MethodDescriptor valueOfMethodDescriptor =
        boxType.getMethodDescriptorByName(MethodDescriptor.VALUE_OF_METHOD_NAME, primitiveType);
    return MethodCall.Builder.from(valueOfMethodDescriptor).setArguments(expression).build();
  }

  /**
   * Unboxes {expression} using the ***Value() method of the corresponding boxed type. e.g
   * expression => expression.intValue().
   */
  public static Expression unbox(Expression expression) {
    DeclaredTypeDescriptor boxType =
        (DeclaredTypeDescriptor) expression.getTypeDescriptor().getRawTypeDescriptor();
    checkArgument(TypeDescriptors.isBoxedType(boxType));
    TypeDescriptor primitiveType = boxType.unboxType();

    MethodDescriptor valueMethodDescriptor =
        boxType.getMethodDescriptorByName(
            primitiveType.getSimpleSourceName() + MethodDescriptor.VALUE_METHOD_SUFFIX);

    // We want "(a ? b : c).intValue()", not "a ? b : c.intValue()".
    expression =
        isValidMethodCallQualifier(expression)
            ? expression
            : MultiExpression.newBuilder().setExpressions(expression).build();

    MethodCall methodCall =
        MethodCall.Builder.from(valueMethodDescriptor).setQualifier(expression).build();
    if (TypeDescriptors.isBoxedBooleanOrDouble(boxType)) {
      methodCall = createDevirtualizedMethodCall(methodCall, boxType);
    }
    return methodCall;
  }

  /** Returns whether the given expression is a syntactically valid qualifier for a MethodCall. */
  private static boolean isValidMethodCallQualifier(Expression expression) {
    return !(expression instanceof ConditionalExpression
        || expression instanceof BinaryExpression
        || expression instanceof PrefixExpression
        || expression instanceof PostfixExpression);
  }

  public static boolean isDelegatedConstructorCall(
      MethodCall methodCall, TypeDescriptor targetTypeDescriptor) {
    if (methodCall == null || !methodCall.getTarget().isConstructor()) {
      return false;
    }
    return methodCall.getTarget().isMemberOf(targetTypeDescriptor);
  }

  /**
   * See JLS 5.2.
   *
   * <p>Note that compound assignments are excluded here. The assignment context arising from
   * compound assignments requires the expression to be rewritten into a plain assignment.
   */
  public static boolean matchesAssignmentContext(BinaryOperator binaryOperator) {
    return binaryOperator == BinaryOperator.ASSIGN;
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
  public static boolean matchesUnaryNumericPromotionContext(TypeDescriptor returnTypeDescriptor) {
    return TypeDescriptors.isBoxedOrPrimitiveType(returnTypeDescriptor);
  }

  /** See JLS 5.6.1. */
  public static boolean matchesUnaryNumericPromotionContext(BinaryExpression binaryExpression) {
    return binaryExpression.getOperator().isShiftOperator();
  }

  /** See JLS 5.6.2. */
  public static boolean matchesBinaryNumericPromotionContext(BinaryExpression binaryExpression) {
    // Both operands must be boxed or primitive.
    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();
    BinaryOperator operator = binaryExpression.getOperator();

    return matchesBinaryNumericPromotionContext(leftOperand, operator, rightOperand);
  }

  /** See JLS 5.1. */
  public static boolean matchesBooleanConversionContext(BinaryOperator operator) {
    switch (operator) {
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
        return true; // Booleans get these operators.
      default:
        return false;
    }
  }

  /** See JLS 5.1. */
  public static boolean matchesBooleanConversionContext(PrefixOperator operator) {
    return operator == PrefixOperator.NOT;
  }

  /** See JLS 5.6.2. */
  public static boolean matchesBinaryNumericPromotionContext(
      Expression leftOperand, BinaryOperator operator, Expression rightOperand) {
    if (!TypeDescriptors.isBoxedOrPrimitiveType(leftOperand.getTypeDescriptor())
        || !TypeDescriptors.isBoxedOrPrimitiveType(rightOperand.getTypeDescriptor())) {
      return false;
    }

    boolean leftIsPrimitive = leftOperand.getTypeDescriptor().isPrimitive();
    boolean rightIsPrimitive = rightOperand.getTypeDescriptor().isPrimitive();

    switch (operator.isCompoundAssignment() ? operator.getUnderlyingBinaryOperator() : operator) {
      case TIMES:
      case DIVIDE:
      case REMAINDER:
      case PLUS:
      case MINUS:
      case LESS:
      case GREATER:
      case LESS_EQUALS:
      case GREATER_EQUALS:
      case BIT_XOR:
      case BIT_AND:
      case BIT_OR:
      case LEFT_SHIFT:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
        return true; // Both numerics and booleans get these operators.
      case EQUALS:
      case NOT_EQUALS:
        return leftIsPrimitive || rightIsPrimitive; // Equality is sometimes instance comparison.
      default:
        return false;
    }
  }

  /**
   * See JLS 5.6.2. <code>
   * X and double -> double
   * X and float -> float
   * X and long -> long
   * otherwise -> int
   * </code>
   */
  public static TypeDescriptor chooseWidenedTypeDescriptor(
      TypeDescriptor leftTypeDescriptor, TypeDescriptor rightTypeDescriptor) {
    checkArgument(leftTypeDescriptor.isPrimitive());
    checkArgument(rightTypeDescriptor.isPrimitive());

    // Pick the wider of the two provided type descriptors.
    TypeDescriptor widenedTypeDescriptor =
        TypeDescriptors.getWidth(leftTypeDescriptor) > TypeDescriptors.getWidth(rightTypeDescriptor)
            ? leftTypeDescriptor
            : rightTypeDescriptor;

    // If it's smaller than int then use int.
    if (TypeDescriptors.getWidth(widenedTypeDescriptor)
        < TypeDescriptors.getWidth(TypeDescriptors.get().primitiveInt)) {
      widenedTypeDescriptor = TypeDescriptors.get().primitiveInt;
    }

    return widenedTypeDescriptor;
  }

  public static MethodDescriptor getStringValueOfMethodDescriptor(TypeDescriptor typeDescriptor) {

    // Find the right overload.
    if (TypeDescriptors.isPrimitiveByte(typeDescriptor)
        || TypeDescriptors.isPrimitiveShort(typeDescriptor)) {
      typeDescriptor = TypeDescriptors.get().primitiveInt;
    } else if (!typeDescriptor.isPrimitive()) {
      typeDescriptor = TypeDescriptors.get().javaLangObject;
    }

    return TypeDescriptors.get()
        .javaLangString
        .getMethodDescriptorByName(MethodDescriptor.VALUE_OF_METHOD_NAME, typeDescriptor);
  }

  /**
   * Returns true if {@code expression} is a string literal or if it is a BinaryExpression that
   * matches String conversion context and one of its operands is non null String.
   */
  public static boolean isNonNullString(Expression expression) {
    if (expression instanceof StringLiteral) {
      return true;
    }
    if (!(expression instanceof BinaryExpression)) {
      return false;
    }
    BinaryExpression binaryExpression = (BinaryExpression) expression;
    return matchesStringContext(binaryExpression);
  }

  /**
   * Returns explicit qualifier for member reference (field access or method call).
   *
   * <p>If {@code qualifier} is null, returns EnclosingTypeDescriptor as the qualifier for static
   * method/field and 'this' reference as the qualifier for instance method/field.
   */
  static Expression getExplicitQualifier(Expression qualifier, MemberDescriptor memberDescriptor) {
    checkNotNull(memberDescriptor);
    if (qualifier != null) {
      return qualifier;
    }
    DeclaredTypeDescriptor enclosingTypeDescriptor = memberDescriptor.getEnclosingTypeDescriptor();
    return memberDescriptor.isStatic()
        ? new JavaScriptConstructorReference(enclosingTypeDescriptor.getRawTypeDescriptor())
        : new ThisReference(enclosingTypeDescriptor);
  }

  /** Returns true if the qualifier of the given member reference is 'this' reference. */
  public static boolean hasThisReferenceAsQualifier(MemberReference memberReference) {
    Expression qualifier = memberReference.getQualifier();
    return qualifier instanceof ThisReference
        && memberReference.getTarget().isMemberOf(qualifier.getTypeDescriptor());
  }

  public static Expression joinExpressionsWithBinaryOperator(
      BinaryOperator operator, List<Expression> expressions) {
    if (expressions.isEmpty()) {
      return null;
    }
    if (expressions.size() == 1) {
      return expressions.get(0);
    }
    Expression joinedExpressions =
        joinExpressionsWithBinaryOperator(operator, expressions.subList(1, expressions.size()));
    return BinaryExpression.newBuilder()
        .setLeftOperand(expressions.get(0))
        .setOperator(operator)
        .setRightOperand(joinedExpressions)
        .build();
  }

  public static Method createDevirtualizedMethod(Method method) {
    checkArgument(
        !method.getDescriptor().isJsPropertyGetter()
            && !method.getDescriptor().isJsPropertySetter(),
        "JsPropery getter and setters should never be devirtualized " + method);
    checkArgument(method.getDescriptor().isPolymorphic());
    // TODO(rluble): remove once init() function is synthesized in AST.
    checkArgument(!method.getDescriptor().isInit(), "Do not devirtualize init().");

    final Variable thisArg =
        Variable.newBuilder()
            .setName("$thisArg")
            .setTypeDescriptor(method.getDescriptor().getEnclosingTypeDescriptor())
            .setParameter(true)
            .setFinal(true)
            .build();

    // Replace all 'this' references in the method with parameter references.
    method.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteThisReference(ThisReference thisReference) {
            return thisArg.getReference();
          }
        });

    // MethodDescriptor for the devirtualized static method.
    MethodDescriptor newMethodDescriptor =
        makeDevirtualizedMethodDescriptor(method.getDescriptor());

    // Add the static method to current type.
    return Method.newBuilder()
        .setMethodDescriptor(newMethodDescriptor)
        .setParameters(
            ImmutableList.<Variable>builder().add(thisArg).addAll(method.getParameters()).build())
        .addStatements(method.getBody().getStatements())
        .setSourcePosition(method.getSourcePosition())
        .build();
  }

  /**
   * Two methods are parameter erasure equal if the erasure of their parameters' types are equal.
   */
  public static boolean areParameterErasureEqual(MethodDescriptor left, MethodDescriptor right) {
    List<TypeDescriptor> leftParameterTypeDescriptors =
        left.getDeclarationDescriptor().getParameterTypeDescriptors();
    List<TypeDescriptor> rightParameterTypeDescriptors =
        right.getDeclarationDescriptor().getParameterTypeDescriptors();

    if (!left.getName().equals(right.getName())
        || leftParameterTypeDescriptors.size() != rightParameterTypeDescriptors.size()) {
      return false;
    }
    for (int i = 0; i < leftParameterTypeDescriptors.size(); i++) {
      if (!leftParameterTypeDescriptors
          .get(i)
          .hasSameRawType(rightParameterTypeDescriptors.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Generates the following code:
   *
   * <p>$Util.$makeLambdaFunction( $Util.$getPrototype(Type).m_equal, $instance, Type.$copy);
   */
  public static MethodCall createLambdaInstance(
      DeclaredTypeDescriptor lambdaType, Expression instance) {
    checkArgument(lambdaType.isJsFunctionImplementation());

    // Use the method from the interface instead instead of the implementation method, since it is
    // the appropriate semantic behaviour. The function might be called(directly as a function) from
    // as a @JsFunction which might require dispatch through the bridge.
    MethodDescriptor jsFunctionMethodDescriptor =
        lambdaType.getFunctionalInterface().getJsFunctionMethodDescriptor();

    // Class.prototype.apply
    String functionalMethodMangledName =
        ManglingNameUtils.getMangledName(jsFunctionMethodDescriptor);

    MethodCall getPrototypeCall =
        RuntimeMethods.createUtilMethodCall(
            "$getPrototype", new JavaScriptConstructorReference(lambdaType));

    FieldAccess applyFunctionFieldAccess =
        FieldAccess.Builder.from(
                FieldDescriptor.newBuilder()
                    .setEnclosingTypeDescriptor(lambdaType)
                    .setName(functionalMethodMangledName)
                    .setTypeDescriptor(TypeDescriptors.get().nativeFunction)
                    .setJsInfo(JsInfo.RAW_FIELD)
                    .build())
            .setQualifier(getPrototypeCall)
            .build();

    FieldAccess copyFunctionFieldAccess =
        FieldAccess.Builder.from(
                FieldDescriptor.newBuilder()
                    .setEnclosingTypeDescriptor(lambdaType)
                    .setName("$copy")
                    .setTypeDescriptor(TypeDescriptors.get().nativeFunction)
                    .setJsInfo(JsInfo.RAW_FIELD)
                    .build())
            .setQualifier(new JavaScriptConstructorReference(lambdaType))
            .build();

    return RuntimeMethods.createUtilMethodCall(
        "$makeLambdaFunction", applyFunctionFieldAccess, instance, copyFunctionFieldAccess);
  }

  private static Expression getInitialValue(Field field) {
    if (field.isCompileTimeConstant()) {
      return field.getInitializer();
    }
    return TypeDescriptors.getDefaultValue(field.getDescriptor().getTypeDescriptor());
  }

  /** Returns a field declaration statement. */
  public static Statement declarationStatement(Field field, SourcePosition sourcePosition) {
    boolean isPublic = !field.isStatic() || field.isCompileTimeConstant();

    // Only skip declaration on static fields.
    boolean skipNullInitialization =
        field.isStatic() && getInitialValue(field) == NullLiteral.get();

    Expression declarationExpression =
        skipNullInitialization
            ? FieldAccess.newBuilder().setTargetFieldDescriptor(field.getDescriptor()).build()
            : BinaryExpression.Builder.asAssignmentTo(field)
                .setRightOperand(getInitialValue(field))
                .build();

    return JsDocFieldDeclaration.newBuilder()
        .setExpression(declarationExpression)
        .setFieldType(field.getDescriptor().getTypeDescriptor())
        .setPublic(isPublic)
        .setConst(field.isCompileTimeConstant())
        .build()
        .makeStatement(field.isCompileTimeConstant() ? field.getSourcePosition() : sourcePosition);
  }

  public static String getSimpleSourceName(List<String> classComponents) {
    String simpleName = Iterables.getLast(classComponents);
    // If the user opted in to declareLegacyNamespaces, then JSCompiler will complain when seeing
    // namespaces like "foo.bar.Baz.4". Prefix anonymous numbered classes with a string to make
    // JSCompiler happy.
    return startsWithNumber(simpleName) ? "$" + simpleName : simpleName;
  }

  @SuppressWarnings("unchecked")
  /** Clones a list of expressions */
  public static <T extends Cloneable<?>> List<T> clone(List<T> nodes) {
    return nodes.stream().map(node -> (T) node.clone()).collect(toImmutableList());
  }

  @SuppressWarnings("unchecked")
  /** Clones a cloneable or returns null */
  public static <T extends Cloneable<?>> T clone(T node) {
    return node != null ? (T) node.clone() : null;
  }

  @SuppressWarnings("unchecked")
  /**
   * Replaces references to variables in {@code fromVariables} to reference to variables in {@code
   * toVariables}.
   */
  public static <T extends Node> List<T> replaceVariables(
      List<Variable> fromVariables, List<Variable> toVariable, List<T> nodes) {
    List<T> result = new ArrayList<>();
    for (T node : nodes) {
      result.add(replaceVariables(fromVariables, toVariable, node));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  /**
   * Replaces references to variables in {@code fromVariables} to reference to variables in {@code
   * toVariables}.
   */
  public static <T extends Node> T replaceVariables(
      List<Variable> fromVariables, List<Variable> toVariable, T node) {
    class VariableReplacer extends AbstractRewriter {
      Map<Variable, Variable> toVariableByFromVariable = new HashMap<>();

      public VariableReplacer(List<Variable> fromVariables, List<Variable> toVariables) {
        checkArgument(fromVariables.size() == toVariables.size());
        for (int i = 0; i < fromVariables.size(); i++) {
          toVariableByFromVariable.put(fromVariables.get(i), toVariables.get(i));
        }
      }

      @Override
      public Variable rewriteVariable(Variable variable) {
        Variable toVariable = toVariableByFromVariable.get(variable);
        return toVariable == null ? variable : toVariable;
      }

      @Override
      public Expression rewriteVariableReference(VariableReference variableReference) {
        Variable toVariable = toVariableByFromVariable.get(variableReference.getTarget());
        if (toVariable != null) {
          return toVariable.getReference();
        }
        return variableReference;
      }
    }

    return (T) node.accept(new VariableReplacer(fromVariables, toVariable));
  }

  /** Get a list of references for {@code variables}. */
  public static List<Expression> getReferences(List<Variable> variables) {
    return variables.stream().map(Variable::getReference).collect(toImmutableList());
  }

  /** Creates an implicit constructor that forwards to a specific super constructor. */
  public static Method createImplicitAnonymousClassConstructor(
      SourcePosition sourcePosition,
      MethodDescriptor constructorDescriptor,
      MethodDescriptor superConstructorDescriptor) {

    // If the super qualifier is explicit then it is passed as the first parameter and the super
    // constructor has 1 parameter less than the constructor being build.
    boolean firstParameterIsSuperQualifier =
        constructorDescriptor.getParameterTypeDescriptors().size()
            != superConstructorDescriptor.getParameterTypeDescriptors().size();

    List<Variable> constructorParameters = new ArrayList<>();
    int index = 0;
    for (TypeDescriptor parameterTypeDescriptor :
        constructorDescriptor.getParameterTypeDescriptors()) {
      String parameterName =
          firstParameterIsSuperQualifier && index == 0 ? "$super_outer_this" : "$_" + index;
      constructorParameters.add(
          Variable.newBuilder()
              .setName(parameterName)
              .setTypeDescriptor(parameterTypeDescriptor)
              .build());
      index++;
    }

    Expression qualifier =
        firstParameterIsSuperQualifier ? constructorParameters.get(0).getReference() : null;

    List<Expression> superConstructorArguments =
        (firstParameterIsSuperQualifier
                ? constructorParameters.subList(1, constructorParameters.size())
                : constructorParameters)
            .stream()
            .map(Variable::getReference)
            .collect(toImmutableList());

    return Method.newBuilder()
        .setMethodDescriptor(constructorDescriptor)
        .setParameters(constructorParameters)
        .addStatements(
            MethodCall.Builder.from(superConstructorDescriptor)
                .setQualifier(qualifier)
                .setArguments(superConstructorArguments)
                .build()
                .makeStatement(sourcePosition))
        .setSourcePosition(sourcePosition)
        .build();
  }

  public static Expression removeJsDocCastIfPresent(Expression expression) {
    if (expression instanceof JsDocCastExpression) {
      return ((JsDocCastExpression) expression).getExpression();
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
    return TypeDescriptors.isPrimitiveVoid(methodReturnTypeDescriptor)
        ? expression.makeStatement(sourcePosition)
        : ReturnStatement.newBuilder()
            .setExpression(expression)
            .setTypeDescriptor(methodReturnTypeDescriptor)
            .setSourcePosition(sourcePosition)
            .build();
  }

  public static void updateMethodsBySignature(
      Map<String, MethodDescriptor> methodsBySignature,
      Iterable<MethodDescriptor> methodDescriptors) {
    for (MethodDescriptor declaredMethod : methodDescriptors) {
      MethodDescriptor existingMethod = methodsBySignature.get(declaredMethod.getMethodSignature());
      // TODO(rluble): implement correct default replacement when existing method != null.
      // Only replace the method if we found a default definition that implements the method at
      // that type; be sure to have all relevant examples, the semantics are quite particular.
      if (existingMethod == null) {
        methodsBySignature.put(declaredMethod.getMethodSignature(), declaredMethod);
      }
    }
  }

  public static boolean startsWithNumber(String string) {
    return !string.isEmpty() && Character.isDigit(string.charAt(0));
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
    List<String> namespaceComponents = components.subList(0, components.size() - 1);
    boolean isExtern = namespaceComponents.size() < 1;
    String jsName = Iterables.getLast(components);

    if (isExtern) {
      return TypeDescriptors.createGlobalNativeTypeDescriptor(jsName);
    }

    String jsNamespace = Joiner.on(".").join(namespaceComponents);
    TypeDescriptor enclosingClassTypeDescriptor =
        getOutermostEnclosingType(memberDescriptor.getEnclosingTypeDescriptor());
    String packageName =
        Joiner.on(".").join(enclosingClassTypeDescriptor.getQualifiedSourceName(), jsNamespace);

    return TypeDescriptors.createNativeTypeDescriptor(packageName, jsName, jsNamespace);
  }

  private static TypeDescriptor getOutermostEnclosingType(DeclaredTypeDescriptor typeDescriptor) {
    if (typeDescriptor.getEnclosingTypeDescriptor() == null) {
      return typeDescriptor;
    }
    return getOutermostEnclosingType(typeDescriptor.getEnclosingTypeDescriptor());
  }

  /**
   * Creates a devritualized static MethodDescriptor from an instance MethodDescriptor.
   *
   * <p>The static MethodDescriptor has an extra parameter as its first parameter whose type is the
   * enclosing class of {@code methodDescriptor}.
   */
  public static MethodDescriptor makeDevirtualizedMethodDescriptor(
      MethodDescriptor methodDescriptor) {
    if (methodDescriptor.isStatic() || methodDescriptor.isConstructor()) {
      return methodDescriptor;
    }
    DeclaredTypeDescriptor enclosingTypeDescriptor = methodDescriptor.getEnclosingTypeDescriptor();

    MethodDescriptor.Builder methodBuilder =
        MethodDescriptor.Builder.from(methodDescriptor)
            .setParameterDescriptors(
                ImmutableList.<ParameterDescriptor>builder()
                    .add(
                        ParameterDescriptor.newBuilder()
                            .setTypeDescriptor(enclosingTypeDescriptor)
                            .build())
                    .addAll(methodDescriptor.getParameterDescriptors())
                    .build())
            .setTypeParameterTypeDescriptors(
                ImmutableList.<TypeDescriptor>builder()
                    .addAll(methodDescriptor.getTypeParameterTypeDescriptors())
                    .addAll(enclosingTypeDescriptor.getTypeArgumentDescriptors())
                    .build())
            .setStatic(true)
            .setAbstract(false)
            .setJsInfo(JsInfo.NONE);

    if (methodDescriptor != methodDescriptor.getDeclarationDescriptor()) {
      methodBuilder.setDeclarationMethodDescriptor(
          makeDevirtualizedMethodDescriptor(
              MethodDescriptor.Builder.from(methodDescriptor.getDeclarationDescriptor())
                  .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
                  .build()));
    }
    return methodBuilder.build();
  }

  /** Returns an Optional.empty() if optionalSourcePosition is empty or unnamed */
  public static Optional<SourcePosition> emptySourcePositionIfNotNamed(
      Optional<SourcePosition> optionalSourcePosition) {
    return optionalSourcePosition.flatMap(
        sourcePosition ->
            sourcePosition.getName() == null ? Optional.empty() : Optional.of(sourcePosition));
  }

  /** Replaces the var arguments with packaged array. */
  public static void maybePackageVarargs(
      MethodDescriptor methodDescriptor, List<Expression> arguments) {
    if (shouldPackageVarargs(methodDescriptor, arguments)) {
      Expression packagedVarargs = getPackagedVarargs(methodDescriptor, arguments);
      int parameterLength = methodDescriptor.getParameterDescriptors().size();
      while (arguments.size() >= parameterLength) {
        arguments.remove(parameterLength - 1);
      }
      arguments.add(packagedVarargs);
    }
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

    return lastArgument != NullLiteral.get()
        && !lastArgument.getTypeDescriptor().isAssignableTo(lastParameter.getTypeDescriptor());
  }

  /** Returns the package the varargs argument as an array. */
  private static Expression getPackagedVarargs(
      MethodDescriptor methodDescriptor, List<Expression> arguments) {
    checkArgument(methodDescriptor.isVarargs());
    int parametersLength = methodDescriptor.getParameterDescriptors().size();
    ParameterDescriptor varargsParameterDescriptor =
        methodDescriptor
            .getParameterDescriptors()
            .get(methodDescriptor.getParameterDescriptors().size() - 1);
    ArrayTypeDescriptor varargsTypeDescriptor =
        (ArrayTypeDescriptor) varargsParameterDescriptor.getTypeDescriptor();
    if (arguments.size() < parametersLength) {
      // no argument for the varargs, add an empty array.
      return new ArrayLiteral(varargsTypeDescriptor);
    }
    List<Expression> valueExpressions = new ArrayList<>();
    for (int i = parametersLength - 1; i < arguments.size(); i++) {
      valueExpressions.add(arguments.get(i));
    }
    if (varargsParameterDescriptor.isDoNotAutobox()) {
      checkArgument(
          TypeDescriptors.isJavaLangObject(varargsTypeDescriptor.getComponentTypeDescriptor()));
      // Use a NATIVE_OBJECT[] instead of Object[] for @DoNotAutobox varargs, so that the
      // boxing logic can avoid boxing here.
      varargsTypeDescriptor =
          ArrayTypeDescriptor.newBuilder()
              .setComponentTypeDescriptor(TypeDescriptors.get().nativeObject)
              .build();
    }
    return new ArrayLiteral(varargsTypeDescriptor, valueExpressions);
  }

  /**
   * Returns a reference to the JavaScript constructor to be used for array marking, instanceof and
   * casts. In most cases it the underlying JavaScript constructor for the class but not in all
   * (such as native @JsTypes and @JsFunctions).
   */
  public static JavaScriptConstructorReference getMetadataConstructorReference(
      TypeDescriptor typeDescriptor) {
    return new JavaScriptConstructorReference(typeDescriptor.getMetadataTypeDescriptor());
  }

  /** Whether this method require methods that override it to have an @override @JsDoc annotation */
  // TODO(b/31312257): fix or decide to not emit @override and suppress the error.
  public static boolean overrideNeedsAtOverrideAnnotation(MethodDescriptor overrideMethod) {
    return !overrideMethod.getEnclosingTypeDescriptor().isStarOrUnknown()
        && !overrideMethod.getEnclosingTypeDescriptor().isJsFunctionInterface();
  }
}
