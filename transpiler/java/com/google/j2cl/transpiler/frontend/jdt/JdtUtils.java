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
package com.google.j2cl.transpiler.frontend.jdt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.JsEnumInfo;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
import com.google.j2cl.transpiler.ast.Kind;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.Visibility;
import com.google.j2cl.transpiler.frontend.common.Nullability;
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

/** Utility functions to manipulate JDT internal representations. */
class JdtUtils {

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

  public static Variable createVariable(
      SourcePosition sourcePosition, IVariableBinding variableBinding) {
    String name = variableBinding.getName();
    TypeDescriptor typeDescriptor =
        variableBinding.isParameter()
            ? createTypeDescriptorWithNullability(
                variableBinding.getType(),
                variableBinding.getAnnotations(),
                /* inNullMarkedScope= */ false)
            : createTypeDescriptor(variableBinding.getType());
    boolean isFinal = variableBinding.isEffectivelyFinal();
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

  public static Expression createFieldAccess(Expression qualifier, IVariableBinding fieldBinding) {
    if (isArrayLengthBinding(fieldBinding)) {
      return ArrayLength.newBuilder().setArrayExpression(qualifier).build();
    }

    return FieldAccess.Builder.from(JdtUtils.createFieldDescriptor(fieldBinding))
        .setQualifier(qualifier)
        .build();
  }

  private static boolean isArrayLengthBinding(IVariableBinding variableBinding) {
    return variableBinding.getName().equals("length")
        && variableBinding.isField()
        && variableBinding.getDeclaringClass() == null;
  }

  /** Returns true if the binding is annotated with @UncheckedCast. */
  public static boolean hasUncheckedCastAnnotation(IBinding binding) {
    return JdtAnnotationUtils.hasAnnotation(binding, "javaemul.internal.annotations.UncheckedCast");
  }

  /** Helper method to work around JDT habit of returning raw collections. */
  @SuppressWarnings("rawtypes")
  public static <T> List<T> asTypedList(List jdtRawCollection) {
    @SuppressWarnings("unchecked")
    List<T> typedList = jdtRawCollection;
    return typedList;
  }

  /** Creates a DeclaredTypeDescriptor from a JDT TypeBinding. */
  public static DeclaredTypeDescriptor createDeclaredTypeDescriptor(ITypeBinding typeBinding) {
    return createDeclaredTypeDescriptor(typeBinding, /* inNullMarkedScope= */ false);
  }

  /**
   * Creates a DeclaredTypeDescriptor from a JDT TypeBinding applying the nullability rules
   * according to the scope this descriptor appears in.
   */
  public static DeclaredTypeDescriptor createDeclaredTypeDescriptor(
      ITypeBinding typeBinding, boolean inNullMarkedScope) {
    return createTypeDescriptor(typeBinding, inNullMarkedScope, DeclaredTypeDescriptor.class);
  }

  /** Creates a DeclaredTypeDescriptor from a JDT TypeBinding. */
  private static <T extends TypeDescriptor> T createTypeDescriptor(
      ITypeBinding typeBinding, boolean inNullMarkedScope, Class<T> clazz) {
    return clazz.cast(createTypeDescriptor(typeBinding, inNullMarkedScope));
  }

  /** Creates a TypeDescriptor from a JDT TypeBinding. */
  public static TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding) {
    return createTypeDescriptor(typeBinding, /* inNullMarkedScope= */ false);
  }

  /**
   * Creates a TypeDescriptor from a JDT TypeBinding applying the nullability rules according to the
   * scope this descriptor appears in.
   */
  public static TypeDescriptor createTypeDescriptor(
      ITypeBinding typeBinding, boolean inNullMarkedScope) {
    return createTypeDescriptorWithNullability(
        typeBinding, new IAnnotationBinding[0], inNullMarkedScope);
  }

