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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Utility functions to manipulate J2CL AST.
 */
public class ASTUtils {
  public static final String CAPTURES_PREFIX = "$c_";
  public static final String ENCLOSING_INSTANCE_NAME = "$outer_this";
  public static final String CREATE_PREFIX = "$create_";
  public static final String TYPE_VARIABLE_IN_METHOD_PREFIX = "M_";
  public static final String TYPE_VARIABLE_IN_TYPE_PREFIX = "C_";
  public static final FieldDescriptor ARRAY_LENGTH_FIELD_DESCRIPTION =
      FieldDescriptor.createRaw(
          false,
          TypeDescriptors.VOID_TYPE_DESCRIPTOR,
          "length",
          TypeDescriptors.INT_TYPE_DESCRIPTOR);

  /**
   * Construct a new method descriptor for {@code methodDescriptor} with the additional
   * parameters {@code extraParameters} at the end.
   */
  public static MethodDescriptor addParametersToMethodDescriptor(
      MethodDescriptor methodDescriptor, TypeDescriptor... extraParameters) {
    return addParametersToMethodDescriptor(methodDescriptor, Arrays.asList(extraParameters));
  }

  /**
   * Construct a new method descriptor for {@code methodDescriptor} with the additional
   * parameters {@code extraParameters} at the end.
   */
  public static MethodDescriptor addParametersToMethodDescriptor(
      MethodDescriptor methodDescriptor, Collection<TypeDescriptor> extraParameters) {
    return MethodDescriptor.create(
        methodDescriptor.isStatic(),
        methodDescriptor.getVisibility(),
        methodDescriptor.getEnclosingClassTypeDescriptor(),
        methodDescriptor.getMethodName(),
        methodDescriptor.isConstructor(),
        methodDescriptor.isNative(),
        methodDescriptor.getReturnTypeDescriptor(),
        Iterables.concat(methodDescriptor.getParameterTypeDescriptors(), extraParameters));
  }

  /**
   * Create "$init" MethodDescriptor.
   */
  public static MethodDescriptor createInitMethodDescriptor(
      TypeDescriptor enclosingClassTypeDescriptor) {
    return MethodDescriptor.create(
        false,
        Visibility.PRIVATE,
        enclosingClassTypeDescriptor,
        MethodDescriptor.METHOD_INIT,
        false,
        false,
        TypeDescriptors.VOID_TYPE_DESCRIPTOR);
  }

  /**
   * Create constructor MethodDescriptor.
   */
  public static MethodDescriptor createConstructorDescriptor(
      TypeDescriptor typeDescriptor,
      Visibility visibility,
      TypeDescriptor... parameterTypeDescriptors) {
    return MethodDescriptor.create(
        false,
        visibility,
        typeDescriptor,
        typeDescriptor.getClassName(),
        true,
        false,
        TypeDescriptors.VOID_TYPE_DESCRIPTOR,
        parameterTypeDescriptors);
  }

  /**
   * Returns the constructor invocation (super call or this call) in a specified constructor,
   * or returns null if it does not have one.
   */
  public static MethodCall getConstructorInvocation(Method method) {
    Preconditions.checkArgument(method.isConstructor());
    if (method.getBody().getStatements().isEmpty()) {
      return null;
    }
    Statement firstStatement = method.getBody().getStatements().get(0);
    if (!(firstStatement instanceof ExpressionStatement)) {
      return null;
    }
    Expression expression = ((ExpressionStatement) firstStatement).getExpression();
    if (!(expression instanceof MethodCall)) {
      return null;
    }
    MethodCall methodCall = (MethodCall) expression;
    return methodCall.getTarget().isConstructor() ? methodCall : null;
  }

