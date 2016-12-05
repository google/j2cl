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
package com.google.j2cl.frontend;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsMemberType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptor.Kind;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.SingletonInitializer;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

/**
 * Utility functions to manipulate JDT internal representations.
 */
public class JdtUtils {
  // JdtUtil members are all package private. Code outside frontend should not be aware of the
  // dependency on JDT.
  static String getCompilationUnitPackageName(CompilationUnit compilationUnit) {
    return compilationUnit.getPackage() == null
        ? ""
        : compilationUnit.getPackage().getName().getFullyQualifiedName();
  }

  static FieldDescriptor createFieldDescriptor(IVariableBinding variableBinding) {
    if (isArrayLengthBinding(variableBinding)) {
      return AstUtils.ARRAY_LENGTH_FIELD_DESCRIPTION;
    }

    boolean isStatic = isStatic(variableBinding);
    Visibility visibility = getVisibility(variableBinding);
    TypeDescriptor enclosingClassTypeDescriptor =
        createTypeDescriptor(variableBinding.getDeclaringClass());
    String fieldName = variableBinding.getName();

    TypeDescriptor thisTypeDescriptor =
        createTypeDescriptorWithNullability(
            variableBinding.getType(), variableBinding.getAnnotations());

    FieldDescriptor declarationFieldDescriptor = null;
    if (variableBinding.getVariableDeclaration() != variableBinding) {
      declarationFieldDescriptor = createFieldDescriptor(variableBinding.getVariableDeclaration());
    }

    JsInfo jsInfo = JsInteropUtils.getJsInfo(variableBinding);
    boolean isCompileTimeConstant = variableBinding.getConstantValue() != null;
    return FieldDescriptor.newBuilder()
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .setName(fieldName)
        .setTypeDescriptor(thisTypeDescriptor)
        .setStatic(isStatic)
        .setVisibility(visibility)
        .setJsInfo(jsInfo)
        .setCompileTimeConstant(isCompileTimeConstant)
        .setDeclarationFieldDescriptor(declarationFieldDescriptor)
        .build();
  }

  static Variable createVariable(IVariableBinding variableBinding) {
    String name = variableBinding.getName();
    TypeDescriptor typeDescriptor =
        variableBinding.isParameter()
            ? createTypeDescriptorWithNullability(
                variableBinding.getType(), variableBinding.getAnnotations())
            : createTypeDescriptor(variableBinding.getType());
    boolean isFinal = isFinal(variableBinding);
    boolean isParameter = variableBinding.isParameter();
    return Variable.newBuilder()
        .setName(name)
        .setTypeDescriptor(typeDescriptor)
        .setIsFinal(isFinal)
        .setIsParameter(isParameter)
        .build();
  }

  public static BinaryOperator getBinaryOperator(InfixExpression.Operator operator) {
    switch (operator.toString()) {
      case "*":
        return BinaryOperator.TIMES;
      case "/":
        return BinaryOperator.DIVIDE;
      case "%":
        return BinaryOperator.REMAINDER;
      case "+":
        return BinaryOperator.PLUS;
      case "-":
        return BinaryOperator.MINUS;
      case "<<":
        return BinaryOperator.LEFT_SHIFT;
      case ">>":
        return BinaryOperator.RIGHT_SHIFT_SIGNED;
      case ">>>":
        return BinaryOperator.RIGHT_SHIFT_UNSIGNED;
      case "<":
        return BinaryOperator.LESS;
      case ">":
        return BinaryOperator.GREATER;
      case "<=":
        return BinaryOperator.LESS_EQUALS;
      case ">=":
        return BinaryOperator.GREATER_EQUALS;
      case "==":
        return BinaryOperator.EQUALS;
      case "!=":
        return BinaryOperator.NOT_EQUALS;
      case "^":
        return BinaryOperator.BIT_XOR;
      case "&":
        return BinaryOperator.BIT_AND;
      case "|":
        return BinaryOperator.BIT_OR;
      case "&&":
        return BinaryOperator.CONDITIONAL_AND;
      case "||":
        return BinaryOperator.CONDITIONAL_OR;
    }
    return null;
  }

  public static BinaryOperator getBinaryOperator(Assignment.Operator operator) {
    switch (operator.toString()) {
      case "=":
        return BinaryOperator.ASSIGN;
      case "+=":
        return BinaryOperator.PLUS_ASSIGN;
      case "-=":
        return BinaryOperator.MINUS_ASSIGN;
      case "*=":
        return BinaryOperator.TIMES_ASSIGN;
      case "/=":
        return BinaryOperator.DIVIDE_ASSIGN;
      case "&=":
        return BinaryOperator.BIT_AND_ASSIGN;
      case "|=":
        return BinaryOperator.BIT_OR_ASSIGN;
      case "^=":
        return BinaryOperator.BIT_XOR_ASSIGN;
      case "%=":
        return BinaryOperator.REMAINDER_ASSIGN;
      case "<<=":
        return BinaryOperator.LEFT_SHIFT_ASSIGN;
      case ">>=":
        return BinaryOperator.RIGHT_SHIFT_SIGNED_ASSIGN;
      case ">>>=":
        return BinaryOperator.RIGHT_SHIFT_UNSIGNED_ASSIGN;
    }
    return null;
  }