  /** Creates a type descriptor for the given type binding, taking into account nullability. */
  private static TypeDescriptor createTypeDescriptorWithNullability(
      ITypeBinding typeBinding,
      IAnnotationBinding[] elementAnnotations,
      boolean inNullMarkedScope) {
    if (typeBinding == null) {
      return null;
    }

    if (typeBinding.isPrimitive()) {
      return PrimitiveTypes.get(typeBinding.getName());
    }

    if (isIntersectionType(typeBinding)) {
      return createIntersectionType(typeBinding);
    }

    if (typeBinding.isNullType()) {
      return TypeDescriptors.get().javaLangObject;
    }

    if (typeBinding.isTypeVariable() || typeBinding.isCapture() || typeBinding.isWildcardType()) {
      return createTypeVariable(typeBinding);
    }

    boolean isNullable = isNullable(typeBinding, elementAnnotations, inNullMarkedScope);
    if (typeBinding.isArray()) {
      TypeDescriptor componentTypeDescriptor =
          createTypeDescriptor(typeBinding.getComponentType(), inNullMarkedScope);
      return ArrayTypeDescriptor.newBuilder()
          .setComponentTypeDescriptor(componentTypeDescriptor)
          .setNullable(isNullable)
          .build();
    }

    // Propagate the context to the creation of the type, so that its type arguments are created
    // in the right context and then apply the nullability rules to the outer type.
    return withNullability(createDeclaredType(typeBinding, inNullMarkedScope), isNullable);
  }

  private static TypeDescriptor createTypeVariable(ITypeBinding typeBinding) {
    Supplier<TypeDescriptor> boundTypeDescriptorFactory = () -> getTypeBound(typeBinding);

    return TypeVariable.newBuilder()
        .setBoundTypeDescriptorSupplier(boundTypeDescriptorFactory)
        .setWildcardOrCapture(typeBinding.isWildcardType() || typeBinding.isCapture())
        .setUniqueKey(typeBinding.getKey())
        .setName(typeBinding.getName())
        .build();
  }

  private static TypeDescriptor getTypeBound(ITypeBinding typeBinding) {
    ITypeBinding[] bounds = typeBinding.getTypeBounds();
    if (bounds == null || bounds.length == 0) {
      return TypeDescriptors.get().javaLangObject;
    }
    // TODO(b/190520117): Handle nullability annotations in bounds.
    if (bounds.length == 1) {
      return createTypeDescriptor(bounds[0]);
    }
    return createIntersectionType(typeBinding);
  }

  private static DeclaredTypeDescriptor withNullability(
      DeclaredTypeDescriptor typeDescriptor, boolean nullable) {
    return nullable ? typeDescriptor.toNullable() : typeDescriptor.toNonNullable();
  }

  /**
   * Returns whether the given type binding should be nullable, according to the annotations on it
   * and if nullability is enabled for the package containing the binding.
   */
  private static boolean isNullable(
      ITypeBinding typeBinding,
      IAnnotationBinding[] elementAnnotations,
      boolean inNullMarkedScope) {
    checkArgument(!typeBinding.isPrimitive());

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
      String annotationName = annotation.getAnnotationType().getQualifiedName();

      if (Nullability.isNonNullAnnotation(annotationName)) {
        return false;
      }

      if (Nullability.isNullableAnnotation(annotationName)) {
        return true;
      }
    }

