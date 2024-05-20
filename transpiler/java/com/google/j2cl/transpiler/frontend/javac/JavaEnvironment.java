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
package com.google.j2cl.transpiler.frontend.javac;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.HAS_NO_SIDE_EFFECTS_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.UNCHECKED_CAST_ANNOTATION_NAME;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.JsEnumInfo;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.Visibility;
import com.google.j2cl.transpiler.frontend.common.Nullability;
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.CapturedType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.IntersectionClassType;
import com.sun.tools.javac.code.Type.JCPrimitiveType;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntryKind;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/** Utility functions to interact with JavaC internal representations. */
class JavaEnvironment {
  JavacTypes javacTypes;
  Types internalTypes;
  JavacElements elements;

  JavaEnvironment(Context context, Collection<String> wellKnownQualifiedBinaryNames) {
    this.javacTypes = JavacTypes.instance(context);
    this.internalTypes = Types.instance(context);
    this.elements = JavacElements.instance(context);

    initWellKnownTypes(wellKnownQualifiedBinaryNames);
  }

  private void initWellKnownTypes(Collection<String> wellKnownQualifiedBinaryNames) {
    if (TypeDescriptors.isInitialized()) {
      return;
    }
    TypeDescriptors.SingletonBuilder builder = new TypeDescriptors.SingletonBuilder();
    // Add well-known, non-primitive types.
    wellKnownQualifiedBinaryNames.forEach(
        binaryName -> {
          String qualifiedSourceName = binaryName.replace('$', '.');
          TypeElement element = getTypeElement(qualifiedSourceName);
          if (element != null) {
            builder.addReferenceType(createDeclaredTypeDescriptor(element.asType()));
          }
        });
    builder.buildSingleton();
  }

  @Nullable
  static PrefixOperator getPrefixOperator(com.sun.source.tree.Tree.Kind operator) {
    switch (operator) {
      case PREFIX_INCREMENT:
        return PrefixOperator.INCREMENT;
      case PREFIX_DECREMENT:
        return PrefixOperator.DECREMENT;
      case UNARY_PLUS:
        return PrefixOperator.PLUS;
      case UNARY_MINUS:
        return PrefixOperator.MINUS;
      case BITWISE_COMPLEMENT:
        return PrefixOperator.COMPLEMENT;
      case LOGICAL_COMPLEMENT:
        return PrefixOperator.NOT;
      default:
        return null;
    }
  }

  @Nullable
  static PostfixOperator getPostfixOperator(com.sun.source.tree.Tree.Kind operator) {
    switch (operator) {
      case POSTFIX_INCREMENT:
        return PostfixOperator.INCREMENT;
      case POSTFIX_DECREMENT:
        return PostfixOperator.DECREMENT;
      default:
        return null;
    }
  }

  @Nullable
  static BinaryOperator getBinaryOperator(com.sun.source.tree.Tree.Kind operator) {
    switch (operator) {
      case ASSIGNMENT:
        return BinaryOperator.ASSIGN;
      case PLUS_ASSIGNMENT:
        return BinaryOperator.PLUS_ASSIGN;
      case MINUS_ASSIGNMENT:
        return BinaryOperator.MINUS_ASSIGN;
      case MULTIPLY_ASSIGNMENT:
        return BinaryOperator.TIMES_ASSIGN;
      case DIVIDE_ASSIGNMENT:
        return BinaryOperator.DIVIDE_ASSIGN;
      case AND_ASSIGNMENT:
        return BinaryOperator.BIT_AND_ASSIGN;
      case OR_ASSIGNMENT:
        return BinaryOperator.BIT_OR_ASSIGN;
      case XOR_ASSIGNMENT:
        return BinaryOperator.BIT_XOR_ASSIGN;
      case REMAINDER_ASSIGNMENT:
        return BinaryOperator.REMAINDER_ASSIGN;
      case LEFT_SHIFT_ASSIGNMENT:
        return BinaryOperator.LEFT_SHIFT_ASSIGN;
      case RIGHT_SHIFT_ASSIGNMENT:
        return BinaryOperator.RIGHT_SHIFT_SIGNED_ASSIGN;
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        return BinaryOperator.RIGHT_SHIFT_UNSIGNED_ASSIGN;
      case AND:
        return BinaryOperator.BIT_AND;
      case CONDITIONAL_AND:
        return BinaryOperator.CONDITIONAL_AND;
      case CONDITIONAL_OR:
        return BinaryOperator.CONDITIONAL_OR;
      case DIVIDE:
        return BinaryOperator.DIVIDE;
      case EQUAL_TO:
        return BinaryOperator.EQUALS;
      case GREATER_THAN:
        return BinaryOperator.GREATER;
      case GREATER_THAN_EQUAL:
        return BinaryOperator.GREATER_EQUALS;
      case LEFT_SHIFT:
        return BinaryOperator.LEFT_SHIFT;
      case LESS_THAN:
        return BinaryOperator.LESS;
      case LESS_THAN_EQUAL:
        return BinaryOperator.LESS_EQUALS;
      case MINUS:
        return BinaryOperator.MINUS;
      case MULTIPLY:
        return BinaryOperator.TIMES;
      case NOT_EQUAL_TO:
        return BinaryOperator.NOT_EQUALS;
      case OR:
        return BinaryOperator.BIT_OR;
      case PLUS:
        return BinaryOperator.PLUS;
      case REMAINDER:
        return BinaryOperator.REMAINDER;
      case RIGHT_SHIFT:
        return BinaryOperator.RIGHT_SHIFT_SIGNED;
      case UNSIGNED_RIGHT_SHIFT:
        return BinaryOperator.RIGHT_SHIFT_UNSIGNED;
      case XOR:
        return BinaryOperator.BIT_XOR;
      default:
        return null;
    }
  }

  Variable createVariable(
      SourcePosition sourcePosition, VariableElement variableElement, boolean isParameter) {
    TypeMirror type = variableElement.asType();
    String name = variableElement.getSimpleName().toString();
    TypeDescriptor typeDescriptor =
        isParameter
            ? createTypeDescriptorWithNullability(
                type, variableElement.getAnnotationMirrors(), /* inNullMarkedScope= */ false)
            : createTypeDescriptor(type);
    boolean isFinal = isFinal(variableElement);
    boolean isUnusableByJsSuppressed =
        JsInteropAnnotationUtils.isUnusableByJsSuppressed(variableElement);
    return Variable.newBuilder()
        .setName(name)
        .setTypeDescriptor(typeDescriptor)
        .setFinal(isFinal)
        .setParameter(isParameter)
        .setUnusableByJsSuppressed(isUnusableByJsSuppressed)
        .setSourcePosition(sourcePosition)
        .build();
  }

  DeclaredTypeDescriptor createDeclaredTypeDescriptor(TypeMirror typeMirror) {
    return createDeclaredTypeDescriptor(typeMirror, /* inNullMarkedScope= */ false);
  }

  DeclaredTypeDescriptor createDeclaredTypeDescriptor(
      TypeMirror typeMirror, boolean inNullMarkedScope) {
    return createTypeDescriptor(typeMirror, inNullMarkedScope, DeclaredTypeDescriptor.class);
  }