  static IMethodBinding getMethodBinding(
      ITypeBinding typeBinding, String methodName, ITypeBinding... parameterTypes) {

    Queue<ITypeBinding> deque = new LinkedList<>();
    deque.add(typeBinding);

    while (!deque.isEmpty()) {
      typeBinding = deque.poll();
      for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
        if (methodBinding.getName().equals(methodName)
            && Arrays.equals(methodBinding.getParameterTypes(), parameterTypes)) {
          return methodBinding;
        }
      }
      ITypeBinding superclass = typeBinding.getSuperclass();
      if (superclass != null) {
        deque.add(superclass);
      }

      ITypeBinding[] superInterfaces = typeBinding.getInterfaces();
      if (superInterfaces != null) {
        for (ITypeBinding superInterface : superInterfaces) {
          deque.add(superInterface);
        }
      }
    }
    return null;
  }

  /**
   * Returns the methods in {@code typeBinding}'s interfaces that are accidentally overridden.
   *
   * <p>'Accidentally overridden' means the type itself does not have its own declared overriding
   * method, and the method it inherits does not really override, but just has the same signature
   * of the overridden method.
   */
  static List<IMethodBinding> getAccidentalOverriddenMethodBindings(ITypeBinding typeBinding) {
    List<IMethodBinding> accidentalOverriddenMethods = new ArrayList<>();
    for (ITypeBinding superInterface :
        Sets.difference(
            getAllInterfaces(typeBinding), getAllInterfaces(typeBinding.getSuperclass()))) {
      accidentalOverriddenMethods.addAll(getUndeclaredMethodBindings(superInterface, typeBinding));
    }
    return accidentalOverriddenMethods;
  }

  /**
   * Returns the methods that are declared by {@code superTypeBinding} but are not declared by
   * {@code typeBinding}.
   */
  private static List<IMethodBinding> getUndeclaredMethodBindings(
      ITypeBinding superTypeBinding, final ITypeBinding typeBinding) {
    return filterMethodBindings(
        superTypeBinding.getDeclaredMethods(),
        methodBinding -> !isDeclaredBy(methodBinding, typeBinding));
  }

  /**
   * Returns true if {@code typeBinding} declares a method with the same signature of
   * {@code methodBinding} in its body.
   */
  private static boolean isDeclaredBy(IMethodBinding methodBinding, ITypeBinding typeBinding) {
    return hasOverrideEquivalentMethod(
        methodBinding, Arrays.asList(typeBinding.getDeclaredMethods()));
  }

  /**
   * Returns true if the given list of method bindings contains a method with the same signature of
   * {@code methodBinding}.
   */
  public static boolean hasOverrideEquivalentMethod(
      IMethodBinding method, List<IMethodBinding> methodBindings) {
    for (IMethodBinding methodBinding : methodBindings) {
      if (areMethodsOverrideEquivalent(method, methodBinding)) {
        return true;
      }
    }
    return false;
  }

  private static List<IMethodBinding> filterMethodBindings(
      IMethodBinding[] methodBindings, Predicate<IMethodBinding> predicate) {
    return Stream.of(methodBindings).filter(predicate).collect(toImmutableList());
  }

  /**
   * Returns all the interfaces {@code typeBinding} implements.
   */
  static Set<ITypeBinding> getAllInterfaces(ITypeBinding typeBinding) {
    Set<ITypeBinding> interfaces = new LinkedHashSet<>();
    if (typeBinding == null) {
      return interfaces;
    }
    for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
      interfaces.add(superInterface);
      interfaces.addAll(getAllInterfaces(superInterface));
    }
    ITypeBinding superclassTypeBinding = typeBinding.getSuperclass();
    if (superclassTypeBinding != null) {
      interfaces.addAll(getAllInterfaces(superclassTypeBinding));
    }
    return interfaces;
  }

  /**
   * Returns the nearest method in the super classes of {code typeBinding} that overrides
   * (regularly or accidentally) {@code methodBinding}.
   */
  static IMethodBinding getOverridingMethodInSuperclasses(
      IMethodBinding methodBinding, ITypeBinding typeBinding) {
    ITypeBinding superclass = typeBinding.getSuperclass();
    while (superclass != null) {
      for (IMethodBinding methodInSuperclass : superclass.getDeclaredMethods()) {
        // TODO: excludes package private method, and add a test for it.
        if (areMethodsOverrideEquivalent(methodInSuperclass, methodBinding)) {
          return methodInSuperclass;
        }
      }
      superclass = superclass.getSuperclass();
    }
    return null;
  }

  public static PrefixOperator getPrefixOperator(PrefixExpression.Operator operator) {
    switch (operator.toString()) {
      case "++":
        return PrefixOperator.INCREMENT;
      case "--":
        return PrefixOperator.DECREMENT;
      case "+":
        return PrefixOperator.PLUS;
      case "-":
        return PrefixOperator.MINUS;
      case "~":
        return PrefixOperator.COMPLEMENT;
      case "!":
        return PrefixOperator.NOT;
    }
    return null;
  }

  public static PostfixOperator getPostfixOperator(PostfixExpression.Operator operator) {
    switch (operator.toString()) {
      case "++":
        return PostfixOperator.INCREMENT;
      case "--":
        return PostfixOperator.DECREMENT;
    }
    return null;
  }

  static boolean isStatic(BodyDeclaration bodyDeclaration) {
    return Modifier.isStatic(bodyDeclaration.getModifiers());
  }

  static boolean isArrayLengthBinding(IVariableBinding variableBinding) {
    return variableBinding.getName().equals("length")
        && variableBinding.isField()
        && variableBinding.getDeclaringClass() == null;
  }

  static boolean isJsOverride(IMethodBinding methodBinding) {
    // If the JsMethod is the first in the override chain, it does not override any methods.
    return isOverride(methodBinding) && !isFirstJsMember(methodBinding);
  }

  /**
   * Returns true if the method is the first JsMember in the override chain (does not override any
   * other JsMembers).
   */
  static boolean isFirstJsMember(IMethodBinding methodBinding) {
    return JsInteropUtils.isJsMember(methodBinding)
        && getOverriddenJsMembers(methodBinding).isEmpty();
  }

  static boolean isOverride(IMethodBinding overridingMethod) {
    ITypeBinding type = overridingMethod.getDeclaringClass();

    // Check immediate super class and interfaces for overridden method.
    if (type.getSuperclass() != null && isOveriddenInType(overridingMethod, type.getSuperclass())) {
      return true;
    }
    for (ITypeBinding interfaceBinding : type.getInterfaces()) {
      if (isOveriddenInType(overridingMethod, interfaceBinding)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isOveriddenInType(IMethodBinding overridingMethod, ITypeBinding type) {
    for (IMethodBinding method : type.getDeclaredMethods()) {
      // exposed overriding is not real overriding in JavaScript because the two methods
      // have different method names and they are connected by dispatch method,
      if (overridingMethod.overrides(method.getMethodDeclaration())
          && (!upgradesPackagePrivateVisibility(overridingMethod, method.getMethodDeclaration()))
          && areMethodsOverrideEquivalent(overridingMethod, method.getMethodDeclaration())) {
        return true;
      }
    }

    // Recurse into immediate super class and interfaces for overridden method.
    if (type.getSuperclass() != null && isOveriddenInType(overridingMethod, type.getSuperclass())) {
      return true;
    }
    for (ITypeBinding interfaceBinding : type.getInterfaces()) {
      if (isOveriddenInType(overridingMethod, interfaceBinding)) {
        return true;
      }
    }

    return false;
  }

  static boolean upgradesPackagePrivateVisibility(
      IMethodBinding overridingMethod, IMethodBinding overriddenMethod) {
    checkArgument(overridingMethod.overrides(overriddenMethod));
    Visibility overriddenMethodVisibility = getVisibility(overriddenMethod);
    Visibility overridingMethodVisibility = getVisibility(overridingMethod);
    return overriddenMethodVisibility.isPackagePrivate()
        && (overridingMethodVisibility.isPublic() || overridingMethodVisibility.isProtected());
  }

  /** Returns true if the binding is annotated with @UncheckedCast. */
  static boolean hasUncheckedCastAnnotation(IBinding binding) {
    return JdtAnnotationUtils.hasAnnotation(binding, "javaemul.internal.annotations.UncheckedCast");
  }

  /**
   * Two methods are parameter erasure equal if the erasure of their parameters' types are equal.
   * Parameter erasure equal means that they are overriding signature equal, which means that they
   * are real overriding/overridden or accidental overriding/overridden.
   */
  static boolean areMethodsOverrideEquivalent(
      IMethodBinding leftMethod, IMethodBinding rightMethod) {
    return getMethodSignature(leftMethod).equals(getMethodSignature(rightMethod));
  }

  static IMethodBinding findSamMethodBinding(ITypeBinding typeBinding) {
    // TODO: there maybe an issue in which case it inherits a default method from an interface
    // and inherits an abstract method with the same signature from another interface. Add an
    // example to address the potential issue.
    checkArgument(typeBinding.isInterface());
    for (IMethodBinding method : typeBinding.getDeclaredMethods()) {
      if (isAbstract(method)) {
        return method;
      }
    }
    for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
      IMethodBinding samMethodFromSuperInterface = findSamMethodBinding(superInterface);
      if (samMethodFromSuperInterface != null) {
        return samMethodFromSuperInterface;
      }
    }
    return null;
  }

  static Method createSamMethod(
      TypeDescriptor lambdaTypeDescriptor,
      ITypeBinding lambdaInterfaceBinding,
      MethodDescriptor lambdaMethodDescriptor) {

    MethodDescriptor samMethodDescriptor =
        createMethodDescriptor(checkNotNull(findSamMethodBinding(lambdaInterfaceBinding)));
    MethodDescriptor samImplementationMethodDescriptor =
        MethodDescriptor.Builder.from(samMethodDescriptor)
            .setEnclosingClassTypeDescriptor(lambdaTypeDescriptor)
            .setNative(false)
            .build();
    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();
    List<TypeDescriptor> parameterTypes = lambdaMethodDescriptor.getParameterTypeDescriptors();
    for (int i = 0; i < parameterTypes.size(); i++) {
      Variable parameter =
          Variable.newBuilder()
              .setName("arg" + i)
              .setTypeDescriptor(parameterTypes.get(i))
              .setIsParameter(true)
              .build();
      parameters.add(parameter);
      arguments.add(parameter.getReference());
    }
    Expression callLambda =
        MethodCall.Builder.from(lambdaMethodDescriptor).setArguments(arguments).build();
    Statement statement =
        TypeDescriptors.isPrimitiveVoid(lambdaMethodDescriptor.getReturnTypeDescriptor())
            ? callLambda.makeStatement()
            : new ReturnStatement(
                callLambda, samImplementationMethodDescriptor.getReturnTypeDescriptor());

    return Method.newBuilder()
        .setMethodDescriptor(samImplementationMethodDescriptor)
        .setParameters(parameters)
        .addStatements(statement)
        .setIsOverride(true)
        .setIsFinal(true)
        .build();
  }

  /**
   * Returns whether {@code subTypeBinding} is a subtype of {@code superTypeBinding} either because
   * subTypeBinding is a child class of class superTypeBinding or because subTypeBinding implements
   * interface superTypeBinding. As 'subtype' is transitive and reflective, a type is subtype of
   * itself.
   */
  static boolean isSubType(ITypeBinding subTypeBinding, ITypeBinding superTypeBinding) {
    if (areSameErasedType(superTypeBinding, subTypeBinding)) {
      return true;
    }
    // Check if it's a child class.
    ITypeBinding superClass = subTypeBinding.getSuperclass();
    if (superClass != null && isSubType(superClass, superTypeBinding)) {
      return true;
    }
    // Check if it implements the interface.
    for (ITypeBinding superInterface : subTypeBinding.getInterfaces()) {
      if (isSubType(superInterface, superTypeBinding)) {
        return true;
      }
    }
    return false;
  }

  static boolean areSameErasedType(ITypeBinding typeBinding, ITypeBinding otherTypeBinding) {
    return typeBinding.getErasure().isEqualTo(otherTypeBinding.getErasure());
  }

  /**
   * Helper method to work around JDT habit of returning raw collections.
   */
  @SuppressWarnings("rawtypes")
  public static <T> List<T> asTypedList(List jdtRawCollection) {
    @SuppressWarnings("unchecked")
    List<T> typedList = jdtRawCollection;
    return typedList;
  }

  /**
   * Returns the method binding of the immediately enclosing method, whether that be an actual
   * method or a lambda expression.
   */
  public static IMethodBinding findCurrentMethodBinding(org.eclipse.jdt.core.dom.ASTNode node) {
    while (true) {
      if (node == null) {
        return null;
      } else if (node instanceof MethodDeclaration) {
        return ((MethodDeclaration) node).resolveBinding();
      } else if (node instanceof LambdaExpression) {
        return ((LambdaExpression) node).resolveMethodBinding();
      }
      node = node.getParent();
    }
  }

  /**
   * Returns the type binding of the immediately enclosing type.
   */
  public static ITypeBinding findCurrentTypeBinding(org.eclipse.jdt.core.dom.ASTNode node) {
    while (true) {
      if (node == null) {
        return null;
      } else if (node instanceof AbstractTypeDeclaration) {
        return ((AbstractTypeDeclaration) node).resolveBinding();
      } else if (node instanceof AnonymousClassDeclaration) {
        return ((AnonymousClassDeclaration) node).resolveBinding();
      }
      node = node.getParent();
    }
  }

  /**
   * Returns whether the ASTNode is in a static context.
   */
  public static boolean isInStaticContext(org.eclipse.jdt.core.dom.ASTNode node) {
    org.eclipse.jdt.core.dom.ASTNode currentNode = node.getParent();
    while (currentNode != null) {
      switch (currentNode.getNodeType()) {
        case ASTNode.FIELD_DECLARATION:
          if (findCurrentTypeBinding(currentNode).isInterface()) {
            // Field declarations in interface are implicitly static.
            return true;
          }
        case ASTNode.METHOD_DECLARATION:
        case ASTNode.INITIALIZER:
          return isStatic((BodyDeclaration) currentNode);
        case ASTNode.ENUM_CONSTANT_DECLARATION: // enum constants are implicitly static.
          return true;
      }
      currentNode = currentNode.getParent();
    }
    return false;
  }

  /** Creates a TypeDescriptor from a JDT TypeBinding. */
  public static TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding) {
    return createTypeDescriptorWithNullability(typeBinding, new IAnnotationBinding[0]);
  }

  /**
   * Creates a type descriptor for the given type binding, taking into account nullability.
   *
   * @param typeBinding the type binding, used to create the type descriptor.
   * @param elementAnnotations the annotations on the element
   */
  public static TypeDescriptor createTypeDescriptorWithNullability(
      ITypeBinding typeBinding, IAnnotationBinding[] elementAnnotations) {
    if (typeBinding == null) {
      return null;
    }
    TypeDescriptor descriptor = internalCreateTypeDescriptor(typeBinding);
    return TypeDescriptors.toGivenNullability(
        descriptor, isNullable(typeBinding, elementAnnotations));
  }

  private static TypeDescriptor internalCreateTypeDescriptor(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }

    if (isIntersectionType(typeBinding)) {
      return createIntersectionType(typeBinding);
    }

    if (typeBinding.isArray()) {
      TypeDescriptor leafTypeDescriptor = createTypeDescriptor(typeBinding.getElementType());
      return TypeDescriptors.getForArray(leafTypeDescriptor, typeBinding.getDimensions());
    }

    return createForType(typeBinding);
  }

  /**
   * Returns whether the given type binding should be nullable, according to the annotations on it
   * and if nullability is enabled for the package containing the binding.
   */
  private static boolean isNullable(
      ITypeBinding typeBinding, IAnnotationBinding[] elementAnnotations) {
    if (typeBinding.isTypeVariable()) {
      return true;
    }
    if (typeBinding.isPrimitive()) {
      return false;
    }
    if (typeBinding.getQualifiedName().equals("java.lang.Void")) {
      // Void is always nullable.
      return true;
    }
    Iterable<IAnnotationBinding> allAnnotations =
        Iterables.concat(
            Arrays.asList(elementAnnotations),
            Arrays.asList(typeBinding.getTypeAnnotations()),
            Arrays.asList(typeBinding.getAnnotations()));
    for (IAnnotationBinding annotation : allAnnotations) {
      String annotationName = annotation.getName();
      // TODO(simionato): Replace those annotations with J2CL-specific annotations
      if (annotationName.equals("Nullable") || annotationName.equals("NullableType")) {
        return true;
      }
      if (annotationName.equalsIgnoreCase("Nonnull")) {
        return false;
      }
    }

    return true;
  }

  /**
   * Incase the given type binding is nested, return the outermost possible enclosing type binding.
   */
  public static ITypeBinding toTopLevelTypeBinding(ITypeBinding typeBinding) {
    ITypeBinding topLevelClass = typeBinding;
    while (topLevelClass.getDeclaringClass() != null) {
      topLevelClass = topLevelClass.getDeclaringClass();
    }
    return topLevelClass;
  }

  private static boolean isIntersectionType(ITypeBinding binding) {
    // TODO(rluble): Use isIntersectionType once JDT is upgraded to RELEASE_4_7 which correctly
    // exposes intersection types through ITypeBinding.
    boolean isIntersectionType =
        !binding.isPrimitive()
            && !binding.isCapture()
            && !binding.isArray()
            && !binding.isTypeVariable()
            && !binding.isWildcardType()
            && binding.getPackage() == null;
    if (isIntersectionType) {
      checkArgument(
          (binding.getSuperclass() != null && binding.getInterfaces().length >= 1)
              || (binding.getSuperclass() == null && binding.getInterfaces().length >= 2));
    }
    return isIntersectionType;
  }

  public static List<String> getClassComponents(ITypeBinding typeBinding) {
    List<String> classComponents = new LinkedList<>();
    if (typeBinding.isWildcardType() || typeBinding.isCapture()) {
      return Collections.singletonList("?");
    }
    ITypeBinding currentType = typeBinding;
    while (currentType != null) {
      String simpleName;
      if (currentType.isLocal()) {
        // JDT binary name for local class is like package.components.EnclosingClass$1SimpleName
        // Extract the generated name by taking the part after the binary name of the declaring
        // class.
        String binaryName = getBinaryNameFromTypeBinding(currentType);
        String declaringClassPrefix =
            getBinaryNameFromTypeBinding(currentType.getDeclaringClass()) + "$";
        checkState(binaryName.startsWith(declaringClassPrefix));
        simpleName = binaryName.substring(declaringClassPrefix.length());
      } else if (currentType.isTypeVariable()) {
        if (currentType.getDeclaringClass() != null) {
          // If it is a class-level type variable, use the simple name (with prefix "C_") as the
          // current name component.
          simpleName = AstUtils.TYPE_VARIABLE_IN_TYPE_PREFIX + currentType.getName();
        } else {
          // If it is a method-level type variable, use the simple name (with prefix "M_") as the
          // current name component, and add declaringClass_declaringMethod as the next name
          // component, and set currentType to declaringClass for the next iteration.
          classComponents.add(0, AstUtils.TYPE_VARIABLE_IN_METHOD_PREFIX + currentType.getName());
          simpleName =
              currentType.getDeclaringMethod().getDeclaringClass().getName()
                  + "_"
                  + currentType.getDeclaringMethod().getName();
          currentType = currentType.getDeclaringMethod().getDeclaringClass();
        }
      } else {
        simpleName = currentType.getErasure().getName();
      }
      classComponents.add(0, simpleName);
      currentType = currentType.getDeclaringClass();
    }
    return classComponents;
  }

  /**
   * Returns the binary name for a type binding.
   *
   * <p>NOTE: This accounts for the cases that JDT does not assign binary names, which are those of
   * unreachable local or anonymous classes.
   */
  private static String getBinaryNameFromTypeBinding(ITypeBinding typeBinding) {
    String binaryName = typeBinding.getBinaryName();
    if (binaryName == null && (typeBinding.isLocal() || typeBinding.isAnonymous())) {
      // Local and anonymous classes in unreachable code have null binary name.

      // The code here is a HACK that relies on the way that JDT synthesizes keys. Keys for
      // unreachable classes have the closest enclosing reachable class key as a prefix (minus the
      // ending semicolon)
      ITypeBinding closestReachableExclosingClass = typeBinding.getDeclaringClass();
      while (closestReachableExclosingClass.getBinaryName() == null) {
        closestReachableExclosingClass = closestReachableExclosingClass.getDeclaringClass();
      }
      String parentKey = closestReachableExclosingClass.getKey();
      String key = typeBinding.getKey();
      return getBinaryNameFromTypeBinding(typeBinding.getDeclaringClass())
          + "$$Unreachable"
          // remove the parent prefix and the ending semicolon
          + key.substring(parentKey.length() - 1, key.length() - 1);
    }
    return binaryName;
  }

  public static List<TypeDescriptor> getTypeArgumentTypeDescriptors(ITypeBinding typeBinding) {
    List<TypeDescriptor> typeArgumentDescriptors = new ArrayList<>();
    if (typeBinding.isParameterizedType()) {
      typeArgumentDescriptors.addAll(createTypeDescriptors(typeBinding.getTypeArguments()));
    } else {
      typeArgumentDescriptors.addAll(createTypeDescriptors(typeBinding.getTypeParameters()));
    }

    // Find type parameters in the enclosing scope and copy them over as well.
    boolean isInstanceNestedClass =
        typeBinding.isNested() && !Modifier.isStatic(typeBinding.getModifiers());
    if (isInstanceNestedClass) {
      // Occasionally JDT's type bindings are specialized in a way that accidentally loses track of
      // it's type's method declaration scope. If so then backtrack the specialization to restore
      // access.
      if (typeBinding.getDeclaringMethod() == null
          && typeBinding.getTypeDeclaration().getDeclaringMethod() != null) {
        typeBinding = typeBinding.getTypeDeclaration();
      }

      if (typeBinding.getDeclaringMethod() != null) {
        typeArgumentDescriptors.addAll(
            createTypeDescriptors(typeBinding.getDeclaringMethod().getTypeParameters()));
      }
      if (typeBinding.getDeclaringMember() == null
          || !Modifier.isStatic(typeBinding.getDeclaringMember().getModifiers())) {
        typeArgumentDescriptors.addAll(
            createTypeDescriptor(typeBinding.getDeclaringClass()).getTypeArgumentDescriptors());
      }
    }

    return typeArgumentDescriptors;
  }

  public static Visibility getVisibility(IBinding binding) {
    return getVisibility(binding.getModifiers());
  }

  public static Visibility getVisibility(int modifiers) {
    if (Modifier.isPublic(modifiers)) {
      return Visibility.PUBLIC;
    } else if (Modifier.isProtected(modifiers)) {
      return Visibility.PROTECTED;
    } else if (Modifier.isPrivate(modifiers)) {
      return Visibility.PRIVATE;
    } else {
      return Visibility.PACKAGE_PRIVATE;
    }
  }

  public static boolean isDefaultMethod(IMethodBinding binding) {
    return Modifier.isDefault(binding.getModifiers());
  }

  public static boolean isAbstract(IBinding binding) {
    return Modifier.isAbstract(binding.getModifiers());
  }

  public static boolean isFinal(IBinding binding) {
    return Modifier.isFinal(binding.getModifiers());
  }

  public static boolean isStatic(IBinding binding) {
    if (binding instanceof IVariableBinding) {
      IVariableBinding variableBinding = (IVariableBinding) binding;
      if (!variableBinding.isField() || variableBinding.getDeclaringClass().isInterface()) {
        // Interface fields and variables are implicitly static.
        return true;
      }
    }
    return Modifier.isStatic(binding.getModifiers());
  }

  /**
   * Returns true if instances of this type capture its outer instances; i.e. if it is an non static
   * member class, or an anonymous or local class defined in an instance context.
   */
  public static boolean capturesEnclosingInstance(ITypeBinding typeBinding) {
    if (!typeBinding.isClass() || !typeBinding.isNested()) {
      // Only non-top level classes (excludes Enums, Interfaces etc.) can capture outer instances.
      return false;
    }

    if (typeBinding.isLocal()) {
      // Local types (which include anonymous classes in JDT) are static only if they are declared
      // in a static context; i.e. if the member where they are declared is static.
      return !isStatic(typeBinding.getTypeDeclaration().getDeclaringMember());
    } else {
      checkArgument(typeBinding.isMember());
      // Member classes must be marked explicitly static.
      return !isStatic(typeBinding);
    }
  }

  /**
   * Creates a MethodDescriptor directly based on the given JDT method binding.
   */
  public static MethodDescriptor createMethodDescriptor(IMethodBinding methodBinding) {
    int modifiers = methodBinding.getModifiers();
    boolean isStatic = Modifier.isStatic(modifiers);
    Visibility visibility = getVisibility(methodBinding);
    boolean isNative = Modifier.isNative(modifiers);
    TypeDescriptor enclosingClassTypeDescriptor =
        createTypeDescriptor(methodBinding.getDeclaringClass());
    boolean isConstructor = methodBinding.isConstructor();
    String methodName = methodBinding.getName();

    JsInfo jsInfo = computeJsInfo(methodBinding);

    TypeDescriptor returnTypeDescriptor =
        createTypeDescriptorWithNullability(
            methodBinding.getReturnType(), methodBinding.getAnnotations());

    // generate parameters type descriptors.
    List<TypeDescriptor> parameterTypeDescriptors = new ArrayList<>();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      TypeDescriptor descriptor =
          createTypeDescriptorWithNullability(
              methodBinding.getParameterTypes()[i], methodBinding.getParameterAnnotations(i));
      parameterTypeDescriptors.add(descriptor);
    }

    MethodDescriptor declarationMethodDescriptor = null;
    if (methodBinding.getMethodDeclaration() != methodBinding) {
      declarationMethodDescriptor = createMethodDescriptor(methodBinding.getMethodDeclaration());
    }

    // generate type parameters declared in the method.
    Iterable<TypeDescriptor> typeParameterTypeDescriptors =
        FluentIterable.from(methodBinding.getTypeParameters())
            .transform(JdtUtils::createTypeDescriptor);

    return MethodDescriptor.newBuilder()
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .setName(isConstructor ? null : methodName)
        .setDeclarationMethodDescriptor(declarationMethodDescriptor)
        .setReturnTypeDescriptor(returnTypeDescriptor)
        .setParameterTypeDescriptors(parameterTypeDescriptors)
        .setTypeParameterTypeDescriptors(typeParameterTypeDescriptors)
        .setJsInfo(jsInfo)
        .setVisibility(visibility)
        .setStatic(isStatic)
        .setConstructor(isConstructor)
        .setNative(isNative)
        .setDefault(Modifier.isDefault(methodBinding.getModifiers()))
        .setVarargs(methodBinding.isVarargs())
        .setAbstract(Modifier.isAbstract(methodBinding.getModifiers()))
        .build();
  }

  public static boolean isOrOverridesJsMember(IMethodBinding methodBinding) {
    return JsInteropUtils.isJsMember(methodBinding)
        || !getOverriddenJsMembers(methodBinding).isEmpty();
  }

  /**
   * Checks overriding chain to compute JsInfo.
   */
  static JsInfo computeJsInfo(IMethodBinding methodBinding) {
    JsInfo jsInfo = JsInteropUtils.getJsInfo(methodBinding);
    if (jsInfo.isJsOverlay()) {
      return jsInfo;
    }

    List<JsInfo> jsInfoList = new ArrayList<>();

    // Add the JsInfo of the method and all the overridden methods to the list.
    if (jsInfo.getJsMemberType() != JsMemberType.NONE) {
      jsInfoList.add(jsInfo);
    }
    for (IMethodBinding overriddenMethod : getOverriddenMethods(methodBinding)) {
      JsInfo inheritedJsInfo = JsInteropUtils.getJsInfo(overriddenMethod);
      if (inheritedJsInfo.getJsMemberType() != JsMemberType.NONE) {
        jsInfoList.add(inheritedJsInfo);
      }
    }

    if (jsInfoList.isEmpty()) {
      return JsInfo.NONE;
    }

    // TODO: Do the same for JsProperty?
    if (jsInfoList.get(0).getJsMemberType() == JsMemberType.METHOD) {
      // Return the first JsInfo with a Js name specified.
      for (JsInfo jsInfoElement : jsInfoList) {
        if (jsInfoElement.getJsName() != null) {
          return jsInfoElement;
        }
      }
    }
    return jsInfoList.get(0);
  }

  public static Set<IMethodBinding> getOverriddenJsMembers(IMethodBinding methodBinding) {
    return Sets.filter(getOverriddenMethods(methodBinding), JsInteropUtils::isJsMember);
  }

  /**
   * Returns the method signature, which identifies a method up to overriding.
   */
  public static String getMethodSignature(IMethodBinding methodBinding) {
    StringBuilder signatureBuilder = new StringBuilder("");
    Visibility methodVisibility = getVisibility(methodBinding);
    if (methodVisibility.isPackagePrivate()) {
      signatureBuilder.append(":pp:");
      signatureBuilder.append(methodBinding.getDeclaringClass().getPackage());
      signatureBuilder.append(":");
    } else if (methodVisibility.isPrivate()) {
      signatureBuilder.append(":p:");
      signatureBuilder.append(getBinaryNameFromTypeBinding(methodBinding.getDeclaringClass()));
      signatureBuilder.append(":");
    }

    signatureBuilder.append(methodBinding.getName());
    signatureBuilder.append("(");

    String separator = "";
    for (ITypeBinding parameterType : methodBinding.getParameterTypes()) {
      signatureBuilder.append(separator);
      signatureBuilder.append(getBinaryNameFromTypeBinding(parameterType.getErasure()));
      separator = ";";
    }
    signatureBuilder.append(")");
    return signatureBuilder.toString();
  }

  public static Set<IMethodBinding> getOverriddenMethods(IMethodBinding methodBinding) {
    return getOverriddenMethodsInType(methodBinding, methodBinding.getDeclaringClass());
  }

  public static Set<IMethodBinding> getOverriddenMethodsInType(
      IMethodBinding methodBinding, ITypeBinding typeBinding) {
    Set<IMethodBinding> overriddenMethods = new HashSet<>();
    for (IMethodBinding declaredMethod : typeBinding.getDeclaredMethods()) {
      if (methodBinding.overrides(declaredMethod) && !methodBinding.isConstructor()) {
        checkArgument(!Modifier.isStatic(methodBinding.getModifiers()));
        overriddenMethods.add(declaredMethod);
      }
    }
    // Recurse into immediate super class and interfaces for overridden method.
    if (typeBinding.getSuperclass() != null) {
      overriddenMethods.addAll(
          getOverriddenMethodsInType(methodBinding, typeBinding.getSuperclass()));
    }
    for (ITypeBinding interfaceBinding : typeBinding.getInterfaces()) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, interfaceBinding));
    }
    return overriddenMethods;
  }

  public static boolean isLocal(ITypeBinding typeBinding) {
    return typeBinding.isLocal();
  }

  /**
   * Returns true if {@code typeBinding} is a class that implements a JsFunction interface.
   */
  public static boolean isJsFunctionImplementation(ITypeBinding typeBinding) {
    if (typeBinding == null || !typeBinding.isClass()) {
      return false;
    }
    for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
      if (JsInteropUtils.isJsFunction(superInterface)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the given type has a JsConstructor.
   */
  public static boolean isJsConstructorClass(ITypeBinding typeBinding) {
    if (typeBinding == null || !typeBinding.isClass()) {
      return false;
    }
    Collection<IMethodBinding> constructors =
        Collections2.filter(
            Arrays.asList(typeBinding.getDeclaredMethods()), IMethodBinding::isConstructor);
    if (constructors.isEmpty()
        && Modifier.isPublic(typeBinding.getModifiers())
        && !typeBinding.isEnum()) {
      // A public JsType with default constructor is a JsConstructor class.
      return JsInteropUtils.isJsType(typeBinding);
    }
    return constructors
        .stream()
        .anyMatch(
            constructor ->
                JsInteropUtils.getJsInfo(constructor).getJsMemberType()
                    == JsMemberType.CONSTRUCTOR);
  }

  /**
   * Returns true if the given type has a JsConstructor, or it is a successor of a class that has a
   * JsConstructor.
   */
  public static boolean isOrSubclassesJsConstructorClass(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return false;
    }
    return isJsConstructorClass(typeBinding)
        || isOrSubclassesJsConstructorClass(typeBinding.getSuperclass());
  }

  /**
   * Returns the MethodDescriptor for the SAM method in JsFunction interface.
   */
  public static MethodDescriptor getJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
    IMethodBinding samInJsFunctionInterface = getSAMInJsFunctionInterface(typeBinding);
    return samInJsFunctionInterface == null
        ? null
        : createMethodDescriptor(samInJsFunctionInterface.getMethodDeclaration());
  }

  /**
   * Returns the MethodDescriptor for the concrete JsFunction method implementation.
   */
  public static MethodDescriptor getConcreteJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
    IMethodBinding samInJsFunctionInterface = getSAMInJsFunctionInterface(typeBinding);
    if (samInJsFunctionInterface == null) {
      return null;
    }
    if (typeBinding.isInterface()) {
      return createMethodDescriptor(samInJsFunctionInterface);
    }
    for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
      if (methodBinding.isSynthetic()) {
        // skip the synthetic method.
        continue;
      }
      if (methodBinding.overrides(samInJsFunctionInterface)) {
        return createMethodDescriptor(methodBinding);
      }
    }
    return null;
  }

  /**
   * Returns JsFunction method in JsFunction interface.
   */
  private static IMethodBinding getSAMInJsFunctionInterface(ITypeBinding typeBinding) {
    if (!isJsFunctionImplementation(typeBinding) && !JsInteropUtils.isJsFunction(typeBinding)) {
      return null;
    }
    ITypeBinding jsFunctionInterface =
        typeBinding.isInterface() ? typeBinding : typeBinding.getInterfaces()[0];

    // TODO(rluble): all the logic below should be replaced by
    //
    //   return jsFunctionInterface.getFunctionalInterfaceMethod()
    //
    // but doing so ends up with slightly different generic signatures.
    //
    List<IMethodBinding> abstractMethods =
        Arrays.stream(jsFunctionInterface.getDeclaredMethods())
            .filter(JdtUtils::isAbstract)
            .collect(Collectors.toList());

    checkArgument(
        abstractMethods.size() == 1,
        "Type %s should have one and only one abstract method.",
        jsFunctionInterface.getName());

    return abstractMethods.get(0);
  }

  static ImmutableList<TypeDescriptor> createTypeDescriptors(List<ITypeBinding> typeBindings) {
    return typeBindings.stream().map(JdtUtils::createTypeDescriptor).collect(toImmutableList());
  }

  static ImmutableList<TypeDescriptor> createTypeDescriptors(ITypeBinding[] typeBindings) {
    return createTypeDescriptors(Arrays.asList(typeBindings));
  }

  public static void initTypeDescriptors(AST ast, Iterable<ITypeBinding> typeBindings) {
    if (TypeDescriptors.isInitialized()) {
      return;
    }
    SingletonInitializer singletonInitializer =
        new SingletonInitializer()
            // Add primitive void type.
            .addPrimitiveType(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.VOID_TYPE_NAME)))
            // Add primitive boxed types.
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.BOOLEAN_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Boolean")))
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.BYTE_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Byte")))
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.CHAR_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Character")))
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.DOUBLE_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Double")))
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.FLOAT_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Float")))
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.INT_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Integer")))
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.LONG_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Long")))
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.SHORT_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Short")));
    // Add well-known, non-primitive types.
    for (ITypeBinding typeBinding : typeBindings) {
      singletonInitializer.addReferenceType(createTypeDescriptor(typeBinding));
    }
    singletonInitializer.init();
  }


  public static TypeDescriptor createLambdaTypeDescriptor(
      boolean inStaticContext,
      final TypeDescriptor enclosingClassTypeDescriptor,
      List<String> classComponents,
      final ITypeBinding lambdaTypeBinding) {
    // About Intersection typed Lambdas:
    //
    // From JLS 15.27.3: A lambda expression is compatible in an assignment context, invocation
    // context, or casting context with a target type T if T is a functional interface type (§9.8)..
    // From JLS 9.8:
    // The declaration of a functional interface allows a functional interface type to be used in a
    // program. There are four kinds of functional interface type:
    // - The type of a non-generic (§6.1) functional interface
    // - A parameterized type that is a parameterization (§4.5) of a generic functional interface
    // - The raw type (§4.8) of a generic functional interface
    // - An intersection type (§4.9) that induces a notional functional interface
    // Note:
    // In special circumstances, it is useful to treat an intersection type as a functional
    // interface type. Typically, this will look like an intersection of a functional interface
    // type with one or more marker interface types, such as Runnable & java.io.Serializable. (...)
    final List<ITypeBinding> lambdaInterfaceBindings =
        isIntersectionType(lambdaTypeBinding)
            ? Arrays.asList(lambdaTypeBinding.getInterfaces())
            : Collections.singletonList(lambdaTypeBinding);

    if (isIntersectionType(lambdaTypeBinding)) {
      checkArgument(lambdaTypeBinding.getSuperclass() == null);
      checkState(lambdaInterfaceBindings.size() >= 2);
    }

    Supplier<MethodDescriptor> concreteJsFunctionMethodDescriptorFactory =
        () ->
            lambdaInterfaceBindings
                .stream()
                .map(JdtUtils::getConcreteJsFunctionMethodDescriptor)
                .filter(Predicates.notNull())
                .findFirst()
                .orElse(null);
    Supplier<MethodDescriptor> jsFunctionMethodDescriptorFactory =
        () ->
            lambdaInterfaceBindings
                .stream()
                .map(JdtUtils::getJsFunctionMethodDescriptor)
                .filter(Predicates.notNull())
                .findFirst()
                .orElse(null);

    final ImmutableList<TypeDescriptor> lambdaInterfaceTypeDescriptors =
        createTypeDescriptors(lambdaInterfaceBindings);
    checkArgument(lambdaInterfaceTypeDescriptors.stream().allMatch(TypeDescriptor::isInterface));
    boolean isJsFunctionImplementation =
        lambdaInterfaceTypeDescriptors.stream().anyMatch(TypeDescriptor::isJsFunctionInterface);

    List<TypeDescriptor> typeArgumentDescriptors = Lists.newArrayList();
    for (TypeDescriptor interfaceTypeDescriptor : lambdaInterfaceTypeDescriptors) {
      typeArgumentDescriptors.addAll(interfaceTypeDescriptor.getAllTypeVariables());
    }
    return TypeDescriptor.newBuilder()
        .setClassComponents(classComponents)
        .setConcreteJsFunctionMethodDescriptorFactory(concreteJsFunctionMethodDescriptorFactory)
        .setEnclosingTypeDescriptorFactory(() -> enclosingClassTypeDescriptor)
        .setCapturingEnclosingInstance(!inStaticContext)
        .setJsFunctionImplementation(isJsFunctionImplementation)
        .setLocal(true)
        .setNullable(true)
        .setJsFunctionMethodDescriptorFactory(jsFunctionMethodDescriptorFactory)
        .setPackageName(enclosingClassTypeDescriptor.getPackageName())
        .setRawTypeDescriptorFactory(
            selfTypeDescriptor ->
                TypeDescriptor.Builder.from(selfTypeDescriptor)
                    .setNullable(true)
                    .setTypeArgumentDescriptors(Collections.emptyList())
                    .build())
        .setInterfaceTypeDescriptorsFactory(() -> lambdaInterfaceTypeDescriptors)
        .setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setVisibility(Visibility.PRIVATE)
        .setKind(Kind.CLASS)
        .build();
  }

  private JdtUtils() {}

  /**
   * Extracts the intersected types from a jdt type binding. These are represented as a super class
   * and 1 or more super interfaces.
   */
  private static List<ITypeBinding> getTypeBindingsForIntersectionType(ITypeBinding binding) {
    // NOTE: Per JDT documentation binding.getTypeBounds() should return the components  of
    // the intersection type but it does not.
    // TODO(rluble): revisit when JDT is upgraded to 4.7.
    checkArgument(isIntersectionType(binding));
    List<ITypeBinding> bindings = Lists.newArrayList(binding.getInterfaces());
    if (binding.getSuperclass() != null) {
      bindings.add(binding.getSuperclass());
    }
    checkArgument(bindings.size() >= 2);
    return bindings;
  }

  /**
   * Since we don't have access to the enclosing class, the proper package and naming cannot be
   * computed here. Instead we have an early normalization pass that traverses the intersection
   * types and sets the correct package and binaryName etc.
   */
  private static final TypeDescriptor createIntersectionType(ITypeBinding typeBinding) {
    checkArgument(isIntersectionType(typeBinding));
    List<ITypeBinding> intersectedTypeBindings = getTypeBindingsForIntersectionType(typeBinding);
    List<TypeDescriptor> intersectedTypeDescriptors =
        createTypeDescriptors(intersectedTypeBindings);
    return TypeDescriptors.createIntersection(intersectedTypeDescriptors);
  }

  // This is only used by TypeProxyUtils, and cannot be used elsewhere. Because to create a
  // TypeDescriptor from a TypeBinding, it should go through the path to check array type.
  private static TypeDescriptor createForType(final ITypeBinding typeBinding) {
    checkArgument(!typeBinding.isArray());

    PackageInfoCache packageInfoCache = PackageInfoCache.get();

    ITypeBinding topLevelTypeBinding = toTopLevelTypeBinding(typeBinding);
    if (topLevelTypeBinding.isFromSource()) {
      // Let the PackageInfoCache know that this class is Source, otherwise it would have to rummage
      // around in the class path to figure it out and it might even come up with the wrong answer
      // for example if this class has also been globbed into some other library that is a
      // dependency of this one.
      PackageInfoCache.get().markAsSource(getBinaryNameFromTypeBinding(topLevelTypeBinding));
    }

    Supplier<TypeDescriptor> rawTypeDescriptorFactory =
        () -> {
          TypeDescriptor rawTypeDescriptor = createTypeDescriptor(typeBinding.getErasure());
          if (rawTypeDescriptor.isParameterizedType()) {
            return TypeDescriptors.replaceTypeArgumentDescriptors(
                rawTypeDescriptor, ImmutableList.of());
          }
          return rawTypeDescriptor;
        };

    // Compute these first since they're reused in other calculations.
    String packageName =
        typeBinding.getPackage() == null ? null : typeBinding.getPackage().getName();
    boolean isTypeVariable = typeBinding.isTypeVariable();
    boolean isWildCardOrCapture = typeBinding.isWildcardType() || typeBinding.isCapture();
    boolean isAbstract = isAbstract(typeBinding);
    boolean isFinal = isFinal(typeBinding);
    boolean isNullable = !typeBinding.isPrimitive() || typeBinding.isTypeVariable();
    String uniqueKey = (isTypeVariable || isWildCardOrCapture) ? typeBinding.getKey() : null;

    Supplier<ImmutableMap<String, MethodDescriptor>> declaredMethods =
        () -> {
          ImmutableMap.Builder<String, MethodDescriptor> mapBuilder = ImmutableMap.builder();
          for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
            MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
            mapBuilder.put(methodDescriptor.getMethodSignature(), methodDescriptor);
          }
          return mapBuilder.build();
        };

    boolean hasTypeBounds =
        (isTypeVariable || isWildCardOrCapture) && typeBinding.getTypeBounds().length != 0;

    Supplier<TypeDescriptor> boundTypeDescriptorFactory =
        () -> {
          if (!hasTypeBounds) {
            return null;
          }
          ITypeBinding[] boundTypeBindings = typeBinding.getTypeBounds();
          if (boundTypeBindings.length == 1) {
            return createTypeDescriptor(boundTypeBindings[0]);
          }
          return TypeDescriptors.createIntersection(createTypeDescriptors(boundTypeBindings));
        };

    // Compute these even later
    return TypeDescriptor.newBuilder()
        .setBoundTypeDescriptorFactory(boundTypeDescriptorFactory)
        .setClassComponents(getClassComponents(typeBinding))
        .setConcreteJsFunctionMethodDescriptorFactory(
            () -> getConcreteJsFunctionMethodDescriptor(typeBinding))
        .setEnclosingTypeDescriptorFactory(
            () -> createTypeDescriptor(typeBinding.getDeclaringClass()))
        .setInterfaceTypeDescriptorsFactory(
            () -> createTypeDescriptors(typeBinding.getInterfaces()))
        .setAbstract(isAbstract)
        .setKind(getKindFromTypeBinding(typeBinding))
        .setCapturingEnclosingInstance(capturesEnclosingInstance(typeBinding))
        .setFinal(isFinal)
        .setFunctionalInterface(typeBinding.getFunctionalInterfaceMethod() != null)
        .setJsFunctionInterface(JsInteropUtils.isJsFunction(typeBinding))
        .setJsFunctionImplementation(isJsFunctionImplementation(typeBinding))
        .setJsType(JsInteropUtils.isJsType(typeBinding))
        .setNative(JsInteropUtils.isNativeType(typeBinding))
        .setLocal(isLocal(typeBinding))
        .setNullable(isNullable)
        .setJsFunctionMethodDescriptorFactory(() -> getJsFunctionMethodDescriptor(typeBinding))
        .setSimpleJsName(getJsName(typeBinding))
        .setJsNamespace(getJsNamespace(typeBinding, packageInfoCache))
        .setPackageName(packageName)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setJsConstructorClassOrSubclass(isOrSubclassesJsConstructorClass(typeBinding))
        .setSuperTypeDescriptorFactory(() -> createTypeDescriptor(typeBinding.getSuperclass()))
        .setTypeArgumentDescriptors(getTypeArgumentTypeDescriptors(typeBinding))
        .setVisibility(getVisibility(typeBinding))
        .setDeclaredMethodDescriptorsFactory(declaredMethods)
        .setUniqueKey(uniqueKey)
        .build();
  }

  private static Kind getKindFromTypeBinding(ITypeBinding typeBinding) {
    // First look whether it is a type variable, a wildcard or capture to avoid mislabeling the
    // the kind. TypeVariables that are bound to Enum respond true to ITypeBinding.isEnum().
    if (typeBinding.isTypeVariable()) {
      return Kind.TYPE_VARIABLE;
    } else if (typeBinding.isWildcardType() || typeBinding.isCapture()) {
      return Kind.WILDCARD_OR_CAPTURE;
    } else if (typeBinding.isPrimitive()) {
      return Kind.PRIMITIVE;
    } else if (typeBinding.isArray()) {
      return Kind.ARRAY;
    } else if (typeBinding.isEnum() && !typeBinding.isAnonymous()) {
      // Do not consider the anonymous classes that constitute enum values as Enums, only the
      // enum "class" itself is considered Kind.ENUM.
      return Kind.ENUM;
    } else if (typeBinding.isClass() || (typeBinding.isEnum() && typeBinding.isAnonymous())) {
      return Kind.CLASS;
    } else if (typeBinding.isInterface()) {
      return Kind.INTERFACE;
    }
    throw new RuntimeException("Type binding " + typeBinding + " not handled");
  }

  private static String getJsName(final ITypeBinding typeBinding) {
    if (typeBinding.isPrimitive()) {
      return "$" + typeBinding.getName();
    }
    return JsInteropAnnotationUtils.getJsName(
        JsInteropAnnotationUtils.getJsTypeAnnotation(typeBinding));
  }

  private static String getJsNamespace(
      ITypeBinding typeBinding, PackageInfoCache packageInfoCache) {
    if (typeBinding.isPrimitive()) {
      return "vmbootstrap.primitives";
    }
    String jsNamespace =
        JsInteropAnnotationUtils.getJsNamespace(
            JsInteropAnnotationUtils.getJsTypeAnnotation(typeBinding));
    if (jsNamespace != null) {
      return jsNamespace;
    }

    // Maybe namespace is set via package-info file?
    boolean isTopLevelType = typeBinding.getDeclaringClass() == null;
    if (isTopLevelType) {
      return packageInfoCache.getJsNamespace(
          getBinaryNameFromTypeBinding(toTopLevelTypeBinding(typeBinding)));
    }
    return null;
  }
}