    return !inNullMarkedScope;
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
    return binding.isIntersectionType()
        // JDT returns true for isIntersectionType() for type variables, wildcards and captures
        // with intersection type bounds.
        && !binding.isCapture()
        && !binding.isTypeVariable()
        && !binding.isWildcardType();
  }

  private static List<String> getClassComponents(ITypeBinding typeBinding) {
    List<String> classComponents = new ArrayList<>();
    ITypeBinding currentType = typeBinding;
    while (currentType != null) {
      checkArgument(currentType.getTypeDeclaration() != null);
      if (currentType.isLocal()) {
        // JDT binary name for local class is like package.components.EnclosingClass$1SimpleName
        // Extract the generated name by taking the part after the binary name of the declaring
        // class.
        String binaryName = getBinaryNameFromTypeBinding(currentType);
        String declaringClassPrefix =
            getBinaryNameFromTypeBinding(currentType.getDeclaringClass()) + "$";
        checkState(binaryName.startsWith(declaringClassPrefix));
        classComponents.add(0, binaryName.substring(declaringClassPrefix.length()));
      } else {
        classComponents.add(0, currentType.getName());
      }
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

  private static List<TypeDescriptor> getTypeArgumentTypeDescriptors(
      ITypeBinding typeBinding, boolean inNullMarkedScope) {
    return getTypeArgumentTypeDescriptors(typeBinding, inNullMarkedScope, TypeDescriptor.class);
  }

  private static <T extends TypeDescriptor> List<T> getTypeArgumentTypeDescriptors(
      ITypeBinding typeBinding, boolean inNullMarkedScope, Class<T> clazz) {
    ImmutableList.Builder<T> typeArgumentDescriptorsBuilder = ImmutableList.builder();
    if (typeBinding.isParameterizedType()) {
      typeArgumentDescriptorsBuilder.addAll(
          createTypeDescriptors(typeBinding.getTypeArguments(), inNullMarkedScope, clazz));
    } else {
      typeArgumentDescriptorsBuilder.addAll(
          createTypeDescriptors(typeBinding.getTypeParameters(), inNullMarkedScope, clazz));
    }

    // DO NOT USE getDeclaringMethod(). getDeclaringMethod() returns a synthetic static method
    // in the declaring class, instead of the proper lambda method with the declaring method
    // enclosing it when the declaration is inside a lambda. If the declaring method declares a
    // type variable, it would get lost.
    IBinding declarationBinding = getDeclaringMethodOrFieldBinding(typeBinding);
    if (declarationBinding instanceof IMethodBinding) {
      typeArgumentDescriptorsBuilder.addAll(
          createTypeDescriptors(
              ((IMethodBinding) declarationBinding).getTypeParameters(), inNullMarkedScope, clazz));
    }

    if (capturesEnclosingInstance(typeBinding.getTypeDeclaration())) {
      // Find type parameters in the enclosing scope and copy them over as well.
      createDeclaredTypeDescriptor(typeBinding.getDeclaringClass(), inNullMarkedScope)
          .getTypeArgumentDescriptors()
          .stream()
          .map(clazz::cast)
          .forEach(typeArgumentDescriptorsBuilder::add);
    }

    return typeArgumentDescriptorsBuilder.build();
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

  private static boolean isDeprecated(IBinding binding) {
    return JdtAnnotationUtils.hasAnnotation(binding, Deprecated.class.getName());
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

  public static boolean isStatic(BodyDeclaration bodyDeclaration) {
    return Modifier.isStatic(bodyDeclaration.getModifiers());
  }

  private static boolean isEnumSyntheticMethod(IMethodBinding methodBinding) {
    // Enum synthetic methods are not marked as such because per JLS 13.1 these methods are
    // implicitly declared but are not marked as synthetic.
    return methodBinding.getDeclaringClass().isEnum()
        && (isEnumValuesMethod(methodBinding) || isEnumValueOfMethod(methodBinding));
  }

  private static boolean isEnumValuesMethod(IMethodBinding methodBinding) {
    return methodBinding.getName().equals("values")
        && methodBinding.getParameterTypes().length == 0;
  }

  private static boolean isEnumValueOfMethod(IMethodBinding methodBinding) {
    return methodBinding.getName().equals("valueOf")
        && methodBinding.getParameterTypes().length == 1
        && methodBinding.getParameterTypes()[0].getQualifiedName().equals("java.lang.String");
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
      return !isStatic(getDeclaringMethodOrFieldBinding(typeBinding));
    } else {
      checkArgument(typeBinding.isMember());
      // Member classes must be marked explicitly static.
      return !isStatic(typeBinding);
    }
  }

  /**
   * Returns the declaring member binding if any, skipping lambdas which are returned by JDT as
   * declaring members but are not.
   */
  private static IBinding getDeclaringMethodOrFieldBinding(ITypeBinding typeBinding) {
    IBinding declarationBinding = getDeclaringMember(typeBinding);

    // Skip all lambda method bindings.
    while (isLambdaBinding(declarationBinding)) {
      declarationBinding = ((IMethodBinding) declarationBinding).getDeclaringMember();
    }
    return declarationBinding;
  }

  private static IBinding getDeclaringMember(ITypeBinding typeBinding) {
    ITypeBinding typeDeclaration = typeBinding.getTypeDeclaration();
    IBinding declaringMember = typeDeclaration.getDeclaringMember();
    if (declaringMember == null) {
      // Work around for b/147690014, in which getDeclaringMember returns null, but there is
      // a declaring member and is returned by getDeclaringMethod.
      declaringMember = typeDeclaration.getDeclaringMethod();
    }
    return declaringMember;
  }

  private static boolean isLambdaBinding(IBinding binding) {
    return binding instanceof IMethodBinding
        && ((IMethodBinding) binding).getDeclaringMember() != null;
  }

  /** Create a FieldDescriptor directly based on the given JDT field variable binding. */
  public static FieldDescriptor createFieldDescriptor(IVariableBinding variableBinding) {
    checkArgument(!isArrayLengthBinding(variableBinding));

    boolean isStatic = isStatic(variableBinding);
    Visibility visibility = getVisibility(variableBinding);
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(variableBinding.getDeclaringClass());
    String fieldName = variableBinding.getName();

    TypeDescriptor thisTypeDescriptor =
        createTypeDescriptorWithNullability(
            variableBinding.getType(),
            variableBinding.getAnnotations(),
            enclosingTypeDescriptor.getTypeDeclaration().isNullMarked());

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
        .setDeclarationDescriptor(declarationFieldDescriptor)
        .setEnumConstant(variableBinding.isEnumConstant())
        .setUnusableByJsSuppressed(
            JsInteropAnnotationUtils.isUnusableByJsSuppressed(variableBinding))
        .setDeprecated(isDeprecated(variableBinding))
        .build();
  }

  /** Create a MethodDescriptor directly based on the given JDT method binding. */
  public static MethodDescriptor createMethodDescriptor(IMethodBinding methodBinding) {
    if (methodBinding == null) {
      return null;
    }

    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(methodBinding.getDeclaringClass());
    boolean inNullMarkedScope = enclosingTypeDescriptor.getTypeDeclaration().isNullMarked();

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
        isConstructor
            ? enclosingTypeDescriptor.toNonNullable()
            : createTypeDescriptorWithNullability(
                methodBinding.getReturnType(), methodBinding.getAnnotations(), inNullMarkedScope);

    MethodDescriptor declarationMethodDescriptor = null;
    if (methodBinding.getMethodDeclaration() != methodBinding) {
      // The declaration for methods in a lambda binding is two hops away. Since the declaration
      // binding of a declaration is itself, we get the declaration of the declaration here.
      IMethodBinding declarationBinding =
          methodBinding.getMethodDeclaration().getMethodDeclaration();
      declarationMethodDescriptor = createMethodDescriptor(declarationBinding);
    }

    // generate type parameters declared in the method.
    Iterable<TypeVariable> typeParameterTypeDescriptors =
        FluentIterable.from(methodBinding.getTypeParameters())
            .transform(JdtUtils::createTypeDescriptor)
            .transform(TypeVariable.class::cast);

    ImmutableList.Builder<ParameterDescriptor> parameterDescriptorBuilder = ImmutableList.builder();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      parameterDescriptorBuilder.add(
          ParameterDescriptor.newBuilder()
              .setTypeDescriptor(
                  createTypeDescriptorWithNullability(
                      methodBinding.getParameterTypes()[i],
                      methodBinding.getParameterAnnotations(i),
                      inNullMarkedScope))
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

    boolean hasUncheckedCast = hasUncheckedCastAnnotation(methodBinding);
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setName(isConstructor ? null : methodName)
        .setParameterDescriptors(parameterDescriptorBuilder.build())
        .setDeclarationDescriptor(declarationMethodDescriptor)
        .setReturnTypeDescriptor(returnTypeDescriptor)
        .setTypeParameterTypeDescriptors(typeParameterTypeDescriptors)
        .setJsInfo(jsInfo)
        .setWasmInfo(getWasmInfo(methodBinding))
        .setJsFunction(isOrOverridesJsFunctionMethod(methodBinding))
        .setVisibility(visibility)
        .setStatic(isStatic)
        .setConstructor(isConstructor)
        .setNative(isNative)
        .setFinal(JdtUtils.isFinal(methodBinding))
        .setDefaultMethod(isDefault)
        .setAbstract(Modifier.isAbstract(methodBinding.getModifiers()))
        .setSynthetic(methodBinding.isSynthetic())
        .setEnumSyntheticMethod(isEnumSyntheticMethod(methodBinding))
        .setUnusableByJsSuppressed(JsInteropAnnotationUtils.isUnusableByJsSuppressed(methodBinding))
        .setSideEffectFree(isAnnotatedWithHasNoSideEffects(methodBinding))
        .setDeprecated(isDeprecated(methodBinding))
        .setUncheckedCast(hasUncheckedCast)
        .build();
  }

  private static String getWasmInfo(IMethodBinding binding) {
    return JdtAnnotationUtils.getStringAttribute(
        JdtAnnotationUtils.findAnnotationBindingByName(
            binding.getAnnotations(), "javaemul.internal.annotations.Wasm"),
        "value");
  }

  private static boolean isOrOverridesJsFunctionMethod(IMethodBinding methodBinding) {
    ITypeBinding declaringType = methodBinding.getDeclaringClass();
    if (JsInteropUtils.isJsFunction(declaringType)
        && declaringType.getFunctionalInterfaceMethod() != null
        && methodBinding.getMethodDeclaration()
            == declaringType.getFunctionalInterfaceMethod().getMethodDeclaration()) {
      return true;
    }
    for (IMethodBinding overriddenMethodBinding : getOverriddenMethods(methodBinding)) {
      if (isOrOverridesJsFunctionMethod(overriddenMethodBinding)) {
        return true;
      }
    }
    return false;
  }

  /** Checks overriding chain to compute JsInfo. */
  private static JsInfo computeJsInfo(IMethodBinding methodBinding) {
    JsInfo originalJsInfo = JsInteropUtils.getJsInfo(methodBinding);

    if (originalJsInfo.isJsOverlay()
        || originalJsInfo.getJsName() != null
        || originalJsInfo.getJsNamespace() != null) {
      // Do not examine overridden methods if the method is marked as JsOverlay or it has a JsMember
      // annotation that customizes the name.
      return originalJsInfo;
    }

    boolean hasExplicitJsMemberAnnotation = hasJsMemberAnnotation(methodBinding);
    JsInfo defaultJsInfo = originalJsInfo;
    for (IMethodBinding overriddenMethod : getOverriddenMethods(methodBinding)) {
      JsInfo inheritedJsInfo = JsInteropUtils.getJsInfo(overriddenMethod);
      if (inheritedJsInfo.getJsMemberType() == JsMemberType.NONE) {
        continue;
      }

      if (hasExplicitJsMemberAnnotation
          && originalJsInfo.getJsMemberType() != inheritedJsInfo.getJsMemberType()) {
        // Only inherit from the overridden method if the JsMember types are consistent.
        continue;
      }

      if (inheritedJsInfo.getJsName() != null) {
        // Found an overridden method of the same JsMember type one that customizes the name, done.
        // If there are any conflicts with other overrides they will be reported by
        // JsInteropRestrictionsChecker.
        return JsInfo.Builder.from(inheritedJsInfo).setJsAsync(originalJsInfo.isJsAsync()).build();
      }

      if (defaultJsInfo == originalJsInfo && !hasExplicitJsMemberAnnotation) {
        // The original method does not have a JsMember annotation and traversing the list of
        // overridden methods we found the first that has an explicit JsMember annotation.
        // Keep it as the one to be used if none is found that customizes the name.
        // This allows to "inherit" the JsMember type from the override.
        defaultJsInfo = inheritedJsInfo;
      }
    }

    // Don't inherit @JsAsync annotation from overridden methods.
    return JsInfo.Builder.from(defaultJsInfo).setJsAsync(originalJsInfo.isJsAsync()).build();
  }

  private static boolean hasJsMemberAnnotation(IMethodBinding methodBinding) {
    return JsInteropAnnotationUtils.getJsMethodAnnotation(methodBinding) != null
        || JsInteropAnnotationUtils.getJsPropertyAnnotation(methodBinding) != null
        || JsInteropAnnotationUtils.getJsConstructorAnnotation(methodBinding) != null;
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

  /** Returns the MethodDescriptor for the JsFunction method. */
  private static MethodDescriptor getJsFunctionMethodDescriptor(ITypeBinding typeBinding) {
    if (JsInteropUtils.isJsFunction(typeBinding)
        && typeBinding.getFunctionalInterfaceMethod() != null) {
      // typeBinding.getFunctionalInterfaceMethod returns in some cases the method declaration
      // instead of the method with the corresponding parameterization. Note: this is observed in
      // the case when a type is parameterized with a wildcard, e.g. JsFunction<?>.
      IMethodBinding jsFunctionMethodBinding =
          Arrays.stream(typeBinding.getDeclaredMethods())
              .filter(
                  methodBinding ->
                      methodBinding.getMethodDeclaration()
                          == typeBinding.getFunctionalInterfaceMethod().getMethodDeclaration())
              .findFirst()
              .get();
      return createMethodDescriptor(jsFunctionMethodBinding).withoutTypeParameters();
    }

    // Find implementation method that corresponds to JsFunction.
    Optional<ITypeBinding> jsFunctionInterface =
        Arrays.stream(typeBinding.getInterfaces()).filter(JsInteropUtils::isJsFunction).findFirst();

    return jsFunctionInterface
        .map(ITypeBinding::getFunctionalInterfaceMethod)
        .flatMap(jsFunctionMethod -> getOverrideInType(typeBinding, jsFunctionMethod))
        .map(MethodDescriptor::withoutTypeParameters)
        .orElse(null);
  }

  private static Optional<MethodDescriptor> getOverrideInType(
      ITypeBinding typeBinding, IMethodBinding methodBinding) {
    return Arrays.stream(typeBinding.getDeclaredMethods())
        .filter(m -> m.overrides(methodBinding))
        .findFirst()
        .map(JdtUtils::createMethodDescriptor);
  }

  private static <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      List<ITypeBinding> typeBindings, boolean inNullMarkedScope, Class<T> clazz) {
    return typeBindings.stream()
        .map(typeBinding -> createTypeDescriptor(typeBinding, inNullMarkedScope, clazz))
        .collect(toImmutableList());
  }

  private static <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      ITypeBinding[] typeBindings, Class<T> clazz) {
    return createTypeDescriptors(typeBindings, /* inNullMarkedScope= */ false, clazz);
  }

  private static <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      ITypeBinding[] typeBindings, boolean inNullMarkedScope, Class<T> clazz) {
    return createTypeDescriptors(Arrays.asList(typeBindings), inNullMarkedScope, clazz);
  }

  private static ThreadLocal<ITypeBinding> javaLangObjectTypeBinding = new ThreadLocal<>();

  public static void initWellKnownTypes(AST ast, Iterable<ITypeBinding> typeBindings) {
    javaLangObjectTypeBinding.set(ast.resolveWellKnownType("java.lang.Object"));

    if (TypeDescriptors.isInitialized()) {
      return;
    }
    TypeDescriptors.SingletonBuilder builder = new TypeDescriptors.SingletonBuilder();
    for (PrimitiveTypeDescriptor typeDescriptor : PrimitiveTypes.TYPES) {
      addPrimitive(ast, builder, typeDescriptor);
    }
    // Add well-known, non-primitive types.
    for (ITypeBinding typeBinding : typeBindings) {
      builder.addReferenceType(createDeclaredTypeDescriptor(typeBinding));
    }
    builder.buildSingleton();
  }

  private static void addPrimitive(
      AST ast, TypeDescriptors.SingletonBuilder builder, PrimitiveTypeDescriptor typeDescriptor) {
    DeclaredTypeDescriptor boxedType =
        createDeclaredTypeDescriptor(ast.resolveWellKnownType(typeDescriptor.getBoxedClassName()));
    builder.addPrimitiveBoxedTypeDescriptorPair(typeDescriptor, boxedType);
  }

  private static final TypeDescriptor createIntersectionType(ITypeBinding typeBinding) {
    // Intersection types created with this method only occur in method bodies, default nullability
    // can be ignored.
    List<DeclaredTypeDescriptor> intersectedTypeDescriptors =
        createTypeDescriptors(typeBinding.getTypeBounds(), DeclaredTypeDescriptor.class);
    return IntersectionTypeDescriptor.newBuilder()
        .setIntersectionTypeDescriptors(intersectedTypeDescriptors)
        .build();
  }

  private static DeclaredTypeDescriptor createDeclaredType(
      final ITypeBinding typeBinding, boolean inNullMarkedScope) {

    DeclaredTypeDescriptor typeDescriptor = getCachedTypeDescriptor(inNullMarkedScope, typeBinding);
    if (typeDescriptor != null) {
      return typeDescriptor;
    }
    checkArgument(typeBinding.isClass() || typeBinding.isInterface() || typeBinding.isEnum());

    Supplier<ImmutableList<MethodDescriptor>> declaredMethods =
        () ->
            Arrays.stream(typeBinding.getDeclaredMethods())
                .map(JdtUtils::createMethodDescriptor)
                .collect(toImmutableList());

    Supplier<ImmutableList<FieldDescriptor>> declaredFields =
        () ->
            Arrays.stream(typeBinding.getDeclaredFields())
                .map(JdtUtils::createFieldDescriptor)
                .collect(toImmutableList());

    TypeDeclaration typeDeclaration =
        JdtUtils.createDeclarationForType(typeBinding.getTypeDeclaration());

    // Compute these even later
    typeDescriptor =
        DeclaredTypeDescriptor.newBuilder()
            .setTypeDeclaration(typeDeclaration)
            .setEnclosingTypeDescriptor(
                createDeclaredTypeDescriptor(typeBinding.getDeclaringClass()))
            .setInterfaceTypeDescriptorsFactory(
                () ->
                    createTypeDescriptors(
                        typeBinding.getInterfaces(),
                        inNullMarkedScope,
                        DeclaredTypeDescriptor.class))
            .setSingleAbstractMethodDescriptorFactory(
                () -> createMethodDescriptor(typeBinding.getFunctionalInterfaceMethod()))
            .setJsFunctionMethodDescriptorFactory(() -> getJsFunctionMethodDescriptor(typeBinding))
            .setSuperTypeDescriptorFactory(
                () -> createDeclaredTypeDescriptor(typeBinding.getSuperclass(), inNullMarkedScope))
            .setTypeArgumentDescriptors(
                getTypeArgumentTypeDescriptors(typeBinding, inNullMarkedScope))
            .setDeclaredFieldDescriptorsFactory(declaredFields)
            .setDeclaredMethodDescriptorsFactory(declaredMethods)
            .build();
    putTypeDescriptorInCache(inNullMarkedScope, typeBinding, typeDescriptor);
    return typeDescriptor;
  }

  private static final ThreadLocal<Map<ITypeBinding, DeclaredTypeDescriptor>>
      cachedDeclaredTypeDescriptorByTypeBindingInNullMarkedScope =
          ThreadLocal.withInitial(HashMap::new);

  private static final ThreadLocal<Map<ITypeBinding, DeclaredTypeDescriptor>>
      cachedDeclaredTypeDescriptorByTypeBindingOutOfNullMarkedScope =
          ThreadLocal.withInitial(HashMap::new);

  private static DeclaredTypeDescriptor getCachedTypeDescriptor(
      boolean inNullMarkedScope, ITypeBinding typeBinding) {
    Map<ITypeBinding, DeclaredTypeDescriptor> cache =
        inNullMarkedScope
            ? cachedDeclaredTypeDescriptorByTypeBindingInNullMarkedScope.get()
            : cachedDeclaredTypeDescriptorByTypeBindingOutOfNullMarkedScope.get();
    return cache.get(typeBinding);
  }

  private static void putTypeDescriptorInCache(
      boolean inNullMarkedScope, ITypeBinding typeBinding, DeclaredTypeDescriptor typeDescriptor) {
    Map<ITypeBinding, DeclaredTypeDescriptor> cache =
        inNullMarkedScope
            ? cachedDeclaredTypeDescriptorByTypeBindingInNullMarkedScope.get()
            : cachedDeclaredTypeDescriptorByTypeBindingOutOfNullMarkedScope.get();
    cache.put(typeBinding, typeDescriptor);
  }

  private static Kind getKindFromTypeBinding(ITypeBinding typeBinding) {
    if (typeBinding.isEnum() && !typeBinding.isAnonymous()) {
      // Do not consider the anonymous classes that constitute enum values as Enums, only the
      // enum "class" itself is considered Kind.ENUM.
      return Kind.ENUM;
    } else if (typeBinding.isClass() || (typeBinding.isEnum() && typeBinding.isAnonymous())) {
      return Kind.CLASS;
    } else if (typeBinding.isInterface()) {
      return Kind.INTERFACE;
    }
    throw new InternalCompilerError("Type binding %s not handled", typeBinding);
  }

  private static String getJsName(final ITypeBinding typeBinding) {
    checkArgument(!typeBinding.isPrimitive());
    return JsInteropAnnotationUtils.getJsName(typeBinding);
  }

  private static String getJsNamespace(
      ITypeBinding typeBinding, PackageInfoCache packageInfoCache) {
    checkArgument(!typeBinding.isPrimitive());
    String jsNamespace = JsInteropAnnotationUtils.getJsNamespace(typeBinding);
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

  public static TypeDeclaration createDeclarationForType(final ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }

    checkArgument(typeBinding.getTypeDeclaration() == typeBinding);
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

    // Compute these first since they're reused in other calculations.
    String packageName =
        typeBinding.getPackage() == null ? null : typeBinding.getPackage().getName();
    boolean isAbstract = isAbstract(typeBinding);
    boolean isFinal = isFinal(typeBinding);

    Supplier<ImmutableList<MethodDescriptor>> declaredMethods =
        () ->
            Arrays.stream(typeBinding.getDeclaredMethods())
                .map(JdtUtils::createMethodDescriptor)
                .collect(toImmutableList());
    ;

    Supplier<ImmutableList<FieldDescriptor>> declaredFields =
        () ->
            Arrays.stream(typeBinding.getDeclaredFields())
                .map(JdtUtils::createFieldDescriptor)
                .collect(toImmutableList());

    JsEnumInfo jsEnumInfo = JsInteropUtils.getJsEnumInfo(typeBinding);

    boolean isNullMarked = isNullMarked(typeBinding, packageInfoCache);
    IBinding declaringMemberBinding = getDeclaringMethodOrFieldBinding(typeBinding);

    return TypeDeclaration.newBuilder()
        .setClassComponents(getClassComponents(typeBinding))
        .setEnclosingTypeDeclaration(createDeclarationForType(typeBinding.getDeclaringClass()))
        .setEnclosingMethodDescriptorFactory(
            () ->
                createMethodDescriptor(
                    declaringMemberBinding instanceof IMethodBinding
                        ? (IMethodBinding) declaringMemberBinding
                        : null))
        .setInterfaceTypeDescriptorsFactory(
            () ->
                createTypeDescriptors(
                    typeBinding.getInterfaces(), isNullMarked, DeclaredTypeDescriptor.class))
        .setUnparameterizedTypeDescriptorFactory(() -> createDeclaredTypeDescriptor(typeBinding))
        .setHasAbstractModifier(isAbstract)
        .setKind(getKindFromTypeBinding(typeBinding))
        .setCapturingEnclosingInstance(capturesEnclosingInstance(typeBinding))
        .setFinal(isFinal)
        .setFunctionalInterface(
            !typeBinding.isAnnotation() && typeBinding.getFunctionalInterfaceMethod() != null)
        .setJsFunctionInterface(JsInteropUtils.isJsFunction(typeBinding))
        .setAnnotatedWithFunctionalInterface(isAnnotatedWithFunctionalInterface(typeBinding))
        .setAnnotatedWithAutoValue(isAnnotatedWithAutoValue(typeBinding))
        .setAnnotatedWithAutoValueBuilder(isAnnotatedWithAutoValueBuilder(typeBinding))
        .setJsType(JsInteropUtils.isJsType(typeBinding))
        .setJsEnumInfo(jsEnumInfo)
        .setNative(JsInteropUtils.isJsNativeType(typeBinding))
        .setAnonymous(typeBinding.isAnonymous())
        .setLocal(isLocal(typeBinding))
        .setSimpleJsName(getJsName(typeBinding))
        .setCustomizedJsNamespace(getJsNamespace(typeBinding, packageInfoCache))
        .setKtTypeInfo(KtInteropUtils.getKtTypeInfo(typeBinding))
        .setNullMarked(isNullMarked)
        .setPackageName(packageName)
        .setSuperTypeDescriptorFactory(
            () -> createDeclaredTypeDescriptor(typeBinding.getSuperclass(), isNullMarked))
        .setTypeParameterDescriptors(
            getTypeArgumentTypeDescriptors(
                typeBinding, /* inNullMarkedScope= */ false, TypeVariable.class))
        .setVisibility(getVisibility(typeBinding))
        .setDeclaredMethodDescriptorsFactory(declaredMethods)
        .setDeclaredFieldDescriptorsFactory(declaredFields)
        .setUnusableByJsSuppressed(JsInteropAnnotationUtils.isUnusableByJsSuppressed(typeBinding))
        .setDeprecated(isDeprecated(typeBinding))
        .build();
  }

  private static boolean isNullMarked(ITypeBinding typeBinding, PackageInfoCache packageInfoCache) {
    return hasNullMarkedAnnotation(typeBinding)
        || packageInfoCache.isNullMarked(
            getBinaryNameFromTypeBinding(toTopLevelTypeBinding(typeBinding)));
  }

  /**
   * Returns true if {@code typeBinding} or one of its enclosing types has a @NullMarked annotation.
   */
  private static boolean hasNullMarkedAnnotation(ITypeBinding typeBinding) {
    if (JdtAnnotationUtils.hasAnnotation(
        typeBinding, Nullability.ORG_JSPECIFY_NULLNESS_NULL_MAKED)) {
      return true;
    }
    return typeBinding.getDeclaringClass() != null
        && hasNullMarkedAnnotation(typeBinding.getDeclaringClass());
  }

  private static boolean isAnnotatedWithFunctionalInterface(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.hasAnnotation(typeBinding, FunctionalInterface.class.getName());
  }

  private static boolean isAnnotatedWithAutoValue(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.hasAnnotation(typeBinding, "com.google.auto.value.AutoValue");
  }

  private static boolean isAnnotatedWithAutoValueBuilder(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.hasAnnotation(typeBinding, "com.google.auto.value.AutoValue.Builder");
  }

  private static boolean isAnnotatedWithHasNoSideEffects(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.hasAnnotation(
        methodBinding, "javaemul.internal.annotations.HasNoSideEffects");
  }

  private JdtUtils() {}
}
