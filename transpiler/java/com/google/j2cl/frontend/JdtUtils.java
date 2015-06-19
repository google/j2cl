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

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.FieldReference;
import com.google.j2cl.ast.MethodReference;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.RegularTypeReference;
import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;

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

  static TypeReference createTypeReference(
      ITypeBinding typeBinding, CompilationUnitNameLocator compilationUnitNameLocator) {
    if (typeBinding == null) {
      return null;
    }
    List<String> nameComponents = new LinkedList<>();
    List<String> packageComponents = new LinkedList<>();
    if (typeBinding.isArray()) {
      RegularTypeReference leafType =
          (RegularTypeReference)
              createTypeReference(typeBinding.getElementType(), compilationUnitNameLocator);
      return leafType.getArray(typeBinding.getDimensions());
    }

    ITypeBinding currentType = typeBinding;
    while (currentType != null) {
      nameComponents.add(0, currentType.getName());
      currentType = currentType.getDeclaringClass();
    }

    if (typeBinding.getPackage() != null) {
      packageComponents = Arrays.asList(typeBinding.getPackage().getNameComponents());
    }

    return RegularTypeReference.create(
        packageComponents, nameComponents, compilationUnitNameLocator.find(typeBinding));
  }

  static FieldReference createFieldReference(
      IVariableBinding variableBinding, CompilationUnitNameLocator compilationUnitNameLocator) {
    int modifiers = variableBinding.getModifiers();
    boolean isStatic = isStatic(modifiers);
    Visibility visibility = getVisibility(modifiers);
    TypeReference enclosingClassReference =
        createTypeReference(variableBinding.getDeclaringClass(), compilationUnitNameLocator);
    String fieldName = variableBinding.getName();
    TypeReference type = createTypeReference(variableBinding.getType(), compilationUnitNameLocator);
    return FieldReference.create(isStatic, visibility, enclosingClassReference, fieldName, type);
  }

  static MethodReference createMethodReference(
      IMethodBinding methodBinding, CompilationUnitNameLocator compilationUnitNameLocator) {
    int modifiers = methodBinding.getModifiers();
    boolean isStatic = isStatic(modifiers);
    Visibility visibility = getVisibility(modifiers);
    TypeReference enclosingClassReference =
        createTypeReference(methodBinding.getDeclaringClass(), compilationUnitNameLocator);
    String methodName = methodBinding.getName();
    boolean isConstructor = methodBinding.isConstructor();
    TypeReference returnTypeReference =
        createTypeReference(methodBinding.getReturnType(), compilationUnitNameLocator);
    int parameterSize = methodBinding.getParameterTypes().length;
    TypeReference[] parameterTypeReferences = new TypeReference[parameterSize];
    for (int i = 0; i < parameterSize; i++) {
      parameterTypeReferences[i] =
          createTypeReference(methodBinding.getParameterTypes()[i], compilationUnitNameLocator);
    }
    return MethodReference.create(
        isStatic,
        visibility,
        enclosingClassReference,
        methodName,
        isConstructor,
        returnTypeReference,
        parameterTypeReferences);
  }

  static Variable createVariable(
      IVariableBinding variableBinding, CompilationUnitNameLocator compilationUnitNameLocator) {
    String name = variableBinding.getName();
    TypeReference type = createTypeReference(variableBinding.getType(), compilationUnitNameLocator);
    boolean isFinal = isFinal(variableBinding.getModifiers());
    boolean isParameter = variableBinding.isParameter();
    return new Variable(name, type, isFinal, isParameter);
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

  static boolean isFinal(int modifier) {
    return Modifier.isFinal(modifier);
  }

  static boolean isStatic(int modifier) {
    return Modifier.isStatic(modifier);
  }

  private JdtUtils() {}
}