  /**
   * Returns whether the specified constructor has a this() call.
   */
  public static boolean hasThisCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && constructorInvocation
            .getTarget()
            .getEnclosingClassTypeDescriptor()
            .equals(method.getDescriptor().getEnclosingClassTypeDescriptor());
  }

  /**
   * Returns whether the specified constructor has a super() call.
   */
  public static boolean hasSuperCall(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null
        && !constructorInvocation
            .getTarget()
            .getEnclosingClassTypeDescriptor()
            .equals(method.getDescriptor().getEnclosingClassTypeDescriptor());
  }

  /**
   * Returns whether the specified constructor has a this() or a super() call.
   */
  public static boolean hasConstructorInvocation(Method method) {
    MethodCall constructorInvocation = getConstructorInvocation(method);
    return constructorInvocation != null;
  }

  /**
   * The following is the cast table between primitive types. The cell marked as 'X'
   * indicates that no cast is needed.
   * <p>
   * For other cases, cast from A to B is translated to method call $castAToB.
   * <p>
   * The general pattern is that you need casts that shrink, all casts involving
   * 'long' (because it has a custom boxed implementation) and the byte->char and
   * char->short casts because char is unsigned.
   * <p>
   * from\to       byte |  char | short | int   | long | float | double|
   * -------------------------------------------------------------------
   * byte        |  X   |       |   X   |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * char        |      |   X   |       |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * short       |      |       |   X   |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * int         |      |       |       |   X   |      |   X   |   X   |
   * -------------------------------------------------------------------
   * long        |      |       |       |       |   X  |       |       |
   * -------------------------------------------------------------------
   * float       |      |       |       |       |      |   X   |   X   |
   * -------------------------------------------------------------------
   * double      |      |       |       |       |      |   X   |   X   |
   */
  public static boolean canRemoveCast(
      TypeDescriptor fromTypeDescriptor, TypeDescriptor toTypeDescriptor) {
    if (fromTypeDescriptor == toTypeDescriptor) {
      return true;
    }
    if (fromTypeDescriptor == TypeDescriptors.LONG_TYPE_DESCRIPTOR
        || toTypeDescriptor == TypeDescriptors.LONG_TYPE_DESCRIPTOR) {
      return false;
    }
    return toTypeDescriptor == TypeDescriptors.FLOAT_TYPE_DESCRIPTOR
        || toTypeDescriptor == TypeDescriptors.DOUBLE_TYPE_DESCRIPTOR
        || (toTypeDescriptor == TypeDescriptors.INT_TYPE_DESCRIPTOR
            && (fromTypeDescriptor == TypeDescriptors.BYTE_TYPE_DESCRIPTOR
                || fromTypeDescriptor == TypeDescriptors.CHAR_TYPE_DESCRIPTOR
                || fromTypeDescriptor == TypeDescriptors.SHORT_TYPE_DESCRIPTOR))
        || (toTypeDescriptor == TypeDescriptors.SHORT_TYPE_DESCRIPTOR
            && fromTypeDescriptor == TypeDescriptors.BYTE_TYPE_DESCRIPTOR);
  }

  public static boolean isShiftOperator(BinaryOperator binaryOperator) {
    switch (binaryOperator) {
      case LEFT_SHIFT:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
      case LEFT_SHIFT_ASSIGN:
      case RIGHT_SHIFT_SIGNED_ASSIGN:
      case RIGHT_SHIFT_UNSIGNED_ASSIGN:
        return false;
      default:
        return true;
    }
  }

  /**
   * Returns the added field descriptor corresponding to the captured variable.
   */
  public static FieldDescriptor getFieldDescriptorForCapture(
      TypeDescriptor enclosingClassTypeDescriptor, Variable capturedVariable) {
    return FieldDescriptor.createRaw(
        false,
        enclosingClassTypeDescriptor,
        CAPTURES_PREFIX + capturedVariable.getName(),
        capturedVariable.getTypeDescriptor());
  }

  /**
   * Returns the added field corresponding to the enclosing instance.
   */
  public static FieldDescriptor getFieldDescriptorForEnclosingInstance(
      TypeDescriptor enclosingClassDescriptor, TypeDescriptor fieldTypeDescriptor) {
    return FieldDescriptor.create(
        false, // not static
        Visibility.PUBLIC,
        enclosingClassDescriptor,
        ENCLOSING_INSTANCE_NAME,
        fieldTypeDescriptor);
  }

  /**
   * Returns the added outer parameter in constructor corresponding to the added field.
   */
  public static Variable createOuterParamByField(Field field) {
    return new Variable(
        field.getDescriptor().getFieldName(),
        field.getDescriptor().getTypeDescriptor(),
        true,
        true);
  }

  /**
   * Returns the MethodDescriptor for the wrapper function in outer class that creates its
   * inner class by calling the corresponding inner class constructor.
   */
  public static MethodDescriptor createMethodDescriptorForInnerClassCreation(
      final TypeDescriptor outerclassTypeDescriptor,
      MethodDescriptor innerclassConstructorDescriptor) {
    boolean isStatic = false;
    String methodName = CREATE_PREFIX + innerclassConstructorDescriptor.getMethodName();
    boolean isConstructor = false;
    boolean isNative = false;
    TypeDescriptor returnTypeDescriptor =
        innerclassConstructorDescriptor.getEnclosingClassTypeDescriptor();
    // if the inner class is a generic type, add its type parameters to the wrapper method.
    List<TypeDescriptor> typeParameterDescriptors = new ArrayList<>();
    TypeDescriptor innerclassTypeDescriptor =
        innerclassConstructorDescriptor.getEnclosingClassTypeDescriptor();
    if (innerclassTypeDescriptor.isParameterizedType()) {
      typeParameterDescriptors.addAll(
          Lists.newArrayList(
              Iterables.filter( // filters out the type parameters declared in the outer class.
                  innerclassTypeDescriptor.getTypeArgumentDescriptors(),
                  new Predicate<TypeDescriptor>() {
                    @Override
                    public boolean apply(TypeDescriptor typeParameter) {
                      return !outerclassTypeDescriptor
                          .getTypeArgumentDescriptors()
                          .contains(typeParameter);
                    }
                  })));
    }
    return MethodDescriptor.create(
        isStatic,
        innerclassConstructorDescriptor.getVisibility(),
        outerclassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        innerclassConstructorDescriptor.getParameterTypeDescriptors(),
        typeParameterDescriptors,
        innerclassConstructorDescriptor.getErasureMethodDescriptor());
  }

  /**
   * Returns the Method for the wrapper function in outer class that creates its inner class
   * by calling the corresponding inner class constructor.
   */
  public static Method createMethodForInnerClassCreation(
      final TypeDescriptor outerclassTypeDescriptor, Method innerclassConstructor) {
    MethodDescriptor innerclassConstructorDescriptor = innerclassConstructor.getDescriptor();
    MethodDescriptor methodDescriptor =
        createMethodDescriptorForInnerClassCreation(
            outerclassTypeDescriptor, innerclassConstructorDescriptor);

    // create arguments.
    List<Expression> arguments = new ArrayList<>();
    for (Variable parameter : innerclassConstructor.getParameters()) {
      arguments.add(new VariableReference(parameter));
    }
    // adds 'this' as the last argument.
    arguments.add(new ThisReference(outerclassTypeDescriptor));

    Expression newInnerClass = new NewInstance(null, innerclassConstructorDescriptor, arguments);
    List<Statement> statements = new ArrayList<>();
    statements.add(new ReturnStatement(newInnerClass)); // return new InnerClass();
    Block body = new Block(statements);

    return new Method(methodDescriptor, innerclassConstructor.getParameters(), body, false);
  }

  /**
   * Returns forwarding method for exposed package private methods (which means that one of its
   * overriding method is public or protected, so the package private method is exposed).
   * The forwarding method is like:
   * exposedMethodDescriptor (args) {return this.forwardToMethodDescriptor(args);}
   */
  public static Method createForwardingMethod(
      MethodDescriptor exposedMethodDescriptor, MethodDescriptor forwardToMethodDescriptor) {
    MethodDescriptor forwardinghMethodDescriptor =
        MethodDescriptor.create(
            exposedMethodDescriptor.isStatic(),
            exposedMethodDescriptor.getVisibility(),
            forwardToMethodDescriptor.getEnclosingClassTypeDescriptor(), // enclosing class
            exposedMethodDescriptor.getMethodName(),
            exposedMethodDescriptor.isConstructor(),
            false, // is native
            forwardToMethodDescriptor.getReturnTypeDescriptor(), // return type
            exposedMethodDescriptor.getParameterTypeDescriptors());
    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();
    for (int i = 0; i < exposedMethodDescriptor.getParameterTypeDescriptors().size(); i++) {
      Variable parameter =
          new Variable(
              "arg" + i, exposedMethodDescriptor.getParameterTypeDescriptors().get(i), false, true);
      parameters.add(parameter);
      arguments.add(parameter.getReference());
    }
    Expression forwardingMethodCall = new MethodCall(null, forwardToMethodDescriptor, arguments);
    Statement statement =
        exposedMethodDescriptor.getReturnTypeDescriptor() == TypeDescriptors.VOID_TYPE_DESCRIPTOR
            ? new ExpressionStatement(forwardingMethodCall)
            : new ReturnStatement(forwardingMethodCall);
    return Method.createSynthetic(
        forwardinghMethodDescriptor, parameters, new Block(Arrays.asList(statement)), true);
  }

  /**
   * Creates devirtualized method call of {@code methodCall} as method call to the static method
   * in {@code enclosingClassTypeDescriptor} with the {@code instanceTypeDescriptor} as the first
   * parameter type.
   */
  public static MethodCall createDevirtualizedMethodCall(
      MethodCall methodCall,
      TypeDescriptor enclosingClassTypeDescriptor,
      TypeDescriptor instanceTypeDescriptor) {
    MethodDescriptor targetMethodDescriptor = methodCall.getTarget();
    Preconditions.checkArgument(!targetMethodDescriptor.isConstructor());
    Preconditions.checkArgument(!targetMethodDescriptor.isStatic());

    MethodDescriptor methodDescriptor =
        MethodDescriptor.create(
            true, // Static method.
            targetMethodDescriptor.getVisibility(),
            enclosingClassTypeDescriptor, // enclosing class
            targetMethodDescriptor.getMethodName(),
            targetMethodDescriptor.isConstructor(),
            targetMethodDescriptor.isNative(),
            targetMethodDescriptor.getReturnTypeDescriptor(),
            Iterables.concat(
                Arrays.asList(instanceTypeDescriptor), // add the first parameter type.
                targetMethodDescriptor.getParameterTypeDescriptors()));
    @SuppressWarnings("unchecked")

    List<Expression> arguments = methodCall.getArguments();
    // Turn the instance into now a first parameter to the devirtualized method.
    Expression instance = methodCall.getQualifier();
    Preconditions.checkNotNull(instance);
    arguments.add(0, instance);
    // Call the method like Objects.foo(instance, ...)
    return new MethodCall(null, methodDescriptor, arguments);
  }
}
