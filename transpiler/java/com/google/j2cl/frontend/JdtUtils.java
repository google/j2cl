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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JavaType.Kind;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsInteropUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeProxyUtils;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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

  /**
   * Creates and returns a JDT name environment for finding class files by name.
   * <p>
   * Sadly this work is redundant with work already done by the JDT parser, but it's not possible to
   * grab the parser's internal name environment instance.
   */
  static INameEnvironment createNameEnvironment(FrontendOptions options) {
    INameEnvironment nameEnvironment =
        new FileSystem(
            Iterables.toArray(options.getClasspathEntries(), String.class),
            null,
            options.getEncoding());
    return nameEnvironment;
  }

  static TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding) {
    return TypeProxyUtils.createTypeDescriptor(typeBinding);
  }

  static Iterable<TypeDescriptor> createTypeDescriptors(ITypeBinding[] typeBindings) {
    return FluentIterable.from(Arrays.asList(typeBindings))
        .transform(
            new Function<ITypeBinding, TypeDescriptor>() {
              @Override
              public TypeDescriptor apply(ITypeBinding typeBinding) {
                return createTypeDescriptor(typeBinding);
              }
            });
  }

  static FieldDescriptor createFieldDescriptor(IVariableBinding variableBinding) {
    if (isArrayLengthBinding(variableBinding)) {
      return AstUtils.ARRAY_LENGTH_FIELD_DESCRIPTION;
    }

    int modifiers = variableBinding.getModifiers();
    boolean isStatic = isStatic(modifiers);
    Visibility visibility = getVisibility(modifiers);
    TypeDescriptor enclosingClassTypeDescriptor =
        createTypeDescriptor(variableBinding.getDeclaringClass());
    String fieldName = variableBinding.getName();
    TypeDescriptor thisTypeDescriptor = createTypeDescriptor(variableBinding.getType());
    boolean isRaw = JsInteropUtils.isJsProperty(variableBinding);
    return isRaw
        ? FieldDescriptor.createRaw(
            isStatic, enclosingClassTypeDescriptor, fieldName, thisTypeDescriptor)
        : FieldDescriptor.create(
            isStatic, visibility, enclosingClassTypeDescriptor, fieldName, thisTypeDescriptor);
  }

  public static MethodDescriptor createMethodDescriptor(IMethodBinding methodBinding) {
    int modifiers = methodBinding.getModifiers();
    boolean isStatic = isStatic(modifiers);
    Visibility visibility = getVisibility(modifiers);
    boolean isNative = isNative(modifiers);
    TypeDescriptor enclosingClassTypeDescriptor =
        createTypeDescriptor(methodBinding.getDeclaringClass());
    boolean isConstructor = methodBinding.isConstructor();
    String methodName =
        isConstructor
            ? createTypeDescriptor(methodBinding.getDeclaringClass()).getClassName()
            : methodBinding.getName();
    boolean isRaw = false;

    JsInfo jsInfo = computeJsInfo(methodBinding);
    isRaw = isOrOverridesJsMember(methodBinding);

    TypeDescriptor returnTypeDescriptor = createTypeDescriptor(methodBinding.getReturnType());

    // generate parameters type descriptors.
    Iterable<TypeDescriptor> parameterTypeDescriptors =
        FluentIterable.from(Arrays.asList(methodBinding.getMethodDeclaration().getParameterTypes()))
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    // Whenever we create the parameter types of a method,
                    // we use the rawTypeDescriptor.
                    return createTypeDescriptor(typeBinding).getRawTypeDescriptor();
                  }
                });
    // generate type parameters declared in the method.
    Iterable<TypeDescriptor> typeParameterDescriptors =
        FluentIterable.from(Arrays.asList(methodBinding.getTypeParameters()))
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    return createTypeDescriptor(typeBinding);
                  }
                });

    return MethodDescriptor.create(
        isStatic,
        isRaw,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        parameterTypeDescriptors,
        typeParameterDescriptors,
        jsInfo);
  }

  static Variable createVariable(IVariableBinding variableBinding) {
    String name = variableBinding.getName();
    TypeDescriptor typeDescriptor = createTypeDescriptor(variableBinding.getType());
    boolean isFinal = isFinal(variableBinding.getModifiers());
    boolean isParameter = variableBinding.isParameter();
    return new Variable(name, typeDescriptor, isFinal, isParameter);
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
        return BinaryOperator.XOR;
      case "&":
        return BinaryOperator.AND;
      case "|":
        return BinaryOperator.OR;
      case "&&":
        return BinaryOperator.CONDITIONAL_AND;
      case "||":
        return BinaryOperator.CONDITIONAL_OR;
    }
    return null;
  }

  public static Kind getKindFromTypeBinding(ITypeBinding typeBinding) {
    if (typeBinding.isInterface()) {
      return Kind.INTERFACE;
    } else if (typeBinding.isClass() || typeBinding.isEnum() && typeBinding.isAnonymous()) {
      // Enum values that are anonymous inner classes, are not consider enums classes in
      // our AST, but are considered enum classes by JDT.
      return Kind.CLASS;
    } else if (typeBinding.isEnum()) {
      Preconditions.checkArgument(!typeBinding.isAnonymous());
      return Kind.ENUM;
    }

    throw new RuntimeException("Type binding " + typeBinding + " not handled");
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
   * Returns the methods that are declared by interfaces of {@code typeBinding} and are not
   * implemented by {@code typeBinding}.
   */
  static List<IMethodBinding> getUnimplementedMethodBindings(ITypeBinding typeBinding) {
    List<IMethodBinding> unimplementedMethodBindings = new ArrayList<>();
    // Only abstract class may have unimplemented methods.
    if (!isAbstract(typeBinding.getModifiers())) {
      return unimplementedMethodBindings;
    }
    for (ITypeBinding superInterface : getAllInterfaces(typeBinding)) {
      unimplementedMethodBindings.addAll(
          getUnimplementedMethodBindings(superInterface, typeBinding));
    }
    return unimplementedMethodBindings;
  }

  /**
   * Returns the methods that are declared by {@code superTypeBinding} and are not implemented
   * by {@code typeBinding}.
   */
  private static List<IMethodBinding> getUnimplementedMethodBindings(
      ITypeBinding superTypeBinding, final ITypeBinding typeBinding) {
    return filterMethodBindings(
        superTypeBinding.getDeclaredMethods(),
        new Predicate<IMethodBinding>() {
          @Override
          public boolean apply(IMethodBinding methodBinding) {
            return !isImplementedBy(methodBinding, typeBinding);
          }
        });
  }

  /**
   * Returns true if {@code typeBinding} implements an overridden method of {@code methodBinding}.
   */
  private static boolean isImplementedBy(IMethodBinding methodBinding, ITypeBinding typeBinding) {
    if (isDeclaredBy(methodBinding, typeBinding)) {
      return true;
    }
    ITypeBinding superclassTypeBinding = typeBinding.getSuperclass();
    // implemented by its superclass.
    return superclassTypeBinding != null && isImplementedBy(methodBinding, superclassTypeBinding);
  }

  /**
   * Returns the methods in {@code typeBinding}'s interfaces that are accidentally overridden.
   *
   * <p>'Accidentally overridden' means the type itself does not have its own declared overriding
   * method, and the method it inherits does not really override, but just has the same signature of
   * the overridden method.
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
   * Returns the methods that are declared by {@code superTypeBinding} but are not declared
   * by {@code typeBinding}.
   */
  private static List<IMethodBinding> getUndeclaredMethodBindings(
      ITypeBinding superTypeBinding, final ITypeBinding typeBinding) {
    return filterMethodBindings(
        superTypeBinding.getDeclaredMethods(),
        new Predicate<IMethodBinding>() {
          @Override
          public boolean apply(IMethodBinding methodBinding) {
            return !isDeclaredBy(methodBinding, typeBinding);
          }
        });
  }

  /**
   * Returns true if {@code typeBinding} declares a method with the same signature of
   * {@code methodBinding} in its body.
   */
  private static boolean isDeclaredBy(IMethodBinding methodBinding, ITypeBinding typeBinding) {
    for (IMethodBinding declaredMethodBinding : typeBinding.getDeclaredMethods()) {
      if (areParameterErasureEqual(declaredMethodBinding, methodBinding)) {
        return true;
      }
    }
    return false;
  }

  private static List<IMethodBinding> filterMethodBindings(
      IMethodBinding[] methodBindings, Predicate<IMethodBinding> predicate) {
    return Lists.newArrayList(FluentIterable.from(methodBindings).filter(predicate));
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
   * Returns the nearest method in the super classes of {code typeBinding} that overrides (regularly
   * or accidentally) {@code methodBinding}.
   */
  static IMethodBinding getOverridingMethodInSuperclasses(
      IMethodBinding methodBinding, ITypeBinding typeBinding) {
    ITypeBinding superclass = typeBinding.getSuperclass();
    while (superclass != null) {
      for (IMethodBinding methodInSuperclass : superclass.getDeclaredMethods()) {
        // TODO: excludes package private method, and add a test for it.
        if (JdtUtils.areParameterErasureEqual(methodInSuperclass, methodBinding)) {
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

  static Visibility getVisibility(int modifiers) {
    return TypeProxyUtils.getVisibility(modifiers);
  }

  static boolean isArrayLengthBinding(IVariableBinding variableBinding) {
    return variableBinding.getName().equals("length")
        && variableBinding.isField()
        && variableBinding.getDeclaringClass() == null;
  }

  static boolean isAbstract(int modifier) {
    return Modifier.isAbstract(modifier);
  }

  static boolean isFinal(int modifier) {
    return Modifier.isFinal(modifier);
  }

  static boolean isStatic(int modifier) {
    return Modifier.isStatic(modifier);
  }

  static boolean isNative(int modifier) {
    return Modifier.isNative(modifier);
  }

  static boolean isInstanceNestedClass(ITypeBinding typeBinding) {
    return typeBinding.getDeclaringClass() != null && !isStatic(typeBinding.getModifiers());
  }

  static boolean isInstanceMemberClass(ITypeBinding typeBinding) {
    return typeBinding.isMember() && !isStatic(typeBinding.getModifiers());
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
          && areParameterErasureEqual(overridingMethod, method.getMethodDeclaration())) {
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
    Preconditions.checkArgument(overridingMethod.overrides(overriddenMethod));
    Visibility overriddenMethodVisibility = getVisibility(overriddenMethod.getModifiers());
    Visibility overridingMethodVisibility = getVisibility(overridingMethod.getModifiers());
    return overriddenMethodVisibility.isPackagePrivate()
        && (overridingMethodVisibility.isPublic() || overridingMethodVisibility.isProtected());
  }

  /**
   * Two methods are parameter erasure equal if the erasure of their parameters' types are equal.
   * Parameter erasure equal means that they are overriding signature equal, which means that they
   * are real overriding/overridden or accidental overriding/overridden.
   */
  static boolean areParameterErasureEqual(IMethodBinding leftMethod, IMethodBinding rightMethod) {
    ITypeBinding[] leftParameterTypes = leftMethod.getParameterTypes();
    ITypeBinding[] rightParameterTypes = rightMethod.getParameterTypes();
    if (!leftMethod.getName().equals(rightMethod.getName())
        || leftParameterTypes.length != rightParameterTypes.length) {
      return false;
    }
    for (int i = 0; i < leftParameterTypes.length; i++) {
      ITypeBinding leftParameterType = leftParameterTypes[i].getErasure();
      ITypeBinding rightParameterType = rightParameterTypes[i].getErasure();
      if (!leftParameterType.isEqualTo(rightParameterType)) {
        return false;
      }
    }
    return true;
  }

  static IMethodBinding findSamMethodBinding(ITypeBinding typeBinding) {
    // TODO: there maybe an issue in which case it inherits a default method from an interface
    // and inherits an abstract method with the same signature from another interface. Add an
    // example to address the potential issue.
    Preconditions.checkArgument(typeBinding.isInterface());
    for (IMethodBinding method : typeBinding.getDeclaredMethods()) {
      if (isAbstract(method.getModifiers())) {
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

  static JavaType createLambdaJavaType(
      String lambdaBinaryName,
      ITypeBinding lambdaInterfaceBinding,
      RegularTypeDescriptor enclosingClassTypeDescriptor) {
    TypeDescriptor lambdaClassTypeDescriptor =
        TypeDescriptor.createSynthetic(
            enclosingClassTypeDescriptor.getPackageComponents(),
            Iterables.concat(
                enclosingClassTypeDescriptor.getClassComponents(),
                Arrays.asList(lambdaBinaryName)));
    JavaType lambdaType = new JavaType(Kind.CLASS, Visibility.PRIVATE, lambdaClassTypeDescriptor);

    lambdaType.setEnclosingTypeDescriptor(enclosingClassTypeDescriptor);
    lambdaType.setSuperTypeDescriptor(TypeDescriptors.get().javaLangObject);
    lambdaType.addSuperInterfaceDescriptor(createTypeDescriptor(lambdaInterfaceBinding));
    lambdaType.setLocal(true);
    return lambdaType;
  }

  static Method createSamMethod(
      ITypeBinding lambdaInterfaceBinding, MethodDescriptor lambdaMethodDescriptor) {
    IMethodBinding samMethodBinding = JdtUtils.findSamMethodBinding(lambdaInterfaceBinding);
    Preconditions.checkNotNull(samMethodBinding);
    MethodDescriptor samMethodDescriptor = JdtUtils.createMethodDescriptor(samMethodBinding);
    List<Variable> parameters = new ArrayList<>();
    List<Expression> arguments = new ArrayList<>();
    for (int i = 0; i < lambdaMethodDescriptor.getParameterTypeDescriptors().size(); i++) {
      Variable parameter =
          new Variable(
              "arg" + i, lambdaMethodDescriptor.getParameterTypeDescriptors().get(i), false, true);
      parameters.add(parameter);
      arguments.add(parameter.getReference());
    }
    Expression callLambda =
        MethodCall.createRegularMethodCall(null, lambdaMethodDescriptor, arguments);
    Statement statement =
        lambdaMethodDescriptor.getReturnTypeDescriptor() == TypeDescriptors.get().primitiveVoid
            ? new ExpressionStatement(callLambda)
            : new ReturnStatement(callLambda, samMethodDescriptor.getReturnTypeDescriptor());
    Method samMethod =
        new Method(
            samMethodDescriptor,
            parameters,
            new Block(Arrays.asList(statement)),
            false,
            true,
            false);
    return samMethod;
  }

  /**
   * Returns whether {@code subTypeBinding} is a subtype of {@code superTypeBinding}.
   */
  static boolean isSubType(ITypeBinding subTypeBinding, ITypeBinding superTypeBinding) {
    if (areSameErasedType(superTypeBinding, subTypeBinding)) {
      return true;
    }
    ITypeBinding superClass = subTypeBinding.getSuperclass();
    while (superClass != null) {
      if (areSameErasedType(superTypeBinding, superClass)) {
        return true;
      }
      superClass = superClass.getSuperclass();
    }
    return false;
  }

  static boolean areSameErasedType(ITypeBinding typeBinding, ITypeBinding otherTypeBinding) {
    return typeBinding.getErasure().isEqualTo(otherTypeBinding.getErasure());
  }

  static boolean isOrOverridesJsMember(IMethodBinding methodBinding) {
    return JsInteropUtils.isJsMember(methodBinding)
        || !getOverriddenJsMembers(methodBinding).isEmpty();
  }

  /**
   * Checks overriding chain to compute JsInfo.
   */
  static JsInfo computeJsInfo(IMethodBinding methodBinding) {
    // The direct JsInfo.
    JsInfo jsInfo = JsInteropUtils.getJsInfo(methodBinding);
    if (jsInfo.getJsMemberType().isJsMember()) {
      return jsInfo;
    }
    // Checks overriding chain.
    for (IMethodBinding overriddenMethod : getOverriddenMethods(methodBinding)) {
      JsInfo inheritedJsInfo = JsInteropUtils.getJsInfo(overriddenMethod);
      if (inheritedJsInfo.getJsMemberType().isJsMember()) {
        return inheritedJsInfo;
      }
    }
    return JsInfo.NONE;
  }

  static Set<IMethodBinding> getOverriddenJsMembers(IMethodBinding methodBinding) {
    return Sets.filter(
        getOverriddenMethods(methodBinding),
        new Predicate<IMethodBinding>() {
          @Override
          public boolean apply(IMethodBinding overriddenMethod) {
            return JsInteropUtils.isJsMember(overriddenMethod);
          }
        });
  }

  static Set<IMethodBinding> getOverriddenMethods(IMethodBinding methodBinding) {
    Set<IMethodBinding> overriddenMethods = new HashSet<>();
    ITypeBinding enclosingClass = methodBinding.getDeclaringClass();
    ITypeBinding superClass = enclosingClass.getSuperclass();
    if (superClass != null) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, superClass));
    }
    for (ITypeBinding superInterface : enclosingClass.getInterfaces()) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, superInterface));
    }
    return overriddenMethods;
  }

  static Set<IMethodBinding> getOverriddenMethodsInType(
      IMethodBinding methodBinding, ITypeBinding typeBinding) {
    Set<IMethodBinding> overriddenMethods = new HashSet<>();
    for (IMethodBinding declaredMethod : typeBinding.getDeclaredMethods()) {
      if (methodBinding.overrides(declaredMethod)) {
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
        case ASTNode.METHOD_DECLARATION:
          return isStatic(((MethodDeclaration) currentNode).getModifiers());
        case ASTNode.FIELD_DECLARATION:
          return isStatic(((FieldDeclaration) currentNode).getModifiers());
        case ASTNode.INITIALIZER:
          return isStatic(((Initializer) currentNode).getModifiers());
        case ASTNode.ENUM_CONSTANT_DECLARATION: // enum constants are implicitly static.
          return true;
      }
      currentNode = currentNode.getParent();
    }
    return false;
  }

  private JdtUtils() {}
}
