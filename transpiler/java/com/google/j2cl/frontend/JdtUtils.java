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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtilConstants;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.IntersectionTypeDescriptor;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsMemberType;
import com.google.j2cl.ast.Kind;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.PrimitiveTypeDescriptor;
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
class JdtUtils {
  // JdtUtil members are all package private. Code outside frontend should not be aware of the
  // dependency on JDT.
  public static String getCompilationUnitPackageName(CompilationUnit compilationUnit) {
    return compilationUnit.getPackage() == null
        ? ""
        : compilationUnit.getPackage().getName().getFullyQualifiedName();
  }

  public static FieldDescriptor createFieldDescriptor(IVariableBinding variableBinding) {
    if (isArrayLengthBinding(variableBinding)) {
      return AstUtilConstants.getArrayLengthFieldDescriptor();
    }

    boolean isStatic = isStatic(variableBinding);
    Visibility visibility = getVisibility(variableBinding);
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(variableBinding.getDeclaringClass());
    String fieldName = variableBinding.getName();

    TypeDescriptor thisTypeDescriptor =
        createTypeDescriptorWithNullability(
            variableBinding.getType(), variableBinding.getAnnotations());

    if (variableBinding.isEnumConstant()) {
      // Enum fields are always non-nullable.
      thisTypeDescriptor = thisTypeDescriptor.toNonNullable();
    }

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

  public static Variable createVariable(
      SourcePosition sourcePosition, IVariableBinding variableBinding) {
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

  public static IMethodBinding getMethodBinding(
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

  public static boolean isStatic(BodyDeclaration bodyDeclaration) {
    return Modifier.isStatic(bodyDeclaration.getModifiers());
  }

  public static boolean isArrayLengthBinding(IVariableBinding variableBinding) {
    return variableBinding.getName().equals("length")
        && variableBinding.isField()
        && variableBinding.getDeclaringClass() == null;
  }

  /** Returns true if the binding is annotated with @UncheckedCast. */
  public static boolean hasUncheckedCastAnnotation(IBinding binding) {
    return JdtAnnotationUtils.hasAnnotation(binding, "javaemul.internal.annotations.UncheckedCast");
  }

  /**
   * Returns whether {@code subTypeBinding} is a subtype of {@code superTypeBinding} either because
   * subTypeBinding is a child class of class superTypeBinding or because subTypeBinding implements
   * interface superTypeBinding. As 'subtype' is transitive and reflective, a type is subtype of
   * itself.
   */
  public static boolean isSubType(ITypeBinding subTypeBinding, ITypeBinding superTypeBinding) {
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

  public static boolean areSameErasedType(ITypeBinding typeBinding, ITypeBinding otherTypeBinding) {
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

  /** Creates a DeclaredTypeDescriptor from a JDT TypeBinding. */
  public static DeclaredTypeDescriptor createDeclaredTypeDescriptor(ITypeBinding typeBinding) {
    return createTypeDescriptor(typeBinding, DeclaredTypeDescriptor.class);
  }

  /** Creates a DeclaredTypeDescriptor from a JDT TypeBinding. */
  private static <T extends TypeDescriptor> T createTypeDescriptor(
      ITypeBinding typeBinding, Class<T> clazz) {
    return clazz.cast(createTypeDescriptor(typeBinding));
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
    if (typeBinding.isPrimitive()) {
      return PrimitiveTypeDescriptor.newBuilder()
          .setSimpleSourceName(typeBinding.getName())
          .build();
    }

    if (isIntersectionType(typeBinding)) {
      return createIntersectionType(typeBinding);
    }

    if (typeBinding.isNullType()) {
      return TypeDescriptors.get().javaLangObject;
    }

    boolean isNullable = isNullable(typeBinding, elementAnnotations);
    if (typeBinding.isArray()) {
      TypeDescriptor componentTypeDescriptor = createTypeDescriptor(typeBinding.getComponentType());
      return ArrayTypeDescriptor.newBuilder()
          .setComponentTypeDescriptor(componentTypeDescriptor)
          .setNullable(isNullable)
          .build();
    }

    return TypeDescriptors.withNullability(createForType(typeBinding), isNullable);
  }

  /**
   * Returns whether the given type binding should be nullable, according to the annotations on it
   * and if nullability is enabled for the package containing the binding.
   */
  private static boolean isNullable(
      ITypeBinding typeBinding, IAnnotationBinding[] elementAnnotations) {
    checkArgument(!typeBinding.isPrimitive());

    if (typeBinding.isTypeVariable()) {
      return true;
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
   * In case the given type binding is nested, return the outermost possible enclosing type binding.
   */
  private static ITypeBinding toTopLevelTypeBinding(ITypeBinding typeBinding) {
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

  private static List<String> getClassComponents(ITypeBinding typeBinding) {
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

  private static List<TypeDescriptor> getTypeArgumentTypeDescriptors(ITypeBinding typeBinding) {
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
            createDeclaredTypeDescriptor(typeBinding.getDeclaringClass())
                .getTypeArgumentDescriptors());
      }
    }

    return typeArgumentDescriptors;
  }

  public static Visibility getVisibility(IBinding binding) {
    return getVisibility(binding.getModifiers());
  }

  private static Visibility getVisibility(int modifiers) {
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

  private static boolean isDefaultMethod(IMethodBinding binding) {
    return Modifier.isDefault(binding.getModifiers());
  }

  private static boolean isAbstract(IBinding binding) {
    return Modifier.isAbstract(binding.getModifiers());
  }

  private static boolean isFinal(IBinding binding) {
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
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(methodBinding.getDeclaringClass());

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

    if (enclosingTypeDescriptor.getTypeDeclaration().isAnonymous()
        && isConstructor
        && enclosingTypeDescriptor.getSuperTypeDescriptor().hasJsConstructor()) {
      jsInfo = JsInfo.Builder.from(jsInfo).setJsMemberType(JsMemberType.CONSTRUCTOR).build();
    }
    // JDT does not provide method bindings for any bridge methods so the current one must not be a
    // bridge.
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
  private static JsInfo computeJsInfo(IMethodBinding methodBinding) {
    JsInfo originalJsInfo = JsInteropUtils.getJsInfo(methodBinding);
    if (originalJsInfo.isJsOverlay()) {
      return originalJsInfo;
    }

    List<JsInfo> inheritedJsInfoList = new ArrayList<>();

    // Add the JsInfo of the method and all the overridden methods to the list.
    if (originalJsInfo.getJsMemberType() != JsMemberType.NONE) {
      inheritedJsInfoList.add(originalJsInfo);
    }
    for (IMethodBinding overriddenMethod : getOverriddenMethods(methodBinding)) {
      JsInfo inheritedJsInfo = JsInteropUtils.getJsInfo(overriddenMethod);
      if (inheritedJsInfo.getJsMemberType() != JsMemberType.NONE) {
        inheritedJsInfoList.add(inheritedJsInfo);
      }
    }

    if (inheritedJsInfoList.isEmpty()) {
      return originalJsInfo;
    }

    // TODO(b/67778330): Make the handling of @JsProperty consistent with the handling of @JsMethod.
    if (inheritedJsInfoList.get(0).getJsMemberType() == JsMemberType.METHOD) {
      // Return the first JsInfo with a Js name specified.
      for (JsInfo inheritedJsInfo : inheritedJsInfoList) {
        if (inheritedJsInfo.getJsName() != null) {
          // Don't inherit @JsAsync annotation from overridden methods.
          return JsInfo.Builder.from(inheritedJsInfo)
              .setJsAsync(originalJsInfo.isJsAsync())
              .build();
        }
      }
    }

    // Don't inherit @JsAsync annotation from overridden methods.
    return JsInfo.Builder.from(inheritedJsInfoList.get(0))
        .setJsAsync(originalJsInfo.isJsAsync())
        .build();
  }

  public static Set<IMethodBinding> getOverriddenMethods(IMethodBinding methodBinding) {
    return getOverriddenMethodsInType(methodBinding, methodBinding.getDeclaringClass());
  }

  private static Set<IMethodBinding> getOverriddenMethodsInType(
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

  private static boolean isLocal(ITypeBinding typeBinding) {
    return typeBinding.isLocal();
  }

  /** Returns true if {@code typeBinding} is a class that implements a JsFunction interface. */
  private static boolean isJsFunctionImplementation(ITypeBinding typeBinding) {
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

  /** Returns the MethodDescriptor for the concrete JsFunction method implementation. */
  private static MethodDescriptor getJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
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

  private static <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      List<ITypeBinding> typeBindings, Class<T> clazz) {
    return typeBindings
        .stream()
        .map(typeBinding -> createTypeDescriptor(typeBinding, clazz))
        .collect(toImmutableList());
  }

  private static ImmutableList<TypeDescriptor> createTypeDescriptors(ITypeBinding[] typeBindings) {
    return createTypeDescriptors(Arrays.asList(typeBindings));
  }

  private static <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      ITypeBinding[] typeBindings, Class<T> clazz) {
    return createTypeDescriptors(Arrays.asList(typeBindings), clazz);
  }

  private static ThreadLocal<ITypeBinding> javaLangObjectTypeBinding = new ThreadLocal<>();

  public static void initWellKnownTypes(AST ast, Iterable<ITypeBinding> typeBindings) {
    javaLangObjectTypeBinding.set(ast.resolveWellKnownType("java.lang.Object"));

    if (TypeDescriptors.isInitialized()) {
      return;
    }
    SingletonInitializer singletonInitializer =
        new SingletonInitializer()
            // Add primitive boxed types.
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(
                        ast.resolveWellKnownType(TypeDescriptors.BOOLEAN_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Boolean")))
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.BYTE_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Byte")))
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.CHAR_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Character")))
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(
                        ast.resolveWellKnownType(TypeDescriptors.DOUBLE_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Double")))
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.FLOAT_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Float")))
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.INT_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Integer")))
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.LONG_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Long")))
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.SHORT_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Short")))
            .addPrimitiveBoxedTypeDescriptorPair(
                (PrimitiveTypeDescriptor)
                    createTypeDescriptor(ast.resolveWellKnownType(TypeDescriptors.VOID_TYPE_NAME)),
                createDeclaredTypeDescriptor(ast.resolveWellKnownType("java.lang.Void")));
    // Add well-known, non-primitive types.
    for (ITypeBinding typeBinding : typeBindings) {
      singletonInitializer.addReferenceType(createDeclaredTypeDescriptor(typeBinding));
    }
    singletonInitializer.init();
  }

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
      bindings.add(0, binding.getSuperclass());
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
    List<DeclaredTypeDescriptor> intersectedTypeDescriptors =
        createTypeDescriptors(intersectedTypeBindings, DeclaredTypeDescriptor.class);
    return IntersectionTypeDescriptor.newBuilder()
        .setIntersectionTypeDescriptors(intersectedTypeDescriptors)
        .build();
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
    checkArgument(!typeBinding.isPrimitive());

    Supplier<DeclaredTypeDescriptor> rawTypeDescriptorFactory =
        getRawTypeDescriptorSupplier(typeBinding);

    // Compute these first since they're reused in other calculations.
    boolean isTypeVariable = typeBinding.isTypeVariable();
    boolean isWildCardOrCapture = typeBinding.isWildcardType() || typeBinding.isCapture();
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
                methodDescriptor.getDeclarationDescriptor().getMethodSignature(), methodDescriptor);
          }
          return mapBuilder.build();
        };

    Supplier<ImmutableList<FieldDescriptor>> declaredFields =
        () ->
            Arrays.stream(typeBinding.getDeclaredFields())
                .map(JdtUtils::createFieldDescriptor)
                .collect(toImmutableList());

    Supplier<TypeDescriptor> boundTypeDescriptorFactory =
        () -> {
          if (!isTypeVariable && !isWildCardOrCapture) {
            return null;
          }
          // TODO(b/67858399): should be using typeBinding.getTypeBounds() but it returns empty in
          // the current version of JDT.

          List<ITypeBinding> bounds = Lists.newArrayList(typeBinding.getInterfaces());
          if (typeBinding.getSuperclass() != JdtUtils.javaLangObjectTypeBinding.get()) {
            bounds.add(0, typeBinding.getSuperclass());
          }
          if (bounds.isEmpty()) {
            return TypeDescriptors.get().javaLangObject;
          }
          if (bounds.size() == 1) {
            return createTypeDescriptor(bounds.get(0));
          }
          return IntersectionTypeDescriptor.newBuilder()
              .setIntersectionTypeDescriptors(
                  createTypeDescriptors(bounds, DeclaredTypeDescriptor.class))
              .build();
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
        DeclaredTypeDescriptor.newBuilder()
            .setBoundTypeDescriptorFactory(boundTypeDescriptorFactory)
            .setClassComponents(getClassComponents(typeBinding))
            .setTypeDeclaration(typeDeclaration)
            .setEnclosingTypeDescriptor(
                isTypeVariable || isWildCardOrCapture
                    ? null
                    : createDeclaredTypeDescriptor(typeBinding.getDeclaringClass()))
            .setInterfaceTypeDescriptorsFactory(
                () ->
                    createTypeDescriptors(
                        typeBinding.getInterfaces(), DeclaredTypeDescriptor.class))
            .setKind(getKindFromTypeBinding(typeBinding))
            .setSingleAbstractMethodDescriptorFactory(
                () -> createMethodDescriptor(typeBinding.getFunctionalInterfaceMethod()))
            .setJsFunctionMethodDescriptorFactory(() -> getJsFunctionMethodDescriptor(typeBinding))
            .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
            .setSuperTypeDescriptorFactory(
                () -> createDeclaredTypeDescriptor(typeBinding.getSuperclass()))
            .setTypeArgumentDescriptors(getTypeArgumentTypeDescriptors(typeBinding))
            .setDeclaredFieldDescriptorsFactory(declaredFields)
            .setDeclaredMethodDescriptorsFactory(declaredMethods)
            .setUniqueKey(uniqueKey)
            .build();
    cachedTypeDescriptorByTypeBinding.put(typeBinding, typeDescriptor);
    return typeDescriptor;
  }

  private static Supplier<DeclaredTypeDescriptor> getRawTypeDescriptorSupplier(
      ITypeBinding typeBinding) {
    return () -> {
      DeclaredTypeDescriptor rawTypeDescriptor =
          createDeclaredTypeDescriptor(typeBinding.getErasure());
      if (rawTypeDescriptor.hasTypeArguments()) {
        checkArgument(!rawTypeDescriptor.isArray());
        checkArgument(!rawTypeDescriptor.isTypeVariable());
        checkArgument(!rawTypeDescriptor.isUnion());
        return DeclaredTypeDescriptor.Builder.from(rawTypeDescriptor)
            .setTypeArgumentDescriptors(ImmutableList.of())
            .build();
      }
      return rawTypeDescriptor;
    };
  }

  private static Kind getKindFromTypeBinding(ITypeBinding typeBinding) {
    checkArgument(!typeBinding.isArray());
    checkArgument(!typeBinding.isPrimitive());
    // First look whether it is a type variable, a wildcard or capture to avoid mislabeling the
    // the kind. TypeVariables that are bound to Enum respond true to ITypeBinding.isEnum().
    if (typeBinding.isTypeVariable()) {
      return Kind.TYPE_VARIABLE;
    } else if (typeBinding.isWildcardType() || typeBinding.isCapture()) {
      return Kind.WILDCARD_OR_CAPTURE;
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
    checkArgument(!typeBinding.isPrimitive());
    return JsInteropAnnotationUtils.getJsName(
        JsInteropAnnotationUtils.getJsTypeAnnotation(typeBinding));
  }

  private static String getJsNamespace(
      ITypeBinding typeBinding, PackageInfoCache packageInfoCache) {
    checkArgument(!typeBinding.isPrimitive());
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

    Supplier<DeclaredTypeDescriptor> rawTypeDescriptorFactory =
        getRawTypeDescriptorSupplier(typeBinding);

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
                methodDescriptor.getDeclarationDescriptor().getMethodSignature(), methodDescriptor);
          }
          return mapBuilder.build();
        };

    Supplier<ImmutableList<FieldDescriptor>> declaredFields =
        () ->
            Arrays.stream(typeBinding.getDeclaredFields())
                .map(JdtUtils::createFieldDescriptor)
                .collect(toImmutableList());

    // Compute these even later
    return TypeDeclaration.newBuilder()
        .setClassComponents(getClassComponents(typeBinding))
        .setEnclosingTypeDeclaration(createDeclarationForType(typeBinding.getDeclaringClass()))
        .setInterfaceTypeDescriptorsFactory(
            () -> createTypeDescriptors(typeBinding.getInterfaces(), DeclaredTypeDescriptor.class))
        .setUnparameterizedTypeDescriptorFactory(() -> createDeclaredTypeDescriptor(typeBinding))
        .setHasAbstractModifier(isAbstract)
        .setKind(getKindFromTypeBinding(typeBinding))
        .setCapturingEnclosingInstance(capturesEnclosingInstance(typeBinding))
        .setFinal(isFinal)
        .setFunctionalInterface(typeBinding.getFunctionalInterfaceMethod() != null)
        .setJsFunctionInterface(JsInteropUtils.isJsFunction(typeBinding))
        .setJsType(JsInteropUtils.isJsType(typeBinding))
        .setNative(JsInteropUtils.isNativeType(typeBinding))
        .setAnonymous(typeBinding.isAnonymous())
        .setLocal(isLocal(typeBinding))
        .setSimpleJsName(getJsName(typeBinding))
        .setJsNamespace(getJsNamespace(typeBinding, packageInfoCache))
        .setPackageName(packageName)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setSuperTypeDescriptorFactory(
            () -> createDeclaredTypeDescriptor(typeBinding.getSuperclass()))
        .setTypeParameterDescriptors((Iterable) getTypeArgumentTypeDescriptors(typeBinding))
        .setVisibility(getVisibility(typeBinding))
        .setDeclaredMethodDescriptorsFactory(declaredMethods)
        .setDeclaredFieldDescriptorsFactory(declaredFields)
        .setUnusableByJsSuppressed(JsInteropAnnotationUtils.isUnusableByJsSuppressed(typeBinding))
        .build();
  }

  private JdtUtils() {}
}
