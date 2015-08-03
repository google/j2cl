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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.ASTUtils;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaType.Kind;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

  static TypeDescriptor createTypeDescriptor(
      ITypeBinding typeBinding, final CompilationUnitNameLocator compilationUnitNameLocator) {
    if (typeBinding == null) {
      return null;
    }
    if (typeBinding.isPrimitive()) {
      switch (typeBinding.getName()) {
        case TypeDescriptor.BOOLEAN_TYPE_NAME:
          return TypeDescriptors.BOOLEAN_TYPE_DESCRIPTOR;
        case TypeDescriptor.BYTE_TYPE_NAME:
          return TypeDescriptors.BYTE_TYPE_DESCRIPTOR;
        case TypeDescriptor.SHORT_TYPE_NAME:
          return TypeDescriptors.SHORT_TYPE_DESCRIPTOR;
        case TypeDescriptor.INT_TYPE_NAME:
          return TypeDescriptors.INT_TYPE_DESCRIPTOR;
        case TypeDescriptor.LONG_TYPE_NAME:
          return TypeDescriptors.LONG_TYPE_DESCRIPTOR;
        case TypeDescriptor.FLOAT_TYPE_NAME:
          return TypeDescriptors.FLOAT_TYPE_DESCRIPTOR;
        case TypeDescriptor.DOUBLE_TYPE_NAME:
          return TypeDescriptors.DOUBLE_TYPE_DESCRIPTOR;
        case TypeDescriptor.CHAR_TYPE_NAME:
          return TypeDescriptors.CHAR_TYPE_DESCRIPTOR;
        case TypeDescriptor.VOID_TYPE_NAME:
          return TypeDescriptors.VOID_TYPE_DESCRIPTOR;
        default:
          Preconditions.checkArgument(
              false, "Primitive type name '" + typeBinding.getName() + "' is unrecognized.");
      }
    }
    List<String> nameComponents = new LinkedList<>();
    List<String> packageComponents = new LinkedList<>();

    if (typeBinding.getPackage() != null) {
      packageComponents = Arrays.asList(typeBinding.getPackage().getNameComponents());
    }

    if (typeBinding.isArray()) {
      TypeDescriptor leafTypeDescriptor =
          createTypeDescriptor(typeBinding.getElementType(), compilationUnitNameLocator);
      return leafTypeDescriptor.getForArray(typeBinding.getDimensions());
    }

    // WildCard type and its capture are translated to WildCardType '?'.
    if (typeBinding.isWildcardType() || typeBinding.isCapture()) {
      return TypeDescriptors.WILD_CARD_TYPE_DESCRIPTOR;
    }

    ITypeBinding currentType = typeBinding;
    while (currentType != null) {
      String simpleName;
      if (currentType.isLocal()) {
        // JDT binary name for local class is like package.components.EnclosingClass$1SimpleName
        // Extract the name after the last '$' as the class component here.
        String binaryName = currentType.getErasure().getBinaryName();
        simpleName = binaryName.substring(binaryName.lastIndexOf('$') + 1);
      } else if (currentType.isTypeVariable()) {
        if (currentType.getDeclaringClass() != null) {
          // If it is a class-level type variable, use the simple name (with prefix "C_") as the
          // current name component.
          simpleName = ASTUtils.TYPE_VARIABLE_IN_TYPE_PREFIX + currentType.getName();
        } else {
          // If it is a method-level type variable, use the simple name (with prefix "M_") as the
          // current name component, and add declaringClass_declaringMethod as the next name
          // component, and set currentType to declaringClass for the next iteration.
          nameComponents.add(0, ASTUtils.TYPE_VARIABLE_IN_METHOD_PREFIX + currentType.getName());
          simpleName =
              currentType.getDeclaringMethod().getDeclaringClass().getName()
                  + "_"
                  + currentType.getDeclaringMethod().getName();
          currentType = currentType.getDeclaringMethod().getDeclaringClass();
        }
      } else {
        simpleName = currentType.getErasure().getName();
      }
      nameComponents.add(0, simpleName);
      currentType = currentType.getDeclaringClass();
    }

    if (typeBinding.isTypeVariable()) {
      return TypeDescriptor.createTypeVariable(
          packageComponents, nameComponents, compilationUnitNameLocator.find(typeBinding));
    }

    if (typeBinding.isParameterizedType()) {
      Iterable<TypeDescriptor> typeArguments =
          createTypeDescriptors(typeBinding.getTypeArguments(), compilationUnitNameLocator);
      return TypeDescriptor.createParameterizedType(
          packageComponents,
          nameComponents,
          compilationUnitNameLocator.find(typeBinding),
          typeArguments);
    }

    List<TypeDescriptor> typeParameters = new ArrayList<>();
    if (typeBinding.isGenericType()) {
      Iterables.addAll(
          typeParameters,
          createTypeDescriptors(typeBinding.getTypeParameters(), compilationUnitNameLocator));
    }
    // add captured type parameters
    if (isInstanceNestedClass(typeBinding)) {
      if (typeBinding.getDeclaringMethod() != null) {
        Iterables.addAll(
            typeParameters,
            createTypeDescriptors(
                typeBinding.getDeclaringMethod().getTypeParameters(), compilationUnitNameLocator));
      }
      typeParameters.addAll(
          createTypeDescriptor(typeBinding.getDeclaringClass(), compilationUnitNameLocator)
              .getTypeArgumentDescriptors());
    }
    return TypeDescriptor.createParameterizedType(
        packageComponents,
        nameComponents,
        compilationUnitNameLocator.find(typeBinding),
        typeParameters);
  }

  static Iterable<TypeDescriptor> createTypeDescriptors(
      ITypeBinding[] typeBindings, final CompilationUnitNameLocator compilationUnitNameLocator) {
    return FluentIterable.from(Arrays.asList(typeBindings))
        .transform(
            new Function<ITypeBinding, TypeDescriptor>() {
              @Override
              public TypeDescriptor apply(ITypeBinding typeBinding) {
                return createTypeDescriptor(typeBinding, compilationUnitNameLocator);
              }
            });
  }

  static FieldDescriptor createFieldDescriptor(
      IVariableBinding variableBinding, CompilationUnitNameLocator compilationUnitNameLocator) {
    if (isArrayLengthBinding(variableBinding)) {
      return FieldDescriptor.createRaw(
          false,
          TypeDescriptors.VOID_TYPE_DESCRIPTOR,
          "length",
          TypeDescriptors.INT_TYPE_DESCRIPTOR);
    }

    int modifiers = variableBinding.getModifiers();
    boolean isStatic = isStatic(modifiers);
    Visibility visibility = getVisibility(modifiers);
    TypeDescriptor enclosingClassTypeDescriptor =
        createTypeDescriptor(variableBinding.getDeclaringClass(), compilationUnitNameLocator);
    String fieldName = variableBinding.getName();
    TypeDescriptor thisTypeDescriptor =
        createTypeDescriptor(variableBinding.getType(), compilationUnitNameLocator);
    return FieldDescriptor.create(
        isStatic, visibility, enclosingClassTypeDescriptor, fieldName, thisTypeDescriptor);
  }

  static MethodDescriptor createMethodDescriptor(
      IMethodBinding methodBinding, final CompilationUnitNameLocator compilationUnitNameLocator) {
    int modifiers = methodBinding.getModifiers();
    boolean isStatic = isStatic(modifiers);
    Visibility visibility = getVisibility(modifiers);
    boolean isNative = isNative(modifiers);
    TypeDescriptor enclosingClassTypeDescriptor =
        createTypeDescriptor(methodBinding.getDeclaringClass(), compilationUnitNameLocator);
    boolean isConstructor = methodBinding.isConstructor();
    String methodName =
        isConstructor
            ? createTypeDescriptor(methodBinding.getDeclaringClass(), compilationUnitNameLocator)
                .getClassName()
            : methodBinding.getName();
    TypeDescriptor returnTypeDescriptor =
        createTypeDescriptor(methodBinding.getReturnType(), compilationUnitNameLocator);

    // generate parameters type descriptors.
    Iterable<TypeDescriptor> parameterTypeDescriptors =
        FluentIterable.from(Arrays.asList(methodBinding.getParameterTypes()))
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    return createTypeDescriptor(typeBinding, compilationUnitNameLocator);
                  }
                });
    // generate type parameters declared in the method.
    Iterable<TypeDescriptor> typeParameterDescriptors =
        FluentIterable.from(Arrays.asList(methodBinding.getTypeParameters()))
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    return createTypeDescriptor(typeBinding, compilationUnitNameLocator);
                  }
                });

    // If it is a parameterized method, declaredMethodBinding is the binding of the generic method.
    // For example, <T> void foo(T a); foo("a");
    // methodBinding is void foo(String), and declaredMethodBinding is void foo(T)
    IMethodBinding declaredMethodBinding = methodBinding.getMethodDeclaration();
    // generate erasureMethodDescriptor using erasure of parameter types.
    Iterable<TypeDescriptor> erasureParameterTypeDescriptors =
        FluentIterable.from(Arrays.asList(declaredMethodBinding.getParameterTypes()))
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    return createTypeDescriptor(
                        typeBinding.getErasure(), compilationUnitNameLocator);
                  }
                });
    MethodDescriptor erasureMethodDescriptor =
        MethodDescriptor.create(
            isStatic,
            visibility,
            enclosingClassTypeDescriptor,
            methodName,
            isConstructor,
            isNative,
            returnTypeDescriptor,
            erasureParameterTypeDescriptors);

    return MethodDescriptor.create(
        isStatic,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        parameterTypeDescriptors,
        typeParameterDescriptors,
        erasureMethodDescriptor);
  }

  static Variable createVariable(
      IVariableBinding variableBinding, CompilationUnitNameLocator compilationUnitNameLocator) {
    String name = variableBinding.getName();
    TypeDescriptor typeDescriptor =
        createTypeDescriptor(variableBinding.getType(), compilationUnitNameLocator);
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
    } else if (typeBinding.isEnum()) {
      return Kind.ENUM;
    } else if (typeBinding.isClass()) {
      return Kind.CLASS;
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

  static Visibility getVisibility(int modifier) {
    if (Modifier.isPublic(modifier)) {
      return Visibility.PUBLIC;
    } else if (Modifier.isProtected(modifier)) {
      return Visibility.PROTECTED;
    } else if (Modifier.isPrivate(modifier)) {
      return Visibility.PRIVATE;
    } else {
      return Visibility.PACKAGE_PRIVATE;
    }
  }

  static boolean isArrayLengthBinding(IVariableBinding variableBinding) {
    return variableBinding.getName().equals("length")
        && variableBinding.isField()
        && variableBinding.getDeclaringClass() == null;
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

  static boolean isOverride(IMethodBinding overridingMethod) {
    ITypeBinding type = overridingMethod.getDeclaringClass();

    // Check immediate super class and interfaces for overridden method.
    if (type.getSuperclass() != null && hasOverideeInType(overridingMethod, type.getSuperclass())) {
      return true;
    }
    for (ITypeBinding interfaceBinding : type.getInterfaces()) {
      if (hasOverideeInType(overridingMethod, interfaceBinding)) {
        return true;
      }
    }

    return false;
  }

  private static boolean hasOverideeInType(IMethodBinding overridingMethod, ITypeBinding type) {
    for (IMethodBinding method : type.getDeclaredMethods()) {
      // TODO: this may need to be fixed for generic methods overriding.
      if (overridingMethod.overrides(method)) {
        return true;
      }
    }

    // Recurse into immediate super class and interfaces for overridden method.
    if (type.getSuperclass() != null && hasOverideeInType(overridingMethod, type.getSuperclass())) {
      return true;
    }
    for (ITypeBinding interfaceBinding : type.getInterfaces()) {
      if (hasOverideeInType(overridingMethod, interfaceBinding)) {
        return true;
      }
    }

    return false;
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

  private JdtUtils() {}
}