  /** Creates a specific subclass of TypeDescriptor from a TypeMirror. */
  <T extends TypeDescriptor> T createTypeDescriptor(TypeMirror typeMirror, Class<T> clazz) {
    return createTypeDescriptor(typeMirror, /* inNullMarkedScope= */ false, clazz);
  }

  /** Creates a specific subclass of TypeDescriptor from a TypeMirror. */
  <T extends TypeDescriptor> T createTypeDescriptor(
      TypeMirror typeMirror, boolean inNullMarkedScope, Class<T> clazz) {
    return clazz.cast(createTypeDescriptor(typeMirror, inNullMarkedScope));
  }

  /** Creates a TypeDescriptor from a TypeMirror. */
  TypeDescriptor createTypeDescriptor(TypeMirror typeMirror) {
    return createTypeDescriptor(typeMirror, /* inNullMarkedScope= */ false);
  }

  /** Creates a TypeDescriptor from a TypeMirror. */
  TypeDescriptor createTypeDescriptor(TypeMirror typeMirror, boolean inNullMarkedScope) {
    return createTypeDescriptorWithNullability(typeMirror, ImmutableList.of(), inNullMarkedScope);
  }

  /** Creates a type descriptor for the given TypeMirror, taking into account nullability. */
  @Nullable
  private TypeDescriptor createTypeDescriptorWithNullability(
      TypeMirror typeMirror,
      List<? extends AnnotationMirror> elementAnnotations,
      boolean inNullMarkedScope) {
    if (typeMirror == null || typeMirror.getKind() == TypeKind.NONE) {
      return null;
    }

    if (typeMirror.getKind().isPrimitive() || typeMirror.getKind() == TypeKind.VOID) {
      return PrimitiveTypes.get(asElement(typeMirror).getSimpleName().toString());
    }

    if (typeMirror.getKind() == TypeKind.INTERSECTION) {
      return createIntersectionType((IntersectionClassType) typeMirror);
    }

    if (typeMirror.getKind() == TypeKind.UNION) {
      return createUnionType((UnionClassType) typeMirror);
    }

    if (typeMirror.getKind() == TypeKind.NULL) {
      return TypeDescriptors.get().javaLangObject;
    }

    if (typeMirror.getKind() == TypeKind.TYPEVAR) {
      return createTypeVariable((javax.lang.model.type.TypeVariable) typeMirror);
    }

    if (typeMirror.getKind() == TypeKind.WILDCARD) {
      return createWildcardTypeVariable(
          ((javax.lang.model.type.WildcardType) typeMirror).getExtendsBound());
    }

    boolean isNullable = isNullable(typeMirror, elementAnnotations, inNullMarkedScope);
    if (typeMirror.getKind() == TypeKind.ARRAY) {
      ArrayType arrayType = (ArrayType) typeMirror;
      TypeDescriptor componentTypeDescriptor =
          createTypeDescriptor(arrayType.getComponentType(), inNullMarkedScope);
      return ArrayTypeDescriptor.newBuilder()
          .setComponentTypeDescriptor(componentTypeDescriptor)
          .setNullable(isNullable)
          .build();
    }

    return withNullability(
        createDeclaredType((ClassType) typeMirror, inNullMarkedScope), isNullable);
  }

  /**
   * Returns whether the given type binding should be nullable, according to the annotations on it
   * and if nullability is enabled for the package containing the binding.
   */
  private boolean isNullable(
      TypeMirror typeMirror,
      List<? extends AnnotationMirror> elementAnnotations,
      boolean inNullMarkedScope) {
    checkArgument(!typeMirror.getKind().isPrimitive());

    if (asTypeElement(typeMirror).getQualifiedName().contentEquals("java.lang.Void")) {
      // Void is always nullable.
      return true;
    }

    Iterable<? extends AnnotationMirror> allAnnotations =
        Iterables.concat(elementAnnotations, typeMirror.getAnnotationMirrors());

    for (AnnotationMirror annotationMirror : allAnnotations) {
      if (isNonNullAnnotation(annotationMirror)) {
        return false;
      }
      if (isNullableAnnotation(annotationMirror)) {
        return true;
      }
    }

    return !inNullMarkedScope;
  }

  private static boolean isNonNullAnnotation(AnnotationMirror annotation) {
    Type annotationType = (Type) annotation.getAnnotationType();
    return Nullability.isNonNullAnnotation(
        annotationType.asElement().getQualifiedName().toString());
  }

  private static boolean isNullableAnnotation(AnnotationMirror annotation) {
    Type annotationType = (Type) annotation.getAnnotationType();
    return Nullability.isNullableAnnotation(
        annotationType.asElement().getQualifiedName().toString());
  }

  private TypeVariable createTypeVariable(javax.lang.model.type.TypeVariable typeVariable) {
    if (typeVariable instanceof CapturedType) {
      return createWildcardTypeVariable(typeVariable.getUpperBound());
    }

    Supplier<TypeDescriptor> boundTypeDescriptorFactory =
        () -> createTypeDescriptor(typeVariable.getUpperBound());

    List<String> classComponents = getClassComponents(typeVariable);
    return TypeVariable.newBuilder()
        .setUpperBoundTypeDescriptorSupplier(boundTypeDescriptorFactory)
        .setUniqueKey(
            String.join("::", classComponents)
                + (typeVariable.getUpperBound() != null
                    ? typeVariable.getUpperBound().toString()
                    : ""))
        .setName(typeVariable.asElement().getSimpleName().toString())
        .build();
  }

  private TypeVariable createWildcardTypeVariable(TypeMirror bound) {
    return TypeVariable.newBuilder()
        .setUpperBoundTypeDescriptorSupplier(() -> createTypeDescriptor(bound))
        .setWildcard(true)
        .setName("?")
        .setUniqueKey("::?::" + (bound != null ? bound.toString() : ""))
        .build();
  }

  private static DeclaredTypeDescriptor withNullability(
      DeclaredTypeDescriptor typeDescriptor, boolean nullable) {
    return nullable ? typeDescriptor.toNullable() : typeDescriptor.toNonNullable();
  }

  /**
   * In case the given type element is nested, return the outermost possible enclosing type element.
   */
  private static TypeElement toTopLevelTypeBinding(Element element) {
    if (element.getEnclosingElement().getKind() == ElementKind.PACKAGE) {
      return (TypeElement) element;
    }
    return toTopLevelTypeBinding(element.getEnclosingElement());
  }

  private ImmutableList<String> getClassComponents(
      javax.lang.model.type.TypeVariable typeVariable) {
    Element enclosingElement = typeVariable.asElement().getEnclosingElement();
    if (enclosingElement.getKind() == ElementKind.CLASS
        || enclosingElement.getKind() == ElementKind.INTERFACE
        || enclosingElement.getKind() == ElementKind.ENUM) {
      return ImmutableList.<String>builder()
          .addAll(getClassComponents(enclosingElement))
          .add(
              // If it is a class-level type variable, use the simple name (with prefix "C_") as the
              // current name component.
              "C_" + typeVariable.asElement().getSimpleName())
          .build();
    } else {
      return ImmutableList.<String>builder()
          .addAll(getClassComponents(enclosingElement.getEnclosingElement()))
          .add(
              "M_"
                  + enclosingElement.getSimpleName()
                  + "_"
                  + typeVariable.asElement().getSimpleName())
          .build();
    }
  }

