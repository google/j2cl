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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.j2cl.transpiler.frontend.common.SupportedAnnotations.isSupportedAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationName;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.hasAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.hasNullMarkedAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropUtils.getJ2ktVariance;
import static com.google.j2cl.transpiler.frontend.javac.JsInteropAnnotationUtils.getJsNamespace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.Annotation;
import com.google.j2cl.transpiler.ast.AnnotationValue;
import com.google.j2cl.transpiler.ast.ArrayConstant;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.JsEnumInfo;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.KtVariance;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.PackageDeclaration;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.DescriptorFactory;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDeclaration.SourceLanguage;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeLiteral;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.Visibility;
import com.google.j2cl.transpiler.frontend.common.Nullability;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.CapturedType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.IntersectionClassType;
import com.sun.tools.javac.code.Type.JCPrimitiveType;
import com.sun.tools.javac.code.Type.TypeVar;
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
import java.util.LinkedHashMap;
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
import javax.lang.model.type.WildcardType;

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
    checkState(!TypeDescriptors.isInitialized());

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
    return switch (operator) {
      case PREFIX_INCREMENT -> PrefixOperator.INCREMENT;
      case PREFIX_DECREMENT -> PrefixOperator.DECREMENT;
      case UNARY_PLUS -> PrefixOperator.PLUS;
      case UNARY_MINUS -> PrefixOperator.MINUS;
      case BITWISE_COMPLEMENT -> PrefixOperator.COMPLEMENT;
      case LOGICAL_COMPLEMENT -> PrefixOperator.NOT;
      default -> null;
    };
  }

  @Nullable
  static PostfixOperator getPostfixOperator(com.sun.source.tree.Tree.Kind operator) {
    return switch (operator) {
      case POSTFIX_INCREMENT -> PostfixOperator.INCREMENT;
      case POSTFIX_DECREMENT -> PostfixOperator.DECREMENT;
      default -> null;
    };
  }

  @Nullable
  static BinaryOperator getBinaryOperator(com.sun.source.tree.Tree.Kind operator) {
    return switch (operator) {
      case ASSIGNMENT -> BinaryOperator.ASSIGN;
      case PLUS_ASSIGNMENT -> BinaryOperator.PLUS_ASSIGN;
      case MINUS_ASSIGNMENT -> BinaryOperator.MINUS_ASSIGN;
      case MULTIPLY_ASSIGNMENT -> BinaryOperator.TIMES_ASSIGN;
      case DIVIDE_ASSIGNMENT -> BinaryOperator.DIVIDE_ASSIGN;
      case AND_ASSIGNMENT -> BinaryOperator.BIT_AND_ASSIGN;
      case OR_ASSIGNMENT -> BinaryOperator.BIT_OR_ASSIGN;
      case XOR_ASSIGNMENT -> BinaryOperator.BIT_XOR_ASSIGN;
      case REMAINDER_ASSIGNMENT -> BinaryOperator.REMAINDER_ASSIGN;
      case LEFT_SHIFT_ASSIGNMENT -> BinaryOperator.LEFT_SHIFT_ASSIGN;
      case RIGHT_SHIFT_ASSIGNMENT -> BinaryOperator.RIGHT_SHIFT_SIGNED_ASSIGN;
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT -> BinaryOperator.RIGHT_SHIFT_UNSIGNED_ASSIGN;
      case AND -> BinaryOperator.BIT_AND;
      case CONDITIONAL_AND -> BinaryOperator.CONDITIONAL_AND;
      case CONDITIONAL_OR -> BinaryOperator.CONDITIONAL_OR;
      case DIVIDE -> BinaryOperator.DIVIDE;
      case EQUAL_TO -> BinaryOperator.EQUALS;
      case GREATER_THAN -> BinaryOperator.GREATER;
      case GREATER_THAN_EQUAL -> BinaryOperator.GREATER_EQUALS;
      case LEFT_SHIFT -> BinaryOperator.LEFT_SHIFT;
      case LESS_THAN -> BinaryOperator.LESS;
      case LESS_THAN_EQUAL -> BinaryOperator.LESS_EQUALS;
      case MINUS -> BinaryOperator.MINUS;
      case MULTIPLY -> BinaryOperator.TIMES;
      case NOT_EQUAL_TO -> BinaryOperator.NOT_EQUALS;
      case OR -> BinaryOperator.BIT_OR;
      case PLUS -> BinaryOperator.PLUS;
      case REMAINDER -> BinaryOperator.REMAINDER;
      case RIGHT_SHIFT -> BinaryOperator.RIGHT_SHIFT_SIGNED;
      case UNSIGNED_RIGHT_SHIFT -> BinaryOperator.RIGHT_SHIFT_UNSIGNED;
      case XOR -> BinaryOperator.BIT_XOR;
      default -> null;
    };
  }

  Variable createVariable(
      SourcePosition sourcePosition,
      VariableElement variableElement,
      boolean isParameter,
      boolean inNullMarkedScope) {
    TypeMirror type = variableElement.asType();
    String name = variableElement.getSimpleName().toString();
    TypeDescriptor typeDescriptor =
        createTypeDescriptorWithNullability(
            type, variableElement.getAnnotationMirrors(), inNullMarkedScope);
    if (!isParameter) {
      // In JSpecify, variables do not inherit the nullability from the scope, instead they are
      // conceptually nullable but their nullability is eventually inferred from the assignments.
      typeDescriptor = typeDescriptor.toNullable();
    }
    boolean isFinal = isFinal(variableElement);
    return Variable.newBuilder()
        .setName(name)
        .setTypeDescriptor(typeDescriptor)
        .setFinal(isFinal)
        .setParameter(isParameter)
        .setSourcePosition(sourcePosition)
        .setAnnotations(createAnnotations(variableElement, inNullMarkedScope))
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
      return createIntersectionType((IntersectionClassType) typeMirror, inNullMarkedScope);
    }

    if (typeMirror.getKind() == TypeKind.UNION) {
      return createUnionType((UnionClassType) typeMirror);
    }

    if (typeMirror.getKind() == TypeKind.NULL) {
      return TypeDescriptors.get().javaLangObject;
    }

    if (typeMirror.getKind() == TypeKind.TYPEVAR) {
      return createTypeVariable(
          (javax.lang.model.type.TypeVariable) typeMirror, elementAnnotations, inNullMarkedScope);
    }

    if (typeMirror.getKind() == TypeKind.WILDCARD) {
      return createWildcard((WildcardType) typeMirror, inNullMarkedScope);
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

  // TODO(b/408478800): Cleanup unique keys for type variables and provide a more meaningful
  // toString().
  private TypeVariable createTypeVariable(
      javax.lang.model.type.TypeVariable typeVariable,
      List<? extends AnnotationMirror> elementAnnotations,
      boolean inNullMarkedScope) {

    Supplier<TypeDescriptor> boundTypeDescriptorFactory =
        () -> createTypeDescriptor(typeVariable.getUpperBound(), inNullMarkedScope);

    TypeDescriptor lowerBound =
        typeVariable.getLowerBound() != null
                && typeVariable.getLowerBound().getKind() != TypeKind.NULL
            ? createTypeDescriptor(typeVariable.getLowerBound(), inNullMarkedScope)
            : null;

    List<String> classComponents = getClassComponents(typeVariable);
    Symbol baseSymbol = ((TypeVariableSymbol) typeVariable.asElement()).baseSymbol();
    // For wildcards occurring in variable declarations, javac creates fresh type variables that
    // are SYNTHETIC that are not instances of CapturedType. (see b/447445848).
    boolean isCapture =
        typeVariable instanceof CapturedType || ((baseSymbol.flags() & Flags.SYNTHETIC) != 0);
    KtVariance ktVariance = getJ2ktVariance(baseSymbol);
    Type baseSymbolType = baseSymbol.asType();
    int id = getTypeVariableId(baseSymbolType);
    return TypeVariable.newBuilder()
        .setUpperBoundTypeDescriptorFactory(boundTypeDescriptorFactory)
        .setLowerBoundTypeDescriptor(lowerBound)
        .setUniqueKey(
            "#"
                + id
                + ":"
                + (isCapture ? "capture of " : "")
                + String.join("::", classComponents)
                + (baseSymbolType.getUpperBound() != null
                    ? "::^::" + baseSymbolType.getUpperBound()
                    : "")
                + (baseSymbolType.getLowerBound() != null
                    ? "::v::" + baseSymbolType.getLowerBound()
                    : ""))
        .setKtVariance(ktVariance)
        .setName(baseSymbol.getSimpleName().toString())
        .setCapture(isCapture)
        .setNullabilityAnnotation(getNullabilityAnnotation(typeVariable, elementAnnotations))
        .build();
  }

  private TypeVariable createWildcard(WildcardType wildcardType, boolean inNullMarkedScope) {
    return createWildcard(wildcardType, null, ImmutableMap.of(), inNullMarkedScope);
  }

  private TypeVariable createUnboundWildcard(
      javax.lang.model.type.TypeVariable declarationTypeVariable,
      Map<TypeVariable, TypeDescriptor> typingContext,
      boolean inNullMarkedScope) {
    return createWildcard(null, declarationTypeVariable, typingContext, inNullMarkedScope);
  }

  private TypeVariable createWildcard(
      WildcardType wildcardType,
      javax.lang.model.type.TypeVariable declarationTypeParameter,
      Map<TypeVariable, TypeDescriptor> typingContext,
      boolean inNullMarkedScope) {
    boolean isUnboundWildcard = wildcardType == null || isUnboundWildcard(wildcardType);

    TypeMirror superBound = isUnboundWildcard ? null : wildcardType.getSuperBound();
    TypeMirror extendsBound = isUnboundWildcard ? null : wildcardType.getExtendsBound();
    String id = getWildcardUniqueKeyPrefix(wildcardType, declarationTypeParameter);

    TypeVariable.DescriptorFactory<TypeDescriptor> upperBoundFactory =
        self -> superBound == null ? createTypeDescriptor(extendsBound, inNullMarkedScope) : null;
    TypeDescriptor lowerBound = createTypeDescriptor(superBound, inNullMarkedScope);
    if (isUnboundWildcard
        && declarationTypeParameter != null
        && !isDefaultUpperbound(declarationTypeParameter.getUpperBound())) {
      // This is an unbound wildcard for a particular type parameter, since javac does not resolve
      // the actual bounds implied by the type parameter, we compute them here.
      //
      // For example consider the class
      //   Enum<T extends Enum<T>>
      // and a reference
      //   Enum<?>
      // the bounds in the wildcard is not present in WildcardType "?" but can be computed from the
      // type variable declaration "T extends Enum<T>" becoming "? extends Enum<?>".
      var typeParameter =
          createTypeVariable(declarationTypeParameter, ImmutableList.of(), inNullMarkedScope);

      // Compute the actual upper bound by using the upper bound in the type parameter declaration
      // "T" and replacing it by the wildcard we are creating here (passed as self).
      upperBoundFactory =
          self -> {
            var upperBoundTypeDescriptor = typeParameter.getUpperBoundTypeDescriptor();
            var specializedBound =
                upperBoundTypeDescriptor.specializeTypeVariables(
                    tv -> {
                      if (typeParameter.toDeclaration() == tv) {
                        return self;
                      }

                      if (typingContext.containsKey(tv)) {
                        return typingContext.get(tv);
                      }

                      return tv;
                    });
            // TODO(b/450611380): Investigate if this is still necessary here and add relevant tests
            // if it is.
            return unravelWildCardsInUpperBound(specializedBound);
          };
    }
    return TypeVariable.newBuilder()
        .setUpperBoundTypeDescriptorFactory(upperBoundFactory)
        .setLowerBoundTypeDescriptor(lowerBound)
        .setWildcard(true)
        .setUnbound(isUnboundWildcard)
        .setName("?")
        .setUniqueKey("#" + id + ":" + (inNullMarkedScope ? "+" : "-"))
        .build();
  }

  /** Removes intermediate wildcards and captures in the computation of an upperbound. */
  private TypeDescriptor unravelWildCardsInUpperBound(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isWildcardOrCapture()) {
      var wildcard = (TypeVariable) typeDescriptor;
      var upperbound = wildcard.getUpperBoundTypeDescriptor();
      return switch (wildcard.getNullabilityAnnotation()) {
        case NULLABLE -> unravelWildCardsInUpperBound(upperbound).toNullable();
        case NOT_NULLABLE -> unravelWildCardsInUpperBound(upperbound).toNonNullable();
        default -> unravelWildCardsInUpperBound(upperbound);
      };
    }

    return typeDescriptor;
  }

  private boolean isUnboundWildcard(WildcardType typeMirror) {
    return isDefaultUpperbound(typeMirror.getExtendsBound()) && typeMirror.getSuperBound() == null;
  }

  private String getWildcardUniqueKeyPrefix(WildcardType wildcard, TypeMirror typeParameter) {
    if (wildcard == null) {
      // We are creating an unbound wildcard for a type parameter, use an unique identifier
      // for it.
      return "unboundFor#" + getTypeVariableId(typeParameter);
    }
    return Integer.toString(getTypeVariableId(wildcard));
  }

  private final Map<TypeMirror, Integer> typeVariableIdByTypeVariable = new HashMap<>();

  private int getTypeVariableId(TypeMirror typeVariable) {
    checkState(
        typeVariable.getKind() == TypeKind.TYPEVAR || typeVariable.getKind() == TypeKind.WILDCARD);
    return typeVariableIdByTypeVariable.computeIfAbsent(
        typeVariable, w -> typeVariableIdByTypeVariable.size());
  }

  private static TypeDescriptor withNullability(TypeDescriptor typeDescriptor, boolean nullable) {
    return nullable ? typeDescriptor.toNullable() : typeDescriptor.toNonNullable();
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
    if (!(element instanceof TypeElement typeElement)) {
      return ImmutableList.of();
    }
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
      classComponents.addFirst(simpleName);
      Element enclosingElement = currentType.getEnclosingElement();
      while (enclosingElement != null && !(enclosingElement instanceof TypeElement)) {
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
        && asTypeElement(methodElement.getParameters().getFirst().asType())
            .getQualifiedName()
            .contentEquals("java.lang.String");
  }

  private static boolean isAnnotationMethod(ExecutableElement executableElement) {
    return executableElement.getEnclosingElement().getKind() == ElementKind.ANNOTATION_TYPE;
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
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(getEnclosingType(variableElement).asType());
    return createFieldDescriptor(enclosingTypeDescriptor, variableElement, type);
  }

  FieldDescriptor createFieldDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      VariableElement variableElement,
      TypeMirror type) {

    boolean isStatic = isStatic(variableElement);
    Visibility visibility = getVisibility(variableElement);
    String fieldName = variableElement.getSimpleName().toString();

    boolean inNullMarkedScope = enclosingTypeDescriptor.getTypeDeclaration().isNullMarked();
    TypeDescriptor thisTypeDescriptor =
        createTypeDescriptorWithNullability(
            type, variableElement.getAnnotationMirrors(), inNullMarkedScope);

    TypeDescriptor declarationTypeDescriptor =
        createTypeDescriptorWithNullability(
            variableElement.asType(), variableElement.getAnnotationMirrors(), inNullMarkedScope);

    FieldDescriptor declarationFieldDescriptor = null;
    if (declarationTypeDescriptor != thisTypeDescriptor || isSpecialized(enclosingTypeDescriptor)) {
      // Field references might be parameterized, and when they are we set the declaration
      // descriptor.
      declarationFieldDescriptor = createFieldDescriptor(variableElement);
      thisTypeDescriptor = propagateNullability(declarationTypeDescriptor, thisTypeDescriptor);
    }

    JsInfo jsInfo = JsInteropUtils.getJsInfo(variableElement);
    KtInfo ktInfo = J2ktInteropUtils.getJ2ktInfo(variableElement);
    Object constantValue = variableElement.getConstantValue();
    boolean isCompileTimeConstant = constantValue != null;
    boolean isEnumConstant = ((VarSymbol) variableElement).isEnum();

    if (isCompileTimeConstant || isEnumConstant) {
      // Enum and compile-time constant fields are always non-nullable.
      thisTypeDescriptor = thisTypeDescriptor.toNonNullable();
    }

    boolean isFinal = isFinal(variableElement);
    boolean isVolatile = isVolatile(variableElement);
    return FieldDescriptor.newBuilder()
        .setDeclarationDescriptor(declarationFieldDescriptor)
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setName(fieldName)
        .setTypeDescriptor(thisTypeDescriptor)
        .setStatic(isStatic)
        .setVisibility(visibility)
        .setOriginalJsInfo(jsInfo)
        .setOriginalKtInfo(ktInfo)
        .setFinal(isFinal)
        .setAnnotations(createAnnotations(variableElement, inNullMarkedScope))
        .setCompileTimeConstant(isCompileTimeConstant)
        .setConstantValue(
            constantValue != null ? Literal.fromValue(constantValue, thisTypeDescriptor) : null)
        .setEnumConstant(isEnumConstant)
        .setVolatile(isVolatile)
        .build();
  }

  /**
   * Applies the nullability annotation present type variables in the declaration to the
   * corresponding substituted type in the reference.
   */
  static TypeDescriptor propagateNullability(
      TypeDescriptor declarationTypeDescriptor, TypeDescriptor referenceTypeDescriptor) {

    return switch (declarationTypeDescriptor) {

      // If the declaration is type variable, apply its nullability annotation if there is one.
      case TypeVariable typeVariable -> {
        if (typeVariable.isAnnotatedNonNullable()) {
          yield referenceTypeDescriptor.toNonNullable();
        } else if (typeVariable.isAnnotatedNullable()) {
          yield referenceTypeDescriptor.toNullable();
        } else {
          yield referenceTypeDescriptor;
        }
      }

      // If the declaration is an array, propagate the nullability on its component.
      case ArrayTypeDescriptor declaration -> {
        var fromRereference = (ArrayTypeDescriptor) referenceTypeDescriptor;
        var declarationComponentTypeDescriptor = declaration.getComponentTypeDescriptor();
        var resultingComponentTypeDescriptor =
            propagateNullability(
                declarationComponentTypeDescriptor, fromRereference.getComponentTypeDescriptor());
        yield ArrayTypeDescriptor.Builder.from(fromRereference)
            .setComponentTypeDescriptor(resultingComponentTypeDescriptor)
            .build();
      }

      // If the declaration is a declared type, propagate the nullability on its type arguments.
      case DeclaredTypeDescriptor declaration -> {
        var fromReference = (DeclaredTypeDescriptor) referenceTypeDescriptor;
        var rewrittenArguments =
            Streams.zip(
                    declaration.getTypeArgumentDescriptors().stream(),
                    fromReference.getTypeArgumentDescriptors().stream(),
                    JavaEnvironment::propagateNullability)
                .collect(toImmutableList());
        yield ((DeclaredTypeDescriptor) referenceTypeDescriptor)
            .getTypeDeclaration()
            .toDescriptor(rewrittenArguments)
            .toNullable(fromReference.isNullable());
      }

      // Nothing to propagate for primitives.
      case PrimitiveTypeDescriptor primitiveTypeDescriptor -> referenceTypeDescriptor;
      default ->
          throw new InternalCompilerError(
              "Unexpected type in declaration: " + declarationTypeDescriptor.getClass());
    };
  }

  /** Create a MethodDescriptor directly based on the given JavaC ExecutableElement. */
  MethodDescriptor createMethodDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      ExecutableType methodType,
      ExecutableElement declarationMethodElement,
      List<TypeDescriptor> typeArguments) {

    // TODO(b/380911302): Remove redundancy in the creation of method descriptors.
    // The enclosing type descriptor might be a subclass of the actual type descriptor, hence
    // traverse the supertypes to find the actual enclosing type descriptor without loosing the
    // parameterization.
    DeclaredTypeDescriptor unparameterizedEnclosingTypeDescriptor =
        fixEnclosingTypeDescriptor(
            createDeclaredTypeDescriptor(
                ((MethodSymbol) declarationMethodElement)
                    .baseSymbol()
                    .getEnclosingElement()
                    .asType()));

    enclosingTypeDescriptor =
        enclosingTypeDescriptor.getAllSuperTypesIncludingSelf().stream()
            .filter(unparameterizedEnclosingTypeDescriptor::isSameBaseType)
            .findFirst()
            .get();

    MethodDescriptor declarationMethodDescriptor = null;

    ImmutableList<TypeMirror> parameters =
        methodType.getParameterTypes().stream().collect(toImmutableList());

    TypeMirror returnType = methodType.getReturnType();
    if (!typeArguments.isEmpty()
        || isSpecialized(
            enclosingTypeDescriptor, declarationMethodElement, parameters, returnType)) {
      declarationMethodDescriptor = createMethodDescriptor(declarationMethodElement);
    } else {
      typeArguments = ImmutableList.of();
    }

    TypeDescriptor returnTypeDescriptor =
        adjustForSyntheticEnumOrAnnotationMethod(
            declarationMethodElement,
            applyReturnTypeNullabilityAnnotations(
                createTypeDescriptorWithNullability(
                    returnType,
                    declarationMethodElement.getAnnotationMirrors(),
                    enclosingTypeDescriptor.getTypeDeclaration().isNullMarked()),
                declarationMethodElement));

    ImmutableList<ParameterDescriptor> parameterDescriptors =
        convertParameterDescriptors(enclosingTypeDescriptor, declarationMethodElement, parameters);

    return createDeclaredMethodDescriptor(
        enclosingTypeDescriptor,
        declarationMethodElement,
        declarationMethodDescriptor,
        parameterDescriptors,
        returnTypeDescriptor,
        typeArguments);
  }

  /** Replace non-existent synthetic enclosing classes with the appropriate class. */
  // TODO(b/443070736): Reconsider whether to just do this in createDeclaredTypeDescriptor.
  static DeclaredTypeDescriptor fixEnclosingTypeDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor) {
    // Methods on array types show an enclosing class of "Array" in the default package.
    if (enclosingTypeDescriptor.getQualifiedSourceName().equals("Array")) {
      // Return java.lang.Object since all methods on arrays are defined in it,
      return TypeDescriptors.get().javaLangObject;
    }
    return enclosingTypeDescriptor;
  }

  private ImmutableList<ParameterDescriptor> convertParameterDescriptors(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      ExecutableElement declarationMethodElement,
      ImmutableList<TypeMirror> parameters) {
    boolean inNullMarkedScope = enclosingTypeDescriptor.getTypeDeclaration().isNullMarked();
    ImmutableList.Builder<ParameterDescriptor> parametersBuilder = ImmutableList.builder();

    for (int i = 0; i < parameters.size(); i++) {
      var parameterAnnotations =
          declarationMethodElement.getParameters().get(i).getAnnotationMirrors();

      TypeDescriptor parameterType =
          adjustForSyntheticEnumOrAnnotationMethod(
              declarationMethodElement,
              applyParameterNullabilityAnnotations(
                  createTypeDescriptorWithNullability(
                      parameters.get(i), parameterAnnotations, inNullMarkedScope),
                  declarationMethodElement,
                  i));

      parametersBuilder.add(
          ParameterDescriptor.newBuilder()
              .setTypeDescriptor(parameterType)
              .setJsOptional(JsInteropUtils.isJsOptional(declarationMethodElement, i))
              .setVarargs(i == parameters.size() - 1 && declarationMethodElement.isVarArgs())
              .setAnnotations(createAnnotations(parameterAnnotations, inNullMarkedScope))
              .build());
    }
    return parametersBuilder.build();
  }

  private TypeDescriptor adjustForSyntheticEnumOrAnnotationMethod(
      ExecutableElement methodSymbol, TypeDescriptor typeDescriptor) {
    if (!isEnumSyntheticMethod(methodSymbol) && !isAnnotationMethod(methodSymbol)) {
      return typeDescriptor;
    }

    if (typeDescriptor.isArray()) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return ArrayTypeDescriptor.newBuilder()
          .setComponentTypeDescriptor(
              arrayTypeDescriptor.getComponentTypeDescriptor().toNonNullable())
          .setNullable(false)
          .build();
    }
    return typeDescriptor.toNonNullable();
  }

  /** Create a MethodDescriptor directly based on the given JavaC ExecutableElement. */
  MethodDescriptor createMethodDescriptor(ExecutableElement methodElement) {
    // Obtain @NullMarked scope from the enclosing type declaration so that both the enclosing type
    // descriptor and the MethodDescriptor are created in the right context.
    TypeElement typeElement = (TypeElement) methodElement.getEnclosingElement();
    boolean inNullMarkedScope = createTypeDeclaration(typeElement).isNullMarked();
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(
            methodElement.getEnclosingElement().asType(), inNullMarkedScope);
    return createMethodDescriptor(
        enclosingTypeDescriptor,
        (ExecutableType) methodElement.asType(),
        methodElement,
        ImmutableList.of());
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Utility methods to process nullability annotations on classes that are compiled separately.
  // Javac does not present TYPE_USE annotation in the returned type instances.
  // TODO(b/443074477): Debug for which cases this is needed. In theory, the original bug in javac
  // was fixed (https://bugs.openjdk.org/browse/JDK-8225377).
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
      if (!positionSelector.test(position)
          // Skip annotations that are on a lambda in the body.
          || position.onLambda != null) {
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
      return withNullability(typeDescriptor, isNullable);
    }
    TypePathEntry currentEntry = location.getFirst();
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
        return declaredTypeDescriptor.withTypeArguments(replacements);
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
        // TODO(b/450914940): Have a more principled approach for applying nullability annotations
        // from declarations to inferred types.
        if (rest.isEmpty()) {
          // Only apply the annotation that is on the bound to the wildcard to work around the
          // issue.
          return withNullability(typeDescriptor, isNullable);
        }
    }
    return typeDescriptor;
  }

  private static int countInner(List<TypePathEntry> rest) {
    return !rest.isEmpty() && rest.getFirst().tag == TypePathEntryKind.INNER_TYPE
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
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      ExecutableElement declarationMethodElement,
      List<? extends TypeMirror> parameters,
      TypeMirror returnType) {
    return !isSameType(returnType, declarationMethodElement.getReturnType())
        || !Streams.zip(
                parameters.stream(),
                declarationMethodElement.getParameters().stream(),
                (thisType, thatType) -> isSameType(thisType, thatType.asType()))
            .allMatch(equals -> equals)
        || isSpecialized(enclosingTypeDescriptor);
  }

  private static boolean isSpecialized(DeclaredTypeDescriptor enclosingTypeDescriptor) {
    return !enclosingTypeDescriptor
        .getTypeArgumentDescriptors()
        .equals(enclosingTypeDescriptor.getTypeDeclaration().getTypeParameterDescriptors());
  }

  private boolean isSameType(TypeMirror thisType, TypeMirror thatType) {
    return internalTypes.isSameType((Type) thisType, (Type) thatType);
  }

  private MethodDescriptor createDeclaredMethodDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor,
      ExecutableElement declarationMethodElement,
      MethodDescriptor declarationMethodDescriptor,
      List<ParameterDescriptor> parameterDescriptors,
      TypeDescriptor returnTypeDescriptor,
      List<TypeDescriptor> typeArguments) {
    boolean inNullMarkedScope = enclosingTypeDescriptor.getTypeDeclaration().isNullMarked();
    ImmutableList<TypeVariable> typeParameterTypeDescriptors =
        declarationMethodElement.getTypeParameters().stream()
            .map(TypeParameterElement::asType)
            .map(javax.lang.model.type.TypeVariable.class::cast)
            .map(tv -> createTypeVariable(tv, ImmutableList.of(), inNullMarkedScope))
            .collect(toImmutableList());

    boolean isStatic = isStatic(declarationMethodElement);
    Visibility visibility = getVisibility(declarationMethodElement);
    boolean isDefault = isDefaultMethod(declarationMethodElement);
    JsInfo jsInfo = JsInteropUtils.getJsInfo(declarationMethodElement);
    KtInfo ktInfo = J2ktInteropUtils.getJ2ktInfo(declarationMethodElement);

    boolean isNative =
        isNative(declarationMethodElement)
            || (!jsInfo.isJsOverlay()
                && enclosingTypeDescriptor.isNative()
                && isAbstract(declarationMethodElement));

    boolean isConstructor = declarationMethodElement.getKind() == ElementKind.CONSTRUCTOR;
    String methodName = declarationMethodElement.getSimpleName().toString();

    var thrownExceptions =
        declarationMethodElement.getThrownTypes().stream()
            .map(this::createTypeDescriptor)
            .collect(toImmutableList());

    // if (methodName.equals("isNull")) {
    //   // debug = true;
    //   var anno = createAnnotations(declarationMethodElement, inNullMarkedScope);
    //   // debug = false;

    //   AnnotationMirror wasmAnnotation =
    //       com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.findAnnotationByName(
    //           declarationMethodElement, "javaemul.internal.annotations.Wasm");
    //   String wasmInfo =
    //       wasmAnnotation == null
    //           ? null
    //           : com.google.j2cl.transpiler.frontend.javac.AnnotationUtils
    //               .getAnnotationParameterString(wasmAnnotation, "value");

    //   System.out.println("isNull found " + anno + " wasmInfo=" + wasmInfo);
    // }

    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setName(isConstructor ? null : methodName)
        .setParameterDescriptors(parameterDescriptors)
        .setDeclarationDescriptor(declarationMethodDescriptor)
        .setReturnTypeDescriptor(isConstructor ? enclosingTypeDescriptor : returnTypeDescriptor)
        .setTypeParameterTypeDescriptors(typeParameterTypeDescriptors)
        .setTypeArgumentTypeDescriptors(typeArguments)
        .setExceptionTypeDescriptors(thrownExceptions)
        .setOriginalJsInfo(jsInfo)
        .setOriginalKtInfo(ktInfo)
        .setVisibility(visibility)
        .setStatic(isStatic)
        .setConstructor(isConstructor)
        .setNative(isNative)
        .setAnnotations(createAnnotations(declarationMethodElement, inNullMarkedScope))
        .setFinal(isFinal(declarationMethodElement))
        .setDefaultMethod(isDefault)
        .setAbstract(isAbstract(declarationMethodElement))
        .setSynchronized(isSynchronized(declarationMethodElement))
        .setSynthetic(isSynthetic(declarationMethodElement))
        .setEnumSyntheticMethod(isEnumSyntheticMethod(declarationMethodElement))
        .build();
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
    if (typeMirror instanceof JCPrimitiveType primitiveType) {
      return primitiveType.asElement();
    }
    if (typeMirror instanceof Type type) {
      return type.tsym;
    }
    return javacTypes.asElement(typeMirror);
  }

  private TypeElement asTypeElement(TypeMirror typeMirror) {
    return (TypeElement) asElement(typeMirror);
  }

  private TypeMirror erasure(TypeMirror typeMirror) {
    return javacTypes.erasure(typeMirror);
  }

  private PackageElement getPackageOf(Element element) {
    return elements.getPackageOf(element);
  }

  private TypeDescriptor createIntersectionType(
      IntersectionClassType intersectionType, boolean inNullMarkedScope) {
    ImmutableList<TypeDescriptor> intersectedTypeDescriptors =
        createTypeDescriptors(
            intersectionType.getBounds(), inNullMarkedScope, TypeDescriptor.class);
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

    TypeElement typeElement = (TypeElement) classType.asElement();
    DeclaredTypeDescriptor typeDescriptor =
        createTypeDeclaration(typeElement)
            .toDescriptor(
                createTypeArgumentDescriptors(
                    getTypeArguments(classType),
                    getTypeParameters(typeElement),
                    inNullMarkedScope));
    putTypeDescriptorInCache(inNullMarkedScope, classType, typeDescriptor);
    return typeDescriptor;
  }

  public ImmutableList<TypeDescriptor> createTypeArgumentDescriptors(
      List<? extends TypeMirror> typeArguments,
      List<? extends TypeParameterElement> declaredTypeParameters,
      boolean inNullMarkedScope) {
    // TODO(b/246332093): Consider doing this in our type model after cleanup. Currently results in
    // an infinite recursion.

    var typeArgumentByTypeVariable = new LinkedHashMap<TypeVariable, TypeDescriptor>();
    for (int i = 0; i < typeArguments.size(); i++) {
      var typeArgument = typeArguments.get(i);
      var typeParameter = (Type.TypeVar) declaredTypeParameters.get(i).asType();

      var typeArgumentDescriptor =
          switch (typeArgument) {
            case WildcardType wildcardType
                when wildcardType.getSuperBound() == null
                    && hasAnnotation(
                        typeParameter.asElement(), "javaemul.internal.annotations.KtIn") ->
                // TODO(b/450403255): Ideally this should be handled in a pass, but since these
                // types can appear anywhere we can hackily handle at type construction time
                // for now.
                createUnboundWildcard(typeParameter, typeArgumentByTypeVariable, inNullMarkedScope);
            case WildcardType wildcardType ->
                createWildcard(
                    wildcardType, typeParameter, typeArgumentByTypeVariable, inNullMarkedScope);
            default -> createTypeDescriptor(typeArgument, inNullMarkedScope);
          };

      typeArgumentByTypeVariable.put(
          createTypeVariable(typeParameter, ImmutableList.of(), inNullMarkedScope).toDeclaration(),
          typeArgumentDescriptor);
    }
    return ImmutableList.copyOf(typeArgumentByTypeVariable.values());
  }

  private boolean isDefaultUpperbound(@Nullable TypeMirror upperbound) {
    if (upperbound == null) {
      return true;
    }
    Element element = asElement(upperbound);
    return element instanceof TypeElement typeElement
        && typeElement.getQualifiedName().contentEquals("java.lang.Object")
        && typeElement.getAnnotationMirrors().isEmpty();
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
      currentType = currentType.getEnclosingType() instanceof DeclaredType type ? type : null;
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
    } else if (isClass(typeElement)
        || isRecord(typeElement)
        || (isEnum(typeElement) && isAnonymous(typeElement))) {
      return Kind.CLASS;
    } else if (isInterface(typeElement)) {
      return Kind.INTERFACE;
    }
    throw new InternalCompilerError("Type binding %s not handled.", typeElement);
  }

  @Nullable
  TypeDeclaration createTypeDeclaration(final TypeElement typeElement) {
    if (typeElement == null) {
      return null;
    }

    // Compute these first since they're reused in other calculations.
    boolean isAbstract = isAbstract(typeElement) && !isInterface(typeElement);
    Kind kind = getKindFromTypeBinding(typeElement);
    boolean isFinal = isFinal(typeElement);

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
            MethodDescriptor methodDescriptor = createMethodDescriptor(methodElement);
            listBuilder.add(methodDescriptor);
          }
          return listBuilder.build();
        };

    DescriptorFactory<MethodDescriptor> singleAbstractMethod =
        typeDeclaration -> {
          if (kind != Kind.INTERFACE) {
            return null;
          }

          // Get the actual abstract method from the frontend; which will return the unparameterized
          // declaration possibly from a supertype.
          var functionalInterfaceMethodDecl =
              getFunctionalInterfaceMethodDecl(typeElement.asType());

          if (functionalInterfaceMethodDecl == null) {
            return null;
          }

          var declaration = createMethodDescriptor(functionalInterfaceMethodDecl);

          // Find the parameterized supertype.
          var parameterizedFunctionalInterface =
              typeDeclaration.toDescriptor().getAllSuperTypesIncludingSelf().stream()
                  .filter(declaration.getEnclosingTypeDescriptor()::isSameBaseType)
                  .collect(onlyElement());

          // Find the parameterized version of the single abstract method in the type.
          return parameterizedFunctionalInterface.getDeclaredMethodDescriptors().stream()
              .filter(m -> m.getDeclarationDescriptor() == declaration)
              .collect(onlyElement());
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

    boolean isNullMarked = isNullMarked(typeElement);
    return TypeDeclaration.newBuilder()
        .setClassComponents(getClassComponents(typeElement))
        .setEnclosingTypeDeclaration(createTypeDeclaration(getEnclosingType(typeElement)))
        .setEnclosingMethodDescriptorFactory(() -> getEnclosingMethodDescriptor(typeElement))
        .setSuperTypeDescriptorFactory(
            () ->
                (DeclaredTypeDescriptor)
                    applyNullabilityAnnotations(
                        createDeclaredTypeDescriptor(typeElement.getSuperclass(), isNullMarked),
                        typeElement,
                        position ->
                            position.type == TargetType.CLASS_EXTENDS
                                && position.type_index == SUPERCLASS_TYPE_INDEX))
        .setInterfaceTypeDescriptorsFactory(
            () ->
                createTypeDescriptors(
                    typeElement.getInterfaces(),
                    isNullMarked,
                    DeclaredTypeDescriptor.class,
                    typeElement))
        .setHasAbstractModifier(isAbstract)
        .setKind(kind)
        .setAnnotation(isAnnotation(typeElement))
        .setCapturingEnclosingInstance(capturesEnclosingInstance((ClassSymbol) typeElement))
        .setFinal(isFinal)
        .setFunctionalInterface(isFunctionalInterface(typeElement.asType()))
        .setJsFunctionInterface(JsInteropUtils.isJsFunction(typeElement))
        .setAnnotationsFactory(() -> createAnnotations(typeElement, isNullMarked))
        .setSourceLanguage(
            isAnnotatedWithKotlinMetadata(typeElement)
                ? SourceLanguage.KOTLIN
                : SourceLanguage.JAVA)
        .setJsType(JsInteropUtils.isJsType(typeElement))
        .setJsEnumInfo(jsEnumInfo)
        .setNative(JsInteropUtils.isJsNativeType(typeElement))
        .setAnonymous(isAnonymous(typeElement))
        // Keep parity with jdt where anonymous classes are also considered local.
        .setLocal(isLocal(typeElement) || isAnonymous(typeElement))
        .setSimpleJsName(JsInteropAnnotationUtils.getJsName(typeElement))
        .setCustomizedJsNamespace(getJsNamespace(typeElement))
        .setObjectiveCNamePrefix(getObjectiveCNamePrefix(typeElement))
        .setKtTypeInfo(J2ktInteropUtils.getJ2ktTypeInfo(typeElement))
        .setNullMarked(isNullMarked)
        .setOriginalSimpleSourceName(
            typeElement.getSimpleName() != null ? typeElement.getSimpleName().toString() : null)
        .setPackage(createPackageDeclaration(getPackageOf(typeElement)))
        .setTypeParameterDescriptors(
            typeParameterElements.stream()
                .map(TypeParameterElement::asType)
                .map(javax.lang.model.type.TypeVariable.class::cast)
                .map(tv -> createTypeVariable(tv, ImmutableList.of(), isNullMarked))
                .collect(toImmutableList()))
        .setVisibility(getVisibility(typeElement))
        .setDeclaredMethodDescriptorsFactory(declaredMethods)
        .setSingleAbstractMethodDescriptorFactory(singleAbstractMethod)
        .setDeclaredFieldDescriptorsFactory(declaredFields)
        .setMemberTypeDeclarationsFactory(
            () ->
                typeElement.getEnclosedElements().stream()
                    .filter(TypeElement.class::isInstance)
                    .map(TypeElement.class::cast)
                    .map(this::createTypeDeclaration)
                    .collect(toImmutableList()))
        .build();
  }

  // The value of type_index for the superclass position in the raw type data of a declaration.
  // Must be in sync with com.sun.tools.javac.code.TypeAnnotationPosition.
  private static final int SUPERCLASS_TYPE_INDEX = 65535;

  @Nullable
  private MethodDescriptor getEnclosingMethodDescriptor(TypeElement typeElement) {
    Element enclosingElement = typeElement.getEnclosingElement();
    if (enclosingElement == null
        || (enclosingElement.getKind() != ElementKind.METHOD
            && enclosingElement.getKind() != ElementKind.CONSTRUCTOR)) {
      return null;
    }

    return createMethodDescriptor((ExecutableElement) enclosingElement);
  }

  private static PackageDeclaration createPackageDeclaration(PackageElement packageElement) {
    // Caching is left to PackageDeclaration.Builder since construction is trivial.
    String packageName = packageElement.getQualifiedName().toString();
    return PackageDeclaration.newBuilder()
        .setName(packageName)
        .setCustomizedJsNamespace(getJsNamespace(packageElement))
        .build();
  }

  private static boolean isNullMarked(Element element) {
    if (hasNullMarkedAnnotation(element)) {
      // The type is NullMarked, no need to look further.
      return true;
    }

    Element enclosingElement = element.getEnclosingElement();
    while (enclosingElement != null
        && !(enclosingElement instanceof TypeElement
            || enclosingElement instanceof PackageElement)) {
      enclosingElement = enclosingElement.getEnclosingElement();
    }

    return enclosingElement != null && isNullMarked(enclosingElement);
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
          && enclosingElement instanceof Parameterizable parameterizable) {
        // Add the enclosing element type variables, skip STATIC_INIT and INSTANCE_INIT since they
        // never define type variables, and throw NPE if getTypeParameters is called on them.
        typeParameterElements.addAll(parameterizable.getTypeParameters());
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
  private MethodSymbol getFunctionalInterfaceMethodDecl(TypeMirror typeMirror) {
    return Optional.ofNullable(getFunctionalInterfaceMethodPair(typeMirror))
        .map(MethodDeclarationPair::getDeclarationMethodSymbol)
        .orElse(null);
  }

  @Nullable
  private MethodDeclarationPair getFunctionalInterfaceMethodPair(TypeMirror typeMirror) {
    Type type = (Type) typeMirror;
    if (!internalTypes.isFunctionalInterface(type)) {
      return null;
    }
    if (type instanceof IntersectionType intersectionType) {
      return intersectionType.getBounds().stream()
          .filter(this::isFunctionalInterface)
          .map(this::getFunctionalInterfaceMethodPair)
          .collect(onlyElement());
    }
    return getMethods((ClassType) type).stream()
        .filter(
            m ->
                isAbstract(m.getDeclarationMethodSymbol())
                    && !isJavaLangObjectOverride(m.getDeclarationMethodSymbol()))
        // There are cases in which the functional interface extends two distinct functional
        // interfaces. In those cases all the methods that remain abstract in this interface must
        // be compatible (i.e. have the same signature in the current parameterization). In this
        // case any of them are suitable abstract methods as the method that implements the
        // functional interface will always override both.
        .findFirst()
        .get();
  }

  @Nullable
  private String getObjectiveCNamePrefix(TypeElement typeElement) {
    // checkArgument(!typeElement.isPrimitive());
    String objectiveCNamePrefix = J2ktInteropAnnotationUtils.getJ2ktObjectiveCName(typeElement);
    boolean isTopLevelType =
        typeElement.getEnclosingElement() == null
            || typeElement.getEnclosingElement() instanceof PackageElement;

    return objectiveCNamePrefix != null || !isTopLevelType
        ? objectiveCNamePrefix
        : J2ktInteropAnnotationUtils.getJ2ktObjectiveCName(getPackageOf(typeElement));
  }

  /** Return whether a type is annotated for nullability and which type of annotation it has. */
  private static NullabilityAnnotation getNullabilityAnnotation(
      AnnotatedConstruct annotatedConstruct, List<? extends AnnotationMirror> elementAnnotations) {

    Iterable<AnnotationMirror> allAnnotations =
        Iterables.concat(elementAnnotations, annotatedConstruct.getAnnotationMirrors());
    for (AnnotationMirror annotation : allAnnotations) {
      String annotationName = getAnnotationName(annotation);

      if (Nullability.isNonNullAnnotation(annotationName)) {
        return NullabilityAnnotation.NOT_NULLABLE;
      }

      if (Nullability.isNullableAnnotation(annotationName)) {
        return NullabilityAnnotation.NULLABLE;
      }
    }

    return NullabilityAnnotation.NONE;
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

  private static boolean isRecord(TypeElement typeElement) {
    return typeElement.getKind() == ElementKind.RECORD;
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

  private static boolean isDefaultMethod(Element element) {
    return element.getModifiers().contains(Modifier.DEFAULT);
  }

  private static boolean isAbstract(Element element) {
    return element.getModifiers().contains(Modifier.ABSTRACT);
  }

  private static boolean isFinal(Element element) {
    return element.getModifiers().contains(Modifier.FINAL);
  }

  private static boolean isVolatile(Element element) {
    return element.getModifiers().contains(Modifier.VOLATILE);
  }

  public static boolean isStatic(Element element) {
    return element.getModifiers().contains(Modifier.STATIC);
  }

  public static boolean isSynchronized(Element element) {
    return element.getModifiers().contains(Modifier.SYNCHRONIZED);
  }

  private static boolean isNative(Element element) {
    return element.getModifiers().contains(Modifier.NATIVE);
  }

  private static boolean isSynthetic(Element element) {
    return element instanceof Symbol s && (s.flags() & Flags.SYNTHETIC) != 0;
  }

  private static boolean isAnnotatedWithKotlinMetadata(Element element) {
    return hasAnnotation(element, "kotlin.Metadata");
  }

  private ImmutableList<Annotation> createAnnotations(Element element, boolean inNullMarkedScope) {
    return createAnnotations(element.getAnnotationMirrors(), inNullMarkedScope);
  }

  private ImmutableList<Annotation> createAnnotations(
      List<? extends AnnotationMirror> annotations, boolean inNullMarkedScope) {
    return annotations.stream()
        .filter(annotationMirror -> isSupportedAnnotation(getAnnotationName(annotationMirror)))
        .map(
            annotationMirror ->
                newAnnotationBuilder(annotationMirror.getElementValues(), inNullMarkedScope)
                    .setTypeDescriptor(
                        createDeclaredType(annotationMirror.getAnnotationType(), inNullMarkedScope))
                    .build())
        .collect(toImmutableList());
  }

  private Annotation.Builder newAnnotationBuilder(
      Map<? extends ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> values,
      boolean inNullMarkedScope) {
    Annotation.Builder annotationBuilder = Annotation.newBuilder();
    for (var valuePair : values.entrySet()) {
      TypeDescriptor elementType =
          createTypeDescriptor(valuePair.getKey().getReturnType(), inNullMarkedScope);
      var translatedValue =
          createAnnotationValue(elementType, valuePair.getValue().getValue(), inNullMarkedScope);
      if (translatedValue == null) {
        continue;
      }
      annotationBuilder.addValue(valuePair.getKey().getSimpleName().toString(), translatedValue);
    }
    return annotationBuilder;
  }

  /**
   * Creates a literal for the given annotation member value.
   *
   * <p>If the value type is not supported, returns {@code null}. TODO(b/397460318, b/395716783):
   * Remove the null return once we handle all member value types.
   */
  @Nullable
  private AnnotationValue createAnnotationValue(
      TypeDescriptor elementType, Object value, boolean inNullMarkedScope) {
    if (TypeDescriptors.isBoxedOrPrimitiveType(elementType)
        || TypeDescriptors.isJavaLangString(elementType)) {
      return Literal.fromValue(value, elementType);
    } else if (TypeDescriptors.isJavaLangClass(elementType)) {
      return new TypeLiteral(
          SourcePosition.NONE, createTypeDescriptor((TypeMirror) value, inNullMarkedScope));
    } else if (elementType.isArray()) {
      List<AnnotationValue> values =
          ((List<?>) value)
              .stream()
                  .map(
                      v ->
                          createAnnotationValue(
                              ((ArrayTypeDescriptor) elementType).getComponentTypeDescriptor(),
                              ((javax.lang.model.element.AnnotationValue) v).getValue(),
                              inNullMarkedScope))
                  .collect(toImmutableList());
      // TODO(b/397460318, b/395716783): Remove this null check once we handle all member value
      // types. We don't expect null unless it's an unhandled value type.
      if (values.contains(null)) {
        return null;
      }
      return ArrayConstant.newBuilder()
          .setTypeDescriptor((ArrayTypeDescriptor) elementType)
          .setValueExpressions(values)
          .build();
    }
    // TODO(b/397460318, b/395716783): Implement various member value types, then throw an exception
    // here if unhandled.
    return null;
  }

  // TODO(b/392124958): Remove this method that was copied from errorprone once we
  // depend on errorprone.
  /**
   * Returns the mapping between type variables and their instantiations in the given type. For
   * example, the instantiation of {@code Map<K, V>} as {@code Map<String, Integer>} would be
   * represented as a {@code TypeSubstitution} from {@code [K, V]} to {@code [String, Integer]}.
   */
  public static ImmutableListMultimap<TypeVariableSymbol, Type> getTypeSubstitution(
      Type type, Symbol sym) {
    ImmutableListMultimap.Builder<Symbol.TypeVariableSymbol, Type> result =
        ImmutableListMultimap.builder();

    Multimap<Type, Type> visited = HashMultimap.create();

    class Visitor extends Types.DefaultTypeVisitor<Void, Type> {

      @Override
      public Void visitMethodType(Type.MethodType t, Type other) {
        scan(t.getParameterTypes(), other.getParameterTypes());
        scan(t.getThrownTypes(), other.getThrownTypes());
        scan(t.getReturnType(), other.getReturnType());
        return null;
      }

      @Override
      public Void visitClassType(ClassType t, Type other) {
        scan(t.getTypeArguments(), other.getTypeArguments());
        return null;
      }

      @Override
      public Void visitTypeVar(TypeVar t, Type other) {
        if (!visited.put(t, other)) {
          // The pair has been visited before, nothing to do.
          return null;
        }

        result.put((Symbol.TypeVariableSymbol) t.asElement(), other);

        // Retrieve potential parameterizations from the bound of the type variable.
        if (other instanceof TypeVar otherTypeVar) {
          // If the substitution is also a type variable then use the bounds.
          scan(t.getUpperBound(), otherTypeVar.getUpperBound());
        } else {
          // Otherwise just use the actual type.
          scan(t.getUpperBound(), other);
        }
        return null;
      }

      @Override
      public Void visitForAll(Type.ForAll t, Type other) {
        scan(t.getParameterTypes(), other.getParameterTypes());
        scan(t.getThrownTypes(), other.getThrownTypes());
        scan(t.getReturnType(), other.getReturnType());
        return null;
      }

      @Override
      public Void visitWildcardType(Type.WildcardType t, Type type) {
        if (type instanceof Type.WildcardType other) {
          scan(t.getExtendsBound(), other.getExtendsBound());
          scan(t.getSuperBound(), other.getSuperBound());
        }
        return null;
      }

      @Override
      public Void visitArrayType(Type.ArrayType t, Type type) {
        scan(t.elemtype, ((Type.ArrayType) type).elemtype);
        return null;
      }

      @Override
      public Void visitType(Type t, Type other) {
        return null;
      }

      private void scan(Collection<Type> from, Collection<Type> to) {
        Streams.forEachPair(from.stream(), to.stream(), this::scan);
      }

      private void scan(Type from, Type to) {
        if (from != null && to != null) {
          from.accept(this, to);
        }
      }
    }
    sym.asType().accept(new Visitor(), type);
    return result.build();
  }
}
