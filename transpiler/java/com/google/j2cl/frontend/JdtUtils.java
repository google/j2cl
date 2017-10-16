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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AstUtilConstants;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsMemberType;
import com.google.j2cl.ast.Kind;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.SingletonInitializer;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

/** Utility functions to manipulate JDT internal representations. */
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
      return AstUtilConstants.getArrayLengthFieldDescriptor();
    }

    boolean isStatic = isStatic(variableBinding);
    Visibility visibility = getVisibility(variableBinding);
    TypeDescriptor enclosingTypeDescriptor =
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
    boolean isFinal = JdtUtils.isFinal(variableBinding);
    return FieldDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setName(fieldName)
        .setTypeDescriptor(thisTypeDescriptor)
        .setStatic(isStatic)
        .setVisibility(visibility)
        .setJsInfo(jsInfo)
        .setFinal(isFinal)
        .setCompileTimeConstant(isCompileTimeConstant)
        .setDeclarationFieldDescriptor(declarationFieldDescriptor)
        .setUnusableByJsSuppressed(
            JsInteropAnnotationUtils.isUnusableByJsSuppressed(variableBinding))
        .build();
  }

  static Variable createVariable(SourcePosition sourcePosition, IVariableBinding variableBinding) {
    String name = variableBinding.getName();
    TypeDescriptor typeDescriptor =
        variableBinding.isParameter()
            ? createTypeDescriptorWithNullability(
                variableBinding.getType(), variableBinding.getAnnotations())
            : createTypeDescriptor(variableBinding.getType());
    boolean isFinal = isFinal(variableBinding);
    boolean isParameter = variableBinding.isParameter();
    boolean isUnusableByJsSuppressed =
        JsInteropAnnotationUtils.isUnusableByJsSuppressed(variableBinding);
    return Variable.newBuilder()
        .setName(name)
        .setTypeDescriptor(typeDescriptor)
        .setFinal(isFinal)
        .setParameter(isParameter)
        .setUnusableByJsSuppressed(isUnusableByJsSuppressed)
        .setSourcePosition(sourcePosition)
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
      default:
        return null;
    }
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
      default:
        return null;
    }
  }

  static IMethodBinding getMethodBinding(
      ITypeBinding typeBinding, String methodName, ITypeBinding... parameterTypes) {

    Queue<ITypeBinding> deque = new ArrayDeque<>();
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
      default:
        return null;
    }
  }

  public static PostfixOperator getPostfixOperator(PostfixExpression.Operator operator) {
    switch (operator.toString()) {
      case "++":
        return PostfixOperator.INCREMENT;
      case "--":
        return PostfixOperator.DECREMENT;
      default:
        return null;
    }
  }

  static boolean isStatic(BodyDeclaration bodyDeclaration) {
    return Modifier.isStatic(bodyDeclaration.getModifiers());
  }

  static boolean isArrayLengthBinding(IVariableBinding variableBinding) {
    return variableBinding.getName().equals("length")
        && variableBinding.isField()
        && variableBinding.getDeclaringClass() == null;
  }

  /** Returns true if the binding is annotated with @UncheckedCast. */
  static boolean hasUncheckedCastAnnotation(IBinding binding) {
    return JdtAnnotationUtils.hasAnnotation(binding, "javaemul.internal.annotations.UncheckedCast");
  }

  static IMethodBinding findFunctionalMethodBinding(ITypeBinding typeBinding) {
    // TODO(leafwang): there maybe an issue in which case it inherits a default method from an
    // interface and inherits an abstract method with the same signature from another interface.
    // Add an example to address the potential issue.
    checkArgument(typeBinding.isInterface());
    for (IMethodBinding method : typeBinding.getDeclaredMethods()) {
      if (isAbstract(method)) {
        return method;
      }
    }
    for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
      IMethodBinding functionalMethodBinding = findFunctionalMethodBinding(superInterface);
      if (functionalMethodBinding != null) {
        return functionalMethodBinding;
      }
    }
    return null;
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

  /** Helper method to work around JDT habit of returning raw collections. */
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

  /** Returns the type binding of the immediately enclosing type. */
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

  /** Returns whether the ASTNode is in a static context. */
  public static boolean isInStaticContext(org.eclipse.jdt.core.dom.ASTNode node) {
    org.eclipse.jdt.core.dom.ASTNode currentNode = node.getParent();
    while (currentNode != null) {
      switch (currentNode.getNodeType()) {
        case ASTNode.FIELD_DECLARATION:
          if (findCurrentTypeBinding(currentNode).isInterface()) {
            // Field declarations in interface are implicitly static.
            return true;
          }
          // fall through
        case ASTNode.METHOD_DECLARATION:
        case ASTNode.INITIALIZER:
          return isStatic((BodyDeclaration) currentNode);
        case ASTNode.ENUM_CONSTANT_DECLARATION: // enum constants are implicitly static.
          return true;
        default:
          break;
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
  private static TypeDescriptor createTypeDescriptorWithNullability(
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

    if (typeBinding.isNullType()) {
      return TypeDescriptors.get().javaLangObject;
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
            && !binding.isNullType()
            && binding.getPackage() == null;
    if (isIntersectionType) {
      checkArgument(
          (binding.getSuperclass() != null && binding.getInterfaces().length >= 1)
              || (binding.getSuperclass() == null && binding.getInterfaces().length >= 2));
    }
    return isIntersectionType;
  }

  public static List<String> getClassComponents(ITypeBinding typeBinding) {
    List<String> classComponents = new ArrayList<>();
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
          simpleName = AstUtilConstants.TYPE_VARIABLE_IN_TYPE_PREFIX + currentType.getName();
        } else {
          // If it is a method-level type variable, use the simple name (with prefix "M_") as the
          // current name component, and add declaringClass_declaringMethod as the next name
          // component, and set currentType to declaringClass for the next iteration.
          classComponents.add(
              0, AstUtilConstants.TYPE_VARIABLE_IN_METHOD_PREFIX + currentType.getName());
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

  /** Creates a MethodDescriptor directly based on the given JDT method binding. */
  public static MethodDescriptor createMethodDescriptor(IMethodBinding methodBinding) {
    TypeDescriptor enclosingTypeDescriptor =
        createTypeDescriptor(methodBinding.getDeclaringClass());

    boolean isStatic = isStatic(methodBinding);
    Visibility visibility = getVisibility(methodBinding);
    boolean isDefault = isDefaultMethod(methodBinding);
    JsInfo jsInfo = computeJsInfo(methodBinding);

    boolean isNative =
        Modifier.isNative(methodBinding.getModifiers())
            || (!jsInfo.isJsOverlay()
                && enclosingTypeDescriptor.isNative()
                && isAbstract(methodBinding));

    boolean isConstructor = methodBinding.isConstructor();
    String methodName = methodBinding.getName();

    TypeDescriptor returnTypeDescriptor =
        createTypeDescriptorWithNullability(
            methodBinding.getReturnType(), methodBinding.getAnnotations());

    MethodDescriptor declarationMethodDescriptor = null;
    if (methodBinding.getMethodDeclaration() != methodBinding) {
      declarationMethodDescriptor = createMethodDescriptor(methodBinding.getMethodDeclaration());
    }

    // generate type parameters declared in the method.
    Iterable<TypeDescriptor> typeParameterTypeDescriptors =
        FluentIterable.from(methodBinding.getTypeParameters())
            .transform(JdtUtils::createTypeDescriptor);

    ImmutableList.Builder<ParameterDescriptor> parameterDescriptorBuilder = ImmutableList.builder();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      parameterDescriptorBuilder.add(
          ParameterDescriptor.newBuilder()
              .setTypeDescriptor(
                  createTypeDescriptorWithNullability(
                      methodBinding.getParameterTypes()[i],
                      methodBinding.getParameterAnnotations(i)))
              .setJsOptional(JsInteropUtils.isJsOptional(methodBinding, i))
              .setVarargs(
                  i == methodBinding.getParameterTypes().length - 1 && methodBinding.isVarargs())
              .setDoNotAutobox(JsInteropUtils.isDoNotAutobox(methodBinding, i))
              .build());
    }

    if (enclosingTypeDescriptor.isAnonymous()
        && isConstructor
        && enclosingTypeDescriptor.getSuperTypeDescriptor().hasJsConstructor()) {
      jsInfo = JsInfo.Builder.from(jsInfo).setJsMemberType(JsMemberType.CONSTRUCTOR).build();
    }
    /**
     * JDT does not provide method bindings for any bridge methods so the current one must not be a
     * bridge.
     */
    boolean isBridge = false;
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setName(isConstructor ? null : methodName)
        .setParameterDescriptors(parameterDescriptorBuilder.build())
        .setDeclarationMethodDescriptor(declarationMethodDescriptor)
        .setReturnTypeDescriptor(returnTypeDescriptor)
        .setTypeParameterTypeDescriptors(typeParameterTypeDescriptors)
        .setJsInfo(jsInfo)
        .setJsFunction(JsInteropUtils.isOrOverridesJsFunctionMethod(methodBinding))
        .setVisibility(visibility)
        .setStatic(isStatic)
        .setConstructor(isConstructor)
        .setNative(isNative)
        .setFinal(JdtUtils.isFinal(methodBinding))
        .setDefaultMethod(isDefault)
        .setAbstract(Modifier.isAbstract(methodBinding.getModifiers()))
        .setSynthetic(methodBinding.isSynthetic())
        .setBridge(isBridge)
        .setUnusableByJsSuppressed(JsInteropAnnotationUtils.isUnusableByJsSuppressed(methodBinding))
        .build();
  }

  /** Checks overriding chain to compute JsInfo. */
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

    // TODO(epmjohnston): Do the same for JsProperty?
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

    ITypeBinding javaLangObjectTypeBinding = JdtUtils.javaLangObjectTypeBinding.get();
    if (typeBinding != javaLangObjectTypeBinding) {
      for (IMethodBinding objectMethodBinding : javaLangObjectTypeBinding.getDeclaredMethods()) {
        if (!isPolymorphic(objectMethodBinding)) {
          continue;
        }
        checkState(!getVisibility(objectMethodBinding).isPackagePrivate());
        if (methodBinding.isSubsignature(objectMethodBinding)) {
          overriddenMethods.add(objectMethodBinding);
        }
      }
    }

    return overriddenMethods;
  }

  private static boolean isPolymorphic(IMethodBinding methodBinding) {
    return !methodBinding.isConstructor()
        && !isStatic(methodBinding)
        && !Modifier.isPrivate(methodBinding.getModifiers());
  }

  public static boolean isLocal(ITypeBinding typeBinding) {
    return typeBinding.isLocal();
  }

  /** Returns true if {@code typeBinding} is a class that implements a JsFunction interface. */
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

  /** Returns the MethodDescriptor for the SAM method in JsFunction interface. */
  public static MethodDescriptor getJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
    IMethodBinding samInJsFunctionInterface = getSAMInJsFunctionInterface(typeBinding);
    return samInJsFunctionInterface == null
        ? null
        : createMethodDescriptor(samInJsFunctionInterface.getMethodDeclaration());
  }

  /** Returns the MethodDescriptor for the concrete JsFunction method implementation. */
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

  /** Returns JsFunction method in JsFunction interface. */
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

  private static ImmutableList<TypeDescriptor> createTypeDescriptors(
      List<ITypeBinding> typeBindings) {
    return typeBindings.stream().map(JdtUtils::createTypeDescriptor).collect(toImmutableList());
  }

  private static ImmutableList<TypeDescriptor> createTypeDescriptors(ITypeBinding[] typeBindings) {
    return createTypeDescriptors(Arrays.asList(typeBindings));
  }

  private static ThreadLocal<ITypeBinding> javaLangObjectTypeBinding = new ThreadLocal<>();

  public static void initWellKnownTypes(AST ast, Iterable<ITypeBinding> typeBindings) {
    javaLangObjectTypeBinding.set(ast.resolveWellKnownType("java.lang.Object"));

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
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Short")))
            .addPrimitiveBoxedTypeDescriptorPair(
                createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.VOID_TYPE_NAME)),
                createTypeDescriptor(ast.resolveWellKnownType("java.lang.Void")));
    // Add well-known, non-primitive types.
    for (ITypeBinding typeBinding : typeBindings) {
      singletonInitializer.addReferenceType(createTypeDescriptor(typeBinding));
    }
    singletonInitializer.init();
  }

  public static TypeDescriptor createLambdaTypeDescriptor(
      boolean inStaticContext,
      final TypeDescriptor enclosingTypeDescriptor,
      List<String> classComponents,
      final ITypeBinding lambdaTypeBinding) {

    final List<ITypeBinding> lambdaInterfaceBindings =
        getLambdaInterfaceBindings(lambdaTypeBinding);

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

    List<TypeDescriptor> typeArgumentDescriptors = Lists.newArrayList();
    for (TypeDescriptor interfaceTypeDescriptor : lambdaInterfaceTypeDescriptors) {
      typeArgumentDescriptors.addAll(interfaceTypeDescriptor.getAllTypeVariables());
    }
    TypeDeclaration lambdaTypeDeclaration =
        JdtUtils.createLambdaTypeDeclaration(
            inStaticContext,
            enclosingTypeDescriptor.getTypeDeclaration(),
            classComponents,
            lambdaTypeBinding);

    return TypeDescriptor.newBuilder()
        .setClassComponents(classComponents)
        .setConcreteJsFunctionMethodDescriptorFactory(concreteJsFunctionMethodDescriptorFactory)
        .setDeclaredMethodDescriptorsFactory(
            // Declare the (synthetic) lambda method implemented in the lambda implementation class
            // so that passes that rely on this information (like BridgeMethodCreator) handle
            // these classes property.
            typeDescriptor ->
                getDeclaredMethodsForLambda(
                    typeDescriptor, lambdaTypeBinding.getFunctionalInterfaceMethod()))
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setNullable(true)
        .setJsFunctionMethodDescriptorFactory(jsFunctionMethodDescriptorFactory)
        .setRawTypeDescriptorFactory(
            selfTypeDescriptor ->
                TypeDescriptor.Builder.from(selfTypeDescriptor)
                    .setNullable(true)
                    .setTypeArgumentDescriptors(Collections.emptyList())
                    .build())
        .setInterfaceTypeDescriptorsFactory(() -> lambdaInterfaceTypeDescriptors)
        .setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .setTypeDeclaration(lambdaTypeDeclaration)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setKind(Kind.CLASS)
        .build();
  }

  private static List<ITypeBinding> getLambdaInterfaceBindings(ITypeBinding lambdaTypeBinding) {

    IMethodBinding functionalMethodBinding = lambdaTypeBinding.getFunctionalInterfaceMethod();
    List<ITypeBinding> lambdaInterfaceBindings =
        isIntersectionType(lambdaTypeBinding)
            ? Arrays.asList(lambdaTypeBinding.getInterfaces())
            : Collections.singletonList(lambdaTypeBinding);

    // Replace the functional interface type with the parameterization that matches the functional
    // method.
    // In JDT modeling there are cases in which the parameterization for lambdaTypeBinding and
    // the functionalMethodBinding do not match. Those cases are the ones in which JDT
    // has some freedom to choose the types e.g.
    //
    //  <T extends A> f() {
    //    Function<? super T, ?> f = i -> 0L;
    //
    // JDT will choose a type for the functional expression (this is probably determined by the JLS
    // to be the most specific type?), in this case, it is Function<T, Long>. This type will be
    // reflected as the declaring class of the functional method and is the same type that
    // the corresponding functional expression has.

    lambdaInterfaceBindings =
        lambdaInterfaceBindings
            .stream()
            .map(
                typeBinding ->
                    typeBinding.getErasure()
                            == functionalMethodBinding.getDeclaringClass().getErasure()
                        ? functionalMethodBinding.getDeclaringClass()
                        : typeBinding)
            .collect(Collectors.toList());

    if (isIntersectionType(lambdaTypeBinding)) {
      checkArgument(lambdaTypeBinding.getSuperclass() == null);
      checkState(lambdaInterfaceBindings.size() >= 2);
    }
    return lambdaInterfaceBindings;
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

  /**
   * This cache is a Hashtable so is already synchronized and safe to use from multiple threads. We
   * don't need a separate cache for each thread (like interners have) since JDT's ITypeBinding
   * instances (which we are using as keys) are unique per JDT parse.
   */
  @SuppressWarnings("JdkObsolete")
  private static Map<ITypeBinding, TypeDescriptor> cachedTypeDescriptorByTypeBinding =
      new Hashtable<>();

  // This is only used by TypeProxyUtils, and cannot be used elsewhere. Because to create a
  // TypeDescriptor from a TypeBinding, it should go through the path to check array type.
  private static TypeDescriptor createForType(final ITypeBinding typeBinding) {
    if (cachedTypeDescriptorByTypeBinding.containsKey(typeBinding)) {
      return cachedTypeDescriptorByTypeBinding.get(typeBinding);
    }

    checkArgument(!typeBinding.isArray());

    Supplier<TypeDescriptor> rawTypeDescriptorFactory = getRawTypeDescriptorSupplier(typeBinding);

    // Compute these first since they're reused in other calculations.
    boolean isTypeVariable = typeBinding.isTypeVariable();
    boolean isWildCardOrCapture = typeBinding.isWildcardType() || typeBinding.isCapture();
    boolean isNullable = !typeBinding.isPrimitive() || typeBinding.isTypeVariable();
    String uniqueKey = (isTypeVariable || isWildCardOrCapture) ? typeBinding.getKey() : null;

    Supplier<ImmutableMap<String, MethodDescriptor>> declaredMethods =
        () -> {
          ImmutableMap.Builder<String, MethodDescriptor> mapBuilder = ImmutableMap.builder();
          for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
            MethodDescriptor methodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
            mapBuilder.put(
                // TODO(b/33595109): Using the method declaration signature here is kind of iffy;
                // but needs to be done because parameterized types might make multiple
                // superinterface methods collide which are represented by JDT as different method
                // bindings but with the same signature, e.g.
                //   interface I<U, V extends Serializable> {
                //     void foo(U u);
                //     void foo(V v);
                //   }
                // When considering the type I<A,A>, there are two different method bindings
                // that describe the single method 'void foo(A a)' each with the respective
                // method declaration.
                methodDescriptor.getDeclarationMethodDescriptor().getMethodSignature(),
                methodDescriptor);
          }
          return mapBuilder.build();
        };

    Supplier<ImmutableList<FieldDescriptor>> declaredFields =
        () ->
            Arrays.stream(typeBinding.getDeclaredFields())
                .map(JdtUtils::createFieldDescriptor)
                .collect(toImmutableList());
    ;

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
    TypeDeclaration typeDeclaration = null;
    ITypeBinding declarationTypeBinding = typeBinding.getTypeDeclaration();
    if (declarationTypeBinding != null && !isTypeVariable && !isWildCardOrCapture) {
      checkArgument(
          !declarationTypeBinding.isArray() && !declarationTypeBinding.isParameterizedType());
      typeDeclaration = JdtUtils.createDeclarationForType(declarationTypeBinding);
    }

    // Compute these even later
    TypeDescriptor typeDescriptor =
        TypeDescriptor.newBuilder()
            .setBoundTypeDescriptorFactory(boundTypeDescriptorFactory)
            .setClassComponents(getClassComponents(typeBinding))
            .setConcreteJsFunctionMethodDescriptorFactory(
                () -> getConcreteJsFunctionMethodDescriptor(typeBinding))
            .setTypeDeclaration(typeDeclaration)
            .setEnclosingTypeDescriptor(
                isTypeVariable || isWildCardOrCapture
                    ? null
                    : createTypeDescriptor(typeBinding.getDeclaringClass()))
            .setInterfaceTypeDescriptorsFactory(
                () -> createTypeDescriptors(typeBinding.getInterfaces()))
            .setKind(getKindFromTypeBinding(typeBinding))
            .setNullable(isNullable)
            .setJsFunctionMethodDescriptorFactory(() -> getJsFunctionMethodDescriptor(typeBinding))
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSuperTypeDescriptorFactory(() -> createTypeDescriptor(typeBinding.getSuperclass()))
            .setTypeArgumentDescriptors(getTypeArgumentTypeDescriptors(typeBinding))
            .setDeclaredFieldDescriptorsFactory(declaredFields)
            .setDeclaredMethodDescriptorsFactory(declaredMethods)
            .setUniqueKey(uniqueKey)
            .build();
    cachedTypeDescriptorByTypeBinding.put(typeBinding, typeDescriptor);
    return typeDescriptor;
  }

  private static Supplier<TypeDescriptor> getRawTypeDescriptorSupplier(ITypeBinding typeBinding) {
    return () -> {
      TypeDescriptor rawTypeDescriptor = createTypeDescriptor(typeBinding.getErasure());
      if (rawTypeDescriptor.hasTypeArguments()) {
        checkArgument(!rawTypeDescriptor.isArray());
        checkArgument(!rawTypeDescriptor.isTypeVariable());
        checkArgument(!rawTypeDescriptor.isUnion());
        return TypeDescriptor.Builder.from(rawTypeDescriptor)
            .setTypeArgumentDescriptors(ImmutableList.of())
            .build();
      }
      return rawTypeDescriptor;
    };
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

  /**
   * Returns true for the cases where the qualifier an expression that has always the same value and
   * will not trigger class initializers.
   */
  public static boolean isEffectivelyConstant(org.eclipse.jdt.core.dom.Expression expression) {
    switch (expression.getNodeType()) {
      case ASTNode.PARENTHESIZED_EXPRESSION:
        return isEffectivelyConstant(((ParenthesizedExpression) expression).getExpression());
      case ASTNode.SIMPLE_NAME:
      case ASTNode.QUALIFIED_NAME:
        IBinding binding = ((Name) expression).resolveBinding();
        if (binding instanceof IVariableBinding) {
          IVariableBinding variableBinding = (IVariableBinding) binding;
          return !variableBinding.isField() && variableBinding.isEffectivelyFinal();
        }
        // Type expressions are always effectively constant.
        return binding instanceof ITypeBinding;
      case ASTNode.THIS_EXPRESSION:
      case ASTNode.BOOLEAN_LITERAL:
      case ASTNode.CHARACTER_LITERAL:
      case ASTNode.NULL_LITERAL:
      case ASTNode.NUMBER_LITERAL:
      case ASTNode.STRING_LITERAL:
      case ASTNode.TYPE_LITERAL:
        return true;
      default:
        return false;
    }
  }

  public static TypeDeclaration createDeclarationForType(final ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }

    checkArgument(!typeBinding.isArray());
    checkArgument(!typeBinding.isParameterizedType());
    checkArgument(!typeBinding.isTypeVariable());
    checkArgument(!typeBinding.isWildcardType());
    checkArgument(!typeBinding.isCapture());

    PackageInfoCache packageInfoCache = PackageInfoCache.get();

    ITypeBinding topLevelTypeBinding = toTopLevelTypeBinding(typeBinding);
    if (topLevelTypeBinding.isFromSource()) {
      // Let the PackageInfoCache know that this class is Source, otherwise it would have to rummage
      // around in the class path to figure it out and it might even come up with the wrong answer
      // for example if this class has also been globbed into some other library that is a
      // dependency of this one.
      PackageInfoCache.get().markAsSource(getBinaryNameFromTypeBinding(topLevelTypeBinding));
    }

    Supplier<TypeDescriptor> rawTypeDescriptorFactory = getRawTypeDescriptorSupplier(typeBinding);

    // Compute these first since they're reused in other calculations.
    String packageName =
        typeBinding.getPackage() == null ? null : typeBinding.getPackage().getName();
    boolean isAbstract = isAbstract(typeBinding);
    boolean isFinal = isFinal(typeBinding);

    Supplier<ImmutableMap<String, MethodDescriptor>> declaredMethods =
        () -> {
          ImmutableMap.Builder<String, MethodDescriptor> mapBuilder = ImmutableMap.builder();
          for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
            MethodDescriptor methodDescriptor = createMethodDescriptor(methodBinding);
            mapBuilder.put(
                // TODO(b/33595109): Using the method declaration signature here is kind of iffy;
                // but needs to be done because parameterized types might make multiple
                // superinterface methods collide which are represented by JDT as different method
                // bindings but with the same signature, e.g.
                //   interface I<U, V extends Serializable> {
                //     void foo(U u);
                //     void foo(V v);
                //   }
                // When considering the type I<A,A>, there are two different method bindings
                // that describe the single method 'void foo(A a)' each with the respective
                // method declaration.
                methodDescriptor.getDeclarationMethodDescriptor().getMethodSignature(),
                methodDescriptor);
          }
          return mapBuilder.build();
        };

    Supplier<ImmutableList<FieldDescriptor>> declaredFields =
        () ->
            Arrays.stream(typeBinding.getDeclaredFields())
                .map(JdtUtils::createFieldDescriptor)
                .collect(toImmutableList());
    ;

    // Compute these even later
    return TypeDeclaration.newBuilder()
        .setClassComponents(getClassComponents(typeBinding))
        .setEnclosingTypeDeclaration(createDeclarationForType(typeBinding.getDeclaringClass()))
        .setInterfaceTypeDescriptorsFactory(
            () -> createTypeDescriptors(typeBinding.getInterfaces()))
        .setUnsafeTypeDescriptorFactory(() -> createTypeDescriptor(typeBinding))
        .setAbstract(isAbstract)
        .setKind(getKindFromTypeBinding(typeBinding))
        .setCapturingEnclosingInstance(capturesEnclosingInstance(typeBinding))
        .setFinal(isFinal)
        .setFunctionalInterface(typeBinding.getFunctionalInterfaceMethod() != null)
        .setJsFunctionInterface(JsInteropUtils.isJsFunction(typeBinding))
        .setJsFunctionImplementation(isJsFunctionImplementation(typeBinding))
        .setJsType(JsInteropUtils.isJsType(typeBinding))
        .setNative(JsInteropUtils.isNativeType(typeBinding))
        .setAnonymous(typeBinding.isAnonymous())
        .setLocal(isLocal(typeBinding))
        .setSimpleJsName(getJsName(typeBinding))
        .setJsNamespace(getJsNamespace(typeBinding, packageInfoCache))
        .setPackageName(packageName)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setSuperTypeDescriptorFactory(() -> createTypeDescriptor(typeBinding.getSuperclass()))
        .setTypeParameterDescriptors(getTypeArgumentTypeDescriptors(typeBinding))
        .setVisibility(getVisibility(typeBinding))
        .setDeclaredMethodDescriptorsFactory(declaredMethods)
        .setDeclaredFieldDescriptorsFactory(declaredFields)
        .setUnusableByJsSuppressed(JsInteropAnnotationUtils.isUnusableByJsSuppressed(typeBinding))
        .build();
  }

  public static TypeDeclaration createLambdaTypeDeclaration(
      boolean inStaticContext,
      final TypeDeclaration enclosingTypeDeclaration,
      List<String> classComponents,
      final ITypeBinding lambdaTypeBinding) {

    final List<ITypeBinding> lambdaInterfaceBindings =
        getLambdaInterfaceBindings(lambdaTypeBinding);

    final ImmutableList<TypeDescriptor> lambdaInterfaceTypeDescriptors =
        createTypeDescriptors(lambdaInterfaceBindings);

    checkArgument(lambdaInterfaceTypeDescriptors.stream().allMatch(TypeDescriptor::isInterface));
    boolean isJsFunctionImplementation =
        lambdaInterfaceTypeDescriptors.stream().anyMatch(TypeDescriptor::isJsFunctionInterface);

    List<TypeDescriptor> typeParameterDescriptors = Lists.newArrayList();
    for (TypeDescriptor interfaceTypeDescriptor : lambdaInterfaceTypeDescriptors) {
      // Yes, copying type arguments from interfaces and making them type parameters in this
      // synthetic declaration.
      typeParameterDescriptors.addAll(interfaceTypeDescriptor.getAllTypeVariables());
    }
    return TypeDeclaration.newBuilder()
        .setClassComponents(classComponents)
        .setEnclosingTypeDeclaration(enclosingTypeDeclaration)
        .setCapturingEnclosingInstance(!inStaticContext)
        .setJsFunctionImplementation(isJsFunctionImplementation)
        .setDeclaredMethodDescriptorsFactory(
            // Declare the (synthetic) lambda method implemented in the lambda implementation class
            // so that passes that rely on this information (like BridgeMethodCreator) handle
            // these classes property.
            typeDeclaration ->
                getDeclaredMethodsForLambda(
                    typeDeclaration.getUnsafeTypeDescriptor(),
                    lambdaTypeBinding.getFunctionalInterfaceMethod()))
        .setLocal(true)
        .setAnonymous(true)
        .setRawTypeDescriptorFactory(
            selfTypeDescriptor ->
                TypeDescriptor.Builder.from(selfTypeDescriptor.getUnsafeTypeDescriptor())
                    .setNullable(true)
                    .setTypeArgumentDescriptors(Collections.emptyList())
                    .build())
        .setInterfaceTypeDescriptorsFactory(() -> lambdaInterfaceTypeDescriptors)
        .setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject)
        .setUnsafeTypeDescriptorFactory(
            () ->
                createLambdaTypeDescriptor(
                    inStaticContext,
                    enclosingTypeDeclaration.getUnsafeTypeDescriptor(),
                    classComponents,
                    lambdaTypeBinding))
        .setTypeParameterDescriptors(typeParameterDescriptors)
        .setVisibility(Visibility.PRIVATE)
        .setKind(Kind.CLASS)
        .build();
  }

  private static ImmutableMap<String, MethodDescriptor> getDeclaredMethodsForLambda(
      TypeDescriptor enclosingTypeDescriptor, IMethodBinding functionalInterfaceMethod) {
    // Declare the lambda implementation method as the only declared method in the
    // synthetic class.

    MethodDescriptor functionalMethodDescriptor =
        MethodDescriptor.Builder.from(createMethodDescriptor(functionalInterfaceMethod))
            .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
            .setDeclarationMethodDescriptor(null)
            .setAbstract(false)
            .setNative(false)
            .build();
    return ImmutableMap.of(
        functionalMethodDescriptor.getMethodSignature(), functionalMethodDescriptor);
  }
}