  private ImmutableList<String> getClassComponents(Element element) {
    if (!(element instanceof TypeElement)) {
      return ImmutableList.of();
    }
    TypeElement typeElement = (TypeElement) element;
    List<String> classComponents = new ArrayList<>();
    TypeElement currentType = typeElement;
    while (currentType != null) {
      String simpleName;
      if (currentType.getNestingKind() == NestingKind.LOCAL
          || currentType.getNestingKind() == NestingKind.ANONYMOUS) {
        // JavaC binary name for local class is like package.components.EnclosingClass$1SimpleName
        // Extract the generated name by taking the part after the binary name of the declaring
        // class.
        String binaryName = getBinaryNameFromTypeBinding(currentType);
        String declaringClassPrefix =
            getBinaryNameFromTypeBinding(getEnclosingType(currentType)) + "$";
        simpleName = binaryName.substring(declaringClassPrefix.length());
      } else {
        simpleName = asElement(erasure(currentType.asType())).getSimpleName().toString();
      }
      classComponents.add(0, simpleName);
      Element enclosingElement = currentType.getEnclosingElement();
      while (enclosingElement != null
          && enclosingElement.getKind() != ElementKind.CLASS
          && enclosingElement.getKind() != ElementKind.INTERFACE
          && enclosingElement.getKind() != ElementKind.ENUM) {
        enclosingElement = enclosingElement.getEnclosingElement();
      }
      currentType = (TypeElement) enclosingElement;
    }
    return ImmutableList.copyOf(classComponents);
  }

  /** Returns the binary name for a type element. */
  private static String getBinaryNameFromTypeBinding(TypeElement typeElement) {
    return ((ClassSymbol) typeElement).flatName().toString();
  }

  private boolean isEnumSyntheticMethod(ExecutableElement methodElement) {
    // Enum synthetic methods are not marked as such because per JLS 13.1 these methods are
    // implicitly declared but are not marked as synthetic.
    return getEnclosingType(methodElement).getKind() == ElementKind.ENUM
        && (isValuesMethod(methodElement) || isValueOfMethod(methodElement));
  }

  private static boolean isValuesMethod(ExecutableElement methodElement) {
    return methodElement.getSimpleName().contentEquals("values")
        && methodElement.getParameters().isEmpty();
  }

  private boolean isValueOfMethod(ExecutableElement methodElement) {
    return methodElement.getSimpleName().contentEquals("valueOf")
        && methodElement.getParameters().size() == 1
        && asTypeElement(methodElement.getParameters().get(0).asType())
            .getQualifiedName()
            .contentEquals("java.lang.String");
  }

  /**
   * Returns true if instances of this type capture its outer instances; i.e. if it is an non static
   * member class, or an anonymous or local class defined in an instance context.
   */
  static boolean capturesEnclosingInstance(ClassSymbol classSymbol) {
    if (classSymbol.isAnonymous()) {
      return classSymbol.hasOuterInstance() || !isStatic(classSymbol.getEnclosingElement());
    }
    return classSymbol.hasOuterInstance();
  }

  FieldDescriptor createFieldDescriptor(VariableElement variableElement) {
    return createFieldDescriptor(variableElement, variableElement.asType());
  }

  FieldDescriptor createFieldDescriptor(VariableElement variableElement, TypeMirror type) {

    boolean isStatic = isStatic(variableElement);
    Visibility visibility = getVisibility(variableElement);
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(getEnclosingType(variableElement).asType());
    String fieldName = variableElement.getSimpleName().toString();

    TypeDescriptor thisTypeDescriptor =
        createTypeDescriptorWithNullability(
            type,
            variableElement.getAnnotationMirrors(),
            enclosingTypeDescriptor.getTypeDeclaration().isNullMarked());

    boolean isEnumConstant = ((VarSymbol) variableElement).isEnum();
    if (isEnumConstant) {
      // Enum fields are always non-nullable.
      thisTypeDescriptor = thisTypeDescriptor.toNonNullable();
    }

    FieldDescriptor declarationFieldDescriptor = null;
    if (!javacTypes.isSameType(variableElement.asType(), type)) {
      // Field references might be parameterized, and when they are we set the declaration
      // descriptor to the unparameterized declaration.
      declarationFieldDescriptor = createFieldDescriptor(variableElement, variableElement.asType());
    }

    JsInfo jsInfo = JsInteropUtils.getJsInfo(variableElement);
    Object constantValue = variableElement.getConstantValue();
    boolean isCompileTimeConstant = constantValue != null;
    if (isCompileTimeConstant) {
      thisTypeDescriptor = thisTypeDescriptor.toNonNullable();
    }
    boolean isFinal = isFinal(variableElement);
    return FieldDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setName(fieldName)
        .setTypeDescriptor(thisTypeDescriptor)
        .setStatic(isStatic)
        .setVisibility(visibility)
        .setOriginalJsInfo(jsInfo)
        .setFinal(isFinal)
        .setCompileTimeConstant(isCompileTimeConstant)
        .setConstantValue(
            constantValue != null ? Literal.fromValue(constantValue, thisTypeDescriptor) : null)
        .setDeclarationDescriptor(declarationFieldDescriptor)
        .setEnumConstant(isEnumConstant)
        .setUnusableByJsSuppressed(
            JsInteropAnnotationUtils.isUnusableByJsSuppressed(variableElement))
        .setDeprecated(isDeprecated(variableElement))
        .build();
  }

  /**
   * Creates a MethodDescriptor from javac internal representation.
   *
   * @param methodType an ExecutableType containing the (inferred) specialization of the method in a
   *     usage location.
   * @param returnType the (inferred) specialized return type.
   * @param declarationMethodElement the method declaration.
   */
  MethodDescriptor createMethodDescriptor(
      ExecutableType methodType, Type returnType, ExecutableElement declarationMethodElement) {

    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(declarationMethodElement.getEnclosingElement().asType());

    MethodDescriptor declarationMethodDescriptor = null;
    List<? extends TypeMirror> parameterTypes = methodType.getParameterTypes();
    if (isSpecialized(declarationMethodElement, parameterTypes, returnType)) {
      declarationMethodDescriptor = createDeclarationMethodDescriptor(declarationMethodElement);
    }

    TypeDescriptor returnTypeDescriptor =
        applyReturnTypeNullabilityAnnotations(
            createTypeDescriptorWithNullability(
                returnType,
                declarationMethodElement.getAnnotationMirrors(),
                enclosingTypeDescriptor.getTypeDeclaration().isNullMarked()),
            declarationMethodElement);

    ImmutableList.Builder<TypeDescriptor> parametersBuilder = ImmutableList.builder();
    for (int i = 0; i < parameterTypes.size(); i++) {
      parametersBuilder.add(
          applyParameterNullabilityAnnotations(
              createTypeDescriptorWithNullability(
                  parameterTypes.get(i),
                  declarationMethodElement.getParameters().get(i).getAnnotationMirrors(),
                  enclosingTypeDescriptor.getTypeDeclaration().isNullMarked()),
              declarationMethodElement,
              i));
    }

    // generate type parameters declared in the method.
    return createDeclaredMethodDescriptor(
        enclosingTypeDescriptor.toNullable(),
        declarationMethodElement,
        declarationMethodDescriptor,
        parametersBuilder.build(),
        returnTypeDescriptor);
  }

  /** Create a MethodDescriptor directly based on the given JavaC ExecutableElement. */
  MethodDescriptor createMethodDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      ExecutableElement methodElement,
      ExecutableElement declarationMethodElement) {

    MethodDescriptor declarationMethodDescriptor = null;

    ImmutableList<TypeMirror> parameters =
        methodElement.getParameters().stream()
            .map(VariableElement::asType)
            .collect(toImmutableList());

    TypeMirror returnType = methodElement.getReturnType();
    if (isSpecialized(declarationMethodElement, parameters, returnType)) {
      declarationMethodDescriptor =
          createDeclarationMethodDescriptor(
              declarationMethodElement, enclosingTypeDescriptor.toUnparameterizedTypeDescriptor());
    }

    TypeDescriptor returnTypeDescriptor =
        applyReturnTypeNullabilityAnnotations(
            createTypeDescriptorWithNullability(
                returnType,
                declarationMethodElement.getAnnotationMirrors(),
                enclosingTypeDescriptor.getTypeDeclaration().isNullMarked()),
            declarationMethodElement);

    ImmutableList.Builder<TypeDescriptor> parametersBuilder = ImmutableList.builder();
    for (int i = 0; i < parameters.size(); i++) {
      parametersBuilder.add(
          applyParameterNullabilityAnnotations(
              createTypeDescriptorWithNullability(
                  parameters.get(i),
                  declarationMethodElement.getParameters().get(i).getAnnotationMirrors(),
                  enclosingTypeDescriptor.getTypeDeclaration().isNullMarked()),
              declarationMethodElement,
              i));
    }

    return createDeclaredMethodDescriptor(
        enclosingTypeDescriptor.toNullable(),
        declarationMethodElement,
        declarationMethodDescriptor,
        parametersBuilder.build(),
        returnTypeDescriptor);
  }

  /** Create a MethodDescriptor directly based on the given JavaC ExecutableElement. */
  MethodDescriptor createDeclarationMethodDescriptor(ExecutableElement methodElement) {
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(methodElement.getEnclosingElement().asType());
    return createDeclarationMethodDescriptor(methodElement, enclosingTypeDescriptor);
  }

  /** Create a MethodDescriptor directly based on the given JavaC ExecutableElement. */
  MethodDescriptor createDeclarationMethodDescriptor(
      ExecutableElement methodElement, DeclaredTypeDescriptor enclosingTypeDescriptor) {
    return createMethodDescriptor(enclosingTypeDescriptor, methodElement, methodElement);
  }


  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Utility methods to process nullability annotations on classes that are compiled separately.
  // Javac does not present TYPE_USE annotation in the returned type instances.
  private static TypeDescriptor applyParameterNullabilityAnnotations(
      TypeDescriptor typeDescriptor, ExecutableElement declarationMethodElement, int index) {
    return applyNullabilityAnnotations(
        typeDescriptor,
        declarationMethodElement,
        position ->
            position.parameter_index == index
                && position.type == TargetType.METHOD_FORMAL_PARAMETER);
  }

  private static TypeDescriptor applyReturnTypeNullabilityAnnotations(
      TypeDescriptor typeDescriptor, ExecutableElement declarationMethodElement) {
    return applyNullabilityAnnotations(
        typeDescriptor,
        declarationMethodElement,
        position -> position.type == TargetType.METHOD_RETURN);
  }

  private static TypeDescriptor applyNullabilityAnnotations(
      TypeDescriptor typeDescriptor,
      Element declarationMethodElement,
      Predicate<TypeAnnotationPosition> positionSelector) {
    List<TypeCompound> methodAnnotations =
        ((Symbol) declarationMethodElement).getRawTypeAttributes();
    for (TypeCompound methodAnnotation : methodAnnotations) {
      TypeAnnotationPosition position = methodAnnotation.getPosition();
      if (!positionSelector.test(position)) {
        continue;
      }
      if (isNonNullAnnotation(methodAnnotation)) {
        typeDescriptor =
            applyNullabilityAnnotation(typeDescriptor, position.location, /* isNullable= */ false);
      } else if (isNullableAnnotation(methodAnnotation)) {
        typeDescriptor =
            applyNullabilityAnnotation(typeDescriptor, position.location, /* isNullable= */ true);
      }
    }

    return typeDescriptor;
  }

  private static TypeDescriptor applyNullabilityAnnotation(
      TypeDescriptor typeDescriptor, List<TypePathEntry> location, boolean isNullable) {
    if (location.isEmpty()) {
      if (TypeDescriptors.isJavaLangVoid(typeDescriptor)) {
        return typeDescriptor;
      }
      return isNullable ? typeDescriptor.toNullable() : typeDescriptor.toNonNullable();
    }
    TypePathEntry currentEntry = location.get(0);
    List<TypePathEntry> rest = location.subList(1, location.size());
    switch (currentEntry.tag) {
      case TYPE_ARGUMENT:
        DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
        List<TypeDescriptor> replacements =
            new ArrayList<>(declaredTypeDescriptor.getTypeArgumentDescriptors());
        if (currentEntry.arg < replacements.size()) {
          // Only apply the type argument annotation if the type is not raw.
          replacements.set(
              currentEntry.arg,
              applyNullabilityAnnotation(replacements.get(currentEntry.arg), rest, isNullable));
        }
        return DeclaredTypeDescriptor.Builder.from(declaredTypeDescriptor)
            .setTypeArgumentDescriptors(replacements)
            .build();
      case ARRAY:
        ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
        return ArrayTypeDescriptor.newBuilder()
            .setComponentTypeDescriptor(
                applyNullabilityAnnotation(
                    arrayTypeDescriptor.getComponentTypeDescriptor(), rest, isNullable))
            .setNullable(typeDescriptor.isNullable())
            .build();
      case INNER_TYPE:
        DeclaredTypeDescriptor innerType = (DeclaredTypeDescriptor) typeDescriptor;
        // Consume all inner type annotation and only continue if does not relate to an outer type
        // of the type in question.
        int innerDepth = getInnerDepth(innerType);
        int innerCount = countInner(rest) + 1;
        if (innerCount != innerDepth) {
          // Applies to outer type, not relevant for nullability, ignore.
          return innerType;
        }
        return applyNullabilityAnnotation(
            typeDescriptor, rest.subList(innerCount - 1, rest.size()), isNullable);
      case WILDCARD:
        TypeVariable typeVariable = (TypeVariable) typeDescriptor;
        return TypeVariable.createWildcardWithUpperBound(
            applyNullabilityAnnotation(
                typeVariable.getUpperBoundTypeDescriptor(), rest, isNullable));
    }
    return typeDescriptor;
  }

  private static int countInner(List<TypePathEntry> rest) {
    return !rest.isEmpty() && rest.get(0).tag == TypePathEntryKind.INNER_TYPE
        ? countInner(rest.subList(1, rest.size())) + 1
        : 0;
  }

  private static int getInnerDepth(DeclaredTypeDescriptor innerType) {
    if (innerType.getTypeDeclaration().isCapturingEnclosingInstance()) {
      return getInnerDepth(innerType.getEnclosingTypeDescriptor()) + 1;
    }
    return 0;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Returns true if any of the type parameters has been specialized.
   *
   * <p>For example the type {@code List<String>} specialized the type variable {@code T} from the
   * class declaration.
   */
  private boolean isSpecialized(
      ExecutableElement declarationMethodElement,
      List<? extends TypeMirror> parameters,
      TypeMirror returnType) {
    return !isSameType(returnType, declarationMethodElement.getReturnType())
        || !Streams.zip(
                parameters.stream(),
                declarationMethodElement.getParameters().stream(),
                (thisType, thatType) -> isSameType(thisType, thatType.asType()))
            .allMatch(equals -> equals);
  }

  private boolean isSameType(TypeMirror thisType, TypeMirror thatType) {
    return internalTypes.isSameType((Type) thisType, (Type) thatType);
  }

  private MethodDescriptor createDeclaredMethodDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      ExecutableElement declarationMethodElement,
      MethodDescriptor declarationMethodDescriptor,
      List<TypeDescriptor> parameters,
      TypeDescriptor returnTypeDescriptor) {
    ImmutableList<TypeVariable> typeParameterTypeDescriptors =
        declarationMethodElement.getTypeParameters().stream()
            .map(Element::asType)
            .map(this::createTypeDescriptor)
            .map(TypeVariable.class::cast)
            .collect(toImmutableList());

    boolean isStatic = isStatic(declarationMethodElement);
    Visibility visibility = getVisibility(declarationMethodElement);
    boolean isDefault = isDefaultMethod(declarationMethodElement);
    JsInfo jsInfo = JsInteropUtils.getJsInfo(declarationMethodElement);

    boolean isNative =
        isNative(declarationMethodElement)
            || (!jsInfo.isJsOverlay()
                && enclosingTypeDescriptor.isNative()
                && isAbstract(declarationMethodElement));

    boolean isConstructor = declarationMethodElement.getKind() == ElementKind.CONSTRUCTOR;
    String methodName = declarationMethodElement.getSimpleName().toString();

    ImmutableList.Builder<ParameterDescriptor> parameterDescriptorBuilder = ImmutableList.builder();
    for (int i = 0; i < parameters.size(); i++) {
      parameterDescriptorBuilder.add(
          ParameterDescriptor.newBuilder()
              .setTypeDescriptor(parameters.get(i))
              .setJsOptional(JsInteropUtils.isJsOptional(declarationMethodElement, i))
              .setVarargs(i == parameters.size() - 1 && declarationMethodElement.isVarArgs())
              .setDoNotAutobox(JsInteropUtils.isDoNotAutobox(declarationMethodElement, i))
              .build());
    }

    boolean hasUncheckedCast = hasUncheckedCastAnnotation(declarationMethodElement);
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setName(isConstructor ? null : methodName)
        .setParameterDescriptors(parameterDescriptorBuilder.build())
        .setDeclarationDescriptor(declarationMethodDescriptor)
        .setReturnTypeDescriptor(isConstructor ? enclosingTypeDescriptor : returnTypeDescriptor)
        .setTypeParameterTypeDescriptors(typeParameterTypeDescriptors)
        .setOriginalJsInfo(jsInfo)
        .setVisibility(visibility)
        .setStatic(isStatic)
        .setConstructor(isConstructor)
        .setNative(isNative)
        .setFinal(isFinal(declarationMethodElement))
        .setDefaultMethod(isDefault)
        .setAbstract(isAbstract(declarationMethodElement))
        .setSynthetic(isSynthetic(declarationMethodElement))
        .setEnumSyntheticMethod(isEnumSyntheticMethod(declarationMethodElement))
        .setSideEffectFree(isAnnotatedWithHasNoSideEffects(declarationMethodElement))
        .setUnusableByJsSuppressed(
            JsInteropAnnotationUtils.isUnusableByJsSuppressed(declarationMethodElement))
        .setDeprecated(isDeprecated(declarationMethodElement))
        .setUncheckedCast(hasUncheckedCast)
        .build();
  }

  /** Returns true if the element is annotated with @UncheckedCast. */
  private static boolean hasUncheckedCastAnnotation(Element element) {
    return AnnotationUtils.hasAnnotation(element, UNCHECKED_CAST_ANNOTATION_NAME);
  }

  /** Returns true if the element is annotated with @HasNoSideEffects. */
  private static boolean isAnnotatedWithHasNoSideEffects(Element element) {
    return AnnotationUtils.hasAnnotation(element, HAS_NO_SIDE_EFFECTS_ANNOTATION_NAME);
  }

  private boolean isJavaLangObjectOverride(MethodSymbol method) {
    return getJavaLangObjectMethods().stream()
        .anyMatch(
            om ->
                method.getSimpleName().equals(om.name)
                    && javacTypes.isSubsignature(
                        (ExecutableType) method.asType(), (ExecutableType) om.asType()));
  }

  private ImmutableSet<MethodSymbol> getJavaLangObjectMethods() {
    ClassType javaLangObjectTypeBinding =
        (ClassType) elements.getTypeElement("java.lang.Object").asType();
    return getDeclaredMethods(javaLangObjectTypeBinding).stream()
        .map(MethodDeclarationPair::getMethodSymbol)
        .filter(JavaEnvironment::isPolymorphic)
        .collect(ImmutableSet.toImmutableSet());
  }

  private static boolean isPolymorphic(MethodSymbol method) {
    return !method.isConstructor()
        && !isStatic(method)
        && !method.getModifiers().contains(Modifier.PRIVATE);
  }

  public ImmutableList<TypeDescriptor> createTypeDescriptors(
      List<? extends TypeMirror> typeMirrors, boolean inNullMarkedScope) {
    return typeMirrors.stream()
        .map(typeMirror -> createTypeDescriptor(typeMirror, inNullMarkedScope))
        .collect(toImmutableList());
  }

  public <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      List<? extends TypeMirror> typeMirrors,
      boolean inNullMarkedScope,
      Class<T> clazz,
      Element declarationElement) {
    ImmutableList.Builder<T> typeDescriptorsBuilder = ImmutableList.builder();
    for (int i = 0; i < typeMirrors.size(); i++) {
      final int index = i;
      typeDescriptorsBuilder.add(
          clazz.cast(
              applyNullabilityAnnotations(
                  createTypeDescriptor(typeMirrors.get(i), inNullMarkedScope, clazz),
                  declarationElement,
                  position ->
                      position.type == TargetType.CLASS_EXTENDS && position.type_index == index)));
    }
    return typeDescriptorsBuilder.build();
  }

  public <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      List<? extends TypeMirror> typeMirrors, boolean inNullMarkedScope, Class<T> clazz) {
    return typeMirrors.stream()
        .map(typeMirror -> createTypeDescriptor(typeMirror, inNullMarkedScope, clazz))
        .collect(toImmutableList());
  }

  private TypeElement getTypeElement(String qualifiedSourceName) {
    return elements.getTypeElement(qualifiedSourceName);
  }

  private Element asElement(TypeMirror typeMirror) {
    if (typeMirror instanceof JCPrimitiveType) {
      return ((JCPrimitiveType) typeMirror).asElement();
    }
    if (typeMirror instanceof Type) {
      return ((Type) typeMirror).tsym;
    }
    return javacTypes.asElement(typeMirror);
  }

  private TypeElement asTypeElement(TypeMirror typeMirror) {
    return (TypeElement) asElement(typeMirror);
  }

  private TypeMirror erasure(TypeMirror typeMirror) {
    return javacTypes.erasure(typeMirror);
  }

  private PackageElement getPackageOf(TypeElement typeElement) {
    return elements.getPackageOf(typeElement);
  }

  private TypeDescriptor createIntersectionType(IntersectionClassType intersectionType) {
    ImmutableList<TypeDescriptor> intersectedTypeDescriptors =
        createTypeDescriptors(
            intersectionType.getBounds(), /* inNullMarkedScope= */ false, TypeDescriptor.class);
    return IntersectionTypeDescriptor.newBuilder()
        .setIntersectionTypeDescriptors(intersectedTypeDescriptors)
        .build();
  }

  private TypeDescriptor createUnionType(UnionClassType unionType) {
    ImmutableList<TypeDescriptor> unionTypeDescriptors =
        createTypeDescriptors(unionType.getAlternatives(), /* inNullMarkedScope= */ false);
    return UnionTypeDescriptor.newBuilder().setUnionTypeDescriptors(unionTypeDescriptors).build();
  }

  private DeclaredTypeDescriptor createDeclaredType(
      final DeclaredType classType, boolean inNullMarkedScope) {

    DeclaredTypeDescriptor cachedTypeDescriptor =
        getCachedTypeDescriptor(classType, inNullMarkedScope);
    if (cachedTypeDescriptor != null) {
      return cachedTypeDescriptor;
    }

    Supplier<ImmutableList<MethodDescriptor>> declaredMethods =
        () ->
            getDeclaredMethods((ClassType) classType).stream()
                .map(
                    methodDeclarationPair ->
                        createMethodDescriptor(
                            createDeclaredTypeDescriptor(classType, inNullMarkedScope),
                            methodDeclarationPair.getMethodSymbol(),
                            methodDeclarationPair.getDeclarationMethodSymbol()))
                .collect(toImmutableList());

    Supplier<ImmutableList<FieldDescriptor>> declaredFields =
        () ->
            ((TypeElement) classType.asElement())
                .getEnclosedElements().stream()
                    .filter(
                        element ->
                            element.getKind() == ElementKind.FIELD
                                || element.getKind() == ElementKind.ENUM_CONSTANT)
                    .map(VariableElement.class::cast)
                    .map(this::createFieldDescriptor)
                    .collect(toImmutableList());

    TypeDeclaration typeDeclaration = createDeclarationForType((TypeElement) classType.asElement());

    // Compute these even later
    DeclaredTypeDescriptor typeDescriptor =
        DeclaredTypeDescriptor.newBuilder()
            .setTypeDeclaration(typeDeclaration)
            .setEnclosingTypeDescriptor(createDeclaredTypeDescriptor(classType.getEnclosingType()))
            .setSuperTypeDescriptorFactory(
                td ->
                    td.isInterface()
                        ? null
                        : createDeclaredTypeDescriptor(
                            javacTypes.directSupertypes(classType).stream()
                                .filter(Predicates.not(Type::isInterface))
                                .findFirst()
                                .orElse(null),
                            inNullMarkedScope))
            .setInterfaceTypeDescriptorsFactory(
                td ->
                    createTypeDescriptors(
                        javacTypes.directSupertypes(classType).stream()
                            .filter(Type::isInterface)
                            .collect(toImmutableList()),
                        inNullMarkedScope,
                        DeclaredTypeDescriptor.class))
            .setSingleAbstractMethodDescriptorFactory(
                td -> {
                  MethodSymbol functionalInterfaceMethod = getFunctionalInterfaceMethod(classType);
                  return createMethodDescriptor(
                      td,
                      (MethodSymbol)
                          functionalInterfaceMethod.asMemberOf(
                              ((ClassSymbol) classType.asElement()).asType(), internalTypes),
                      getFunctionalInterfaceMethodDecl(classType));
                })
            .setTypeArgumentDescriptors(
                createTypeDescriptors(getTypeArguments(classType), inNullMarkedScope))
            .setDeclaredFieldDescriptorsFactory(declaredFields)
            .setDeclaredMethodDescriptorsFactory(declaredMethods)
            .build();
    putTypeDescriptorInCache(inNullMarkedScope, classType, typeDescriptor);
    return typeDescriptor;
  }

  private final Map<DeclaredType, DeclaredTypeDescriptor>
      cachedDeclaredTypeDescriptorByDeclaredTypeInNullMarkedScope = new HashMap<>();

  private final Map<DeclaredType, DeclaredTypeDescriptor>
      cachedDeclaredTypeDescriptorByDeclaredTypeOutOfNullMarkedScope = new HashMap<>();

  private DeclaredTypeDescriptor getCachedTypeDescriptor(
      DeclaredType classType, boolean inNullMarkedScope) {
    Map<DeclaredType, DeclaredTypeDescriptor> cache =
        inNullMarkedScope
            ? cachedDeclaredTypeDescriptorByDeclaredTypeInNullMarkedScope
            : cachedDeclaredTypeDescriptorByDeclaredTypeOutOfNullMarkedScope;
    return cache.get(classType);
  }

  private void putTypeDescriptorInCache(
      boolean inNullMarkedScope, DeclaredType classType, DeclaredTypeDescriptor typeDescriptor) {
    Map<DeclaredType, DeclaredTypeDescriptor> cache =
        inNullMarkedScope
            ? cachedDeclaredTypeDescriptorByDeclaredTypeInNullMarkedScope
            : cachedDeclaredTypeDescriptorByDeclaredTypeOutOfNullMarkedScope;
    cache.put(classType, typeDescriptor);
  }

  private static List<TypeMirror> getTypeArguments(DeclaredType declaredType) {
    List<TypeMirror> typeArguments = new ArrayList<>();
    DeclaredType currentType = declaredType;
    do {
      typeArguments.addAll(currentType.getTypeArguments());
      Element enclosingElement = currentType.asElement().getEnclosingElement();
      if (enclosingElement.getKind() == ElementKind.METHOD
          || enclosingElement.getKind() == ElementKind.CONSTRUCTOR) {
        typeArguments.addAll(
            ((Parameterizable) enclosingElement)
                .getTypeParameters().stream().map(Element::asType).collect(toImmutableList()));
      }
      currentType =
          currentType.getEnclosingType() instanceof DeclaredType
              ? (DeclaredType) currentType.getEnclosingType()
              : null;
    } while (currentType != null);
    return typeArguments;
  }

  private ImmutableList<MethodDeclarationPair> getDeclaredMethods(ClassType classType) {
    return classType.asElement().getEnclosedElements().stream()
        .filter(
            element ->
                !isSynthetic(element)
                    && (element.getKind() == ElementKind.METHOD
                        || element.getKind() == ElementKind.CONSTRUCTOR))
        .map(MethodSymbol.class::cast)
        .map(
            methodSymbol ->
                new MethodDeclarationPair(
                    (MethodSymbol) methodSymbol.asMemberOf(classType, internalTypes), methodSymbol))
        .collect(toImmutableList());
  }

  static final class MethodDeclarationPair {
    private final MethodSymbol methodSymbol;
    private final MethodSymbol declarationMethodSymbol;

    private MethodDeclarationPair(MethodSymbol methodSymbol, MethodSymbol declarationMethodSymbol) {
      this.methodSymbol = methodSymbol;
      this.declarationMethodSymbol = declarationMethodSymbol;
    }

    public MethodSymbol getMethodSymbol() {
      return methodSymbol;
    }

    public MethodSymbol getDeclarationMethodSymbol() {
      return declarationMethodSymbol;
    }
  }

  private ImmutableList<MethodDeclarationPair> getMethods(ClassType classType) {
    return elements.getAllMembers((TypeElement) classType.asElement()).stream()
        .filter(
            element ->
                !isSynthetic(element)
                    && (element.getKind() == ElementKind.METHOD
                        || element.getKind() == ElementKind.CONSTRUCTOR))
        .map(MethodSymbol.class::cast)
        .map(
            methodSymbol ->
                new MethodDeclarationPair(
                    (MethodSymbol) methodSymbol.asMemberOf(classType, internalTypes), methodSymbol))
        .collect(toImmutableList());
  }

  private static Kind getKindFromTypeBinding(TypeElement typeElement) {
    if (isEnum(typeElement) && !isAnonymous(typeElement)) {
      // Do not consider the anonymous classes that constitute enum values as Enums, only the
      // enum "class" itself is considered Kind.ENUM.
      return Kind.ENUM;
    } else if (isClass(typeElement) || (isEnum(typeElement) && isAnonymous(typeElement))) {
      return Kind.CLASS;
    } else if (isInterface(typeElement)) {
      return Kind.INTERFACE;
    }
    throw new InternalCompilerError("Type binding %s not handled.", typeElement);
  }

  private static String getJsName(final TypeElement classSymbol) {
    return JsInteropAnnotationUtils.getJsName(classSymbol);
  }

  @Nullable
  private static String getJsNamespace(TypeElement classSymbol, PackageInfoCache packageInfoCache) {
    String jsNamespace = JsInteropAnnotationUtils.getJsNamespace(classSymbol);
    if (jsNamespace != null) {
      return jsNamespace;
    }

    // Maybe namespace is set via package-info file?
    boolean isTopLevelType = classSymbol.getEnclosingElement().getKind() == ElementKind.PACKAGE;
    if (isTopLevelType) {
      return packageInfoCache.getJsNamespace(getBinaryNameFromTypeBinding(classSymbol));
    }
    return null;
  }

  @Nullable
  TypeDeclaration createDeclarationForType(final TypeElement typeElement) {
    if (typeElement == null) {
      return null;
    }

    PackageInfoCache packageInfoCache = PackageInfoCache.get();

    boolean isFromSource = ((ClassSymbol) typeElement).classfile == null;
    if (isFromSource) {
      TypeElement topLevelTypeBinding = toTopLevelTypeBinding(typeElement);
      // Let the PackageInfoCache know that this class is Source, otherwise it would have to rummage
      // around in the class path to figure it out and it might even come up with the wrong answer
      // for example if this class has also been globbed into some other library that is a
      // dependency of this one.
      PackageInfoCache.get().markAsSource(getBinaryNameFromTypeBinding(topLevelTypeBinding));
    }

    // Compute these first since they're reused in other calculations.
    String packageName = getPackageOf(typeElement).getQualifiedName().toString();
    boolean isAbstract = isAbstract(typeElement) && !isInterface(typeElement);
    Kind kind = getKindFromTypeBinding(typeElement);
    // TODO(b/341721484): Even though enums can not have the final modifier, turbine make them final
    // in the header jars.
    boolean isFinal = isFinal(typeElement) && kind != Kind.ENUM;

    Supplier<ImmutableList<MethodDescriptor>> declaredMethods =
        () -> {
          ImmutableList.Builder<MethodDescriptor> listBuilder = ImmutableList.builder();
          for (MethodSymbol methodElement :
              typeElement.getEnclosedElements().stream()
                  .filter(
                      element ->
                          element.getKind() == ElementKind.METHOD
                              || element.getKind() == ElementKind.CONSTRUCTOR)
                  .map(MethodSymbol.class::cast)
                  .collect(toImmutableList())) {
            MethodDescriptor methodDescriptor = createDeclarationMethodDescriptor(methodElement);
            listBuilder.add(methodDescriptor);
          }
          return listBuilder.build();
        };

    Supplier<ImmutableList<FieldDescriptor>> declaredFields =
        () ->
            typeElement.getEnclosedElements().stream()
                .filter(
                    element ->
                        element.getKind() == ElementKind.FIELD
                            || element.getKind() == ElementKind.ENUM_CONSTANT)
                .map(VariableElement.class::cast)
                .map(this::createFieldDescriptor)
                .collect(toImmutableList());

    JsEnumInfo jsEnumInfo = JsInteropUtils.getJsEnumInfo(typeElement);

    List<TypeParameterElement> typeParameterElements = getTypeParameters(typeElement);

    boolean isNullMarked = isNullMarked(typeElement, packageInfoCache);
    return TypeDeclaration.newBuilder()
        .setClassComponents(getClassComponents(typeElement))
        .setEnclosingTypeDeclaration(createDeclarationForType(getEnclosingType(typeElement)))
        .setInterfaceTypeDescriptorsFactory(
            () ->
                createTypeDescriptors(
                    typeElement.getInterfaces(),
                    isNullMarked,
                    DeclaredTypeDescriptor.class,
                    typeElement))
        .setUnparameterizedTypeDescriptorFactory(
            () -> createDeclaredTypeDescriptor(typeElement.asType()))
        .setHasAbstractModifier(isAbstract)
        .setKind(kind)
        .setAnnotation(isAnnotation(typeElement))
        .setCapturingEnclosingInstance(capturesEnclosingInstance((ClassSymbol) typeElement))
        .setFinal(isFinal)
        .setFunctionalInterface(isFunctionalInterface(typeElement.asType()))
        .setJsFunctionInterface(JsInteropUtils.isJsFunction(typeElement))
        .setJsType(JsInteropUtils.isJsType(typeElement))
        .setJsEnumInfo(jsEnumInfo)
        .setNative(JsInteropUtils.isJsNativeType(typeElement))
        .setAnonymous(isAnonymous(typeElement))
        .setLocal(isLocal(typeElement))
        .setSimpleJsName(getJsName(typeElement))
        .setCustomizedJsNamespace(getJsNamespace(typeElement, packageInfoCache))
        .setNullMarked(isNullMarked)
        .setOriginalSimpleSourceName(
            typeElement.getSimpleName() != null ? typeElement.getSimpleName().toString() : null)
        .setPackageName(packageName)
        .setSuperTypeDescriptorFactory(
            () ->
                (DeclaredTypeDescriptor)
                    applyNullabilityAnnotations(
                        createDeclaredTypeDescriptor(typeElement.getSuperclass(), isNullMarked),
                        typeElement,
                        position ->
                            position.type == TargetType.CLASS_EXTENDS && position.type_index == -1))
        .setTypeParameterDescriptors(
            typeParameterElements.stream()
                .map(TypeParameterElement::asType)
                .map(javax.lang.model.type.TypeVariable.class::cast)
                .map(this::createTypeVariable)
                .collect(toImmutableList()))
        .setVisibility(getVisibility(typeElement))
        .setDeclaredMethodDescriptorsFactory(declaredMethods)
        .setDeclaredFieldDescriptorsFactory(declaredFields)
        .setUnusableByJsSuppressed(JsInteropAnnotationUtils.isUnusableByJsSuppressed(typeElement))
        .setDeprecated(isDeprecated(typeElement))
        .build();
  }

  private static boolean isNullMarked(TypeElement classSymbol, PackageInfoCache packageInfoCache) {
    if (packageInfoCache.isNullMarked(
        getBinaryNameFromTypeBinding(toTopLevelTypeBinding(classSymbol)))) {
      // The package is NullMarked, no need to look further.
      return true;
    }

    return hasNullMarkedAnnotation(classSymbol);
  }

  private static boolean hasNullMarkedAnnotation(TypeElement classSymbol) {
    if (classSymbol.getAnnotationMirrors().stream()
        .anyMatch(a -> Nullability.isNullMarkedAnnotation(AnnotationUtils.getAnnotationName(a)))) {
      // The type is NullMarked, no need to look further.
      return true;
    }

    Element enclosingElement = classSymbol.getEnclosingElement();
    return enclosingElement instanceof TypeElement
        && hasNullMarkedAnnotation((TypeElement) enclosingElement);
  }

  private static List<TypeParameterElement> getTypeParameters(TypeElement typeElement) {
    List<TypeParameterElement> typeParameterElements =
        new ArrayList<>(typeElement.getTypeParameters());
    Element currentElement = typeElement;
    Element enclosingElement = typeElement.getEnclosingElement();
    while (enclosingElement != null) {
      if (isStatic(currentElement)) {
        break;
      }

      if (enclosingElement.getKind() != ElementKind.STATIC_INIT
          && enclosingElement.getKind() != ElementKind.INSTANCE_INIT
          && enclosingElement instanceof Parameterizable) {
        // Add the enclosing element type variables, skip STATIC_INIT and INSTANCE_INIT since they
        // never define type variables, and throw NPE if getTypeParameters is called on them.
        typeParameterElements.addAll(((Parameterizable) enclosingElement).getTypeParameters());
      }
      currentElement = enclosingElement;
      enclosingElement = enclosingElement.getEnclosingElement();
    }
    return typeParameterElements;
  }

  public static TypeElement getEnclosingType(Element typeElement) {
    Element enclosing = typeElement.getEnclosingElement();
    while (enclosing != null && !(enclosing instanceof TypeElement)) {
      enclosing = enclosing.getEnclosingElement();
    }
    return (TypeElement) enclosing;
  }

  private static TypeElement getEnclosingType(TypeElement typeElement) {
    Element enclosing = typeElement.getEnclosingElement();
    while (enclosing != null && !(enclosing instanceof TypeElement)) {
      enclosing = enclosing.getEnclosingElement();
    }
    return (TypeElement) enclosing;
  }

  @Nullable
  private TypeMirror getFunctionalInterface(Type type) {
    if (type.isIntersection()) {
      return ((IntersectionType) type)
          .getBounds().stream().filter(this::isFunctionalInterface).findFirst().orElse(null);
    }
    checkArgument(isFunctionalInterface(type));
    return type;
  }

  MethodDescriptor getJsFunctionMethodDescriptor(TypeMirror type) {
    DeclaredTypeDescriptor expressionTypeDescriptor =
        createDeclaredTypeDescriptor(getFunctionalInterface((Type) type));
    return createMethodDescriptor(
        expressionTypeDescriptor,
        (MethodSymbol) getFunctionalInterfaceMethod(type).asMemberOf((Type) type, internalTypes),
        getFunctionalInterfaceMethod(type));
  }

  @Nullable
  private MethodSymbol getFunctionalInterfaceMethodDecl(TypeMirror typeMirror) {
    return Optional.ofNullable(getFunctionalInterfaceMethodPair(typeMirror))
        .map(MethodDeclarationPair::getDeclarationMethodSymbol)
        .orElse(null);
  }

  @Nullable
  private MethodSymbol getFunctionalInterfaceMethod(TypeMirror typeMirror) {
    return Optional.ofNullable(getFunctionalInterfaceMethodPair(typeMirror))
        .map(MethodDeclarationPair::getMethodSymbol)
        .orElse(null);
  }

  @Nullable
  private MethodDeclarationPair getFunctionalInterfaceMethodPair(TypeMirror typeMirror) {
    Type type = (Type) typeMirror;
    if (!internalTypes.isFunctionalInterface(type)) {
      return null;
    }
    if (type.isIntersection()) {

      return ((IntersectionType) type)
          .getBounds().stream()
              .filter(this::isFunctionalInterface)
              .map(this::getFunctionalInterfaceMethodPair)
              .findFirst()
              .orElse(null);
    }
    return getMethods((ClassType) type).stream()
        .filter(
            p ->
                isAbstract(p.getDeclarationMethodSymbol())
                    && !isJavaLangObjectOverride(p.getDeclarationMethodSymbol()))
        .findFirst()
        .orElse(null);
  }

  private boolean isFunctionalInterface(TypeMirror type) {
    return internalTypes.isFunctionalInterface((Type) type)
        && ((Type) type).asElement().getKind() == ElementKind.INTERFACE;
  }

  private static boolean isEnum(TypeElement typeElement) {
    return typeElement.getKind() == ElementKind.ENUM;
  }

  private static boolean isAnnotation(TypeElement typeElement) {
    return typeElement.getKind() == ElementKind.ANNOTATION_TYPE;
  }

  private static boolean isAnonymous(TypeElement typeElement) {
    return typeElement.getNestingKind() == NestingKind.ANONYMOUS;
  }

  private static boolean isClass(TypeElement typeElement) {
    return typeElement.getKind() == ElementKind.CLASS;
  }

  private static boolean isInterface(TypeElement typeElement) {
    return typeElement.getKind() == ElementKind.INTERFACE
        || typeElement.getKind() == ElementKind.ANNOTATION_TYPE;
  }

  private static boolean isLocal(TypeElement typeElement) {
    return typeElement.getNestingKind() == NestingKind.LOCAL;
  }

  public static Visibility getVisibility(Element element) {
    if (element.getModifiers().contains(Modifier.PUBLIC)) {
      return Visibility.PUBLIC;
    } else if (element.getModifiers().contains(Modifier.PROTECTED)) {
      return Visibility.PROTECTED;
    } else if (element.getModifiers().contains(Modifier.PRIVATE)) {
      return Visibility.PRIVATE;
    } else {
      return Visibility.PACKAGE_PRIVATE;
    }
  }

  private static boolean isDeprecated(AnnotatedConstruct binding) {
    return AnnotationUtils.hasAnnotation(binding, Deprecated.class.getName());
  }

  private static boolean isDefaultMethod(Element element) {
    return element.getModifiers().contains(Modifier.DEFAULT);
  }

  private static boolean isAbstract(Element element) {
    return element.getModifiers().contains(Modifier.ABSTRACT);
  }

  private static boolean isFinal(Element element) {
    return element.getModifiers().contains(Modifier.FINAL);
  }

  public static boolean isStatic(Element element) {
    return element.getModifiers().contains(Modifier.STATIC);
  }

  private static boolean isNative(Element element) {
    return element.getModifiers().contains(Modifier.NATIVE);
  }

  private static boolean isSynthetic(Element element) {
    return element instanceof Symbol && (((Symbol) element).flags() & Flags.SYNTHETIC) != 0;
  }
}
