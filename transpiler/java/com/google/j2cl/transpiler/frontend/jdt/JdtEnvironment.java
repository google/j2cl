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
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.HAS_NO_SIDE_EFFECTS_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.UNCHECKED_CAST_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.WASM_ANNOTATION_NAME;

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
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
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.NullabilityAnnotation;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.PrefixOperator;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDeclaration.Kind;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.Visibility;
import com.google.j2cl.transpiler.frontend.common.Nullability;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
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

/** Environment used to manipulate JDT internal representations. */
public class JdtEnvironment {
  private final Map<ITypeBinding, DeclaredTypeDescriptor>
      cachedDeclaredTypeDescriptorByTypeBindingInNullMarkedScope = new HashMap<>();

  private final Map<ITypeBinding, DeclaredTypeDescriptor>
      cachedDeclaredTypeDescriptorByTypeBindingOutOfNullMarkedScope = new HashMap<>();

  private final Map<ITypeBinding, TypeDeclaration> cachedTypeDeclarationByTypeBinding =
      new HashMap<>();

  private final Map<IMethodBinding, MethodDescriptor> cachedMethodDescriptorByMethodBinding =
      new HashMap<>();

  private final Map<IVariableBinding, FieldDescriptor> cachedFieldDescriptorByVariableBinding =
      new HashMap<>();

  private final PackageAnnotationsResolver packageAnnotationsResolver;

  /**
   * Creates a JdtEnvironment to allow construction of type model objects from Java sources and
   * classfiles.
   *
   * <p>Note that creating the environment also initializes the well known type descriptors, which
   * might be all that is needed by the caller.
   */
  @CanIgnoreReturnValue
  public JdtEnvironment(JdtParser jdtParser, Collection<String> wellKnownTypesBinaryNames) {
    this.packageAnnotationsResolver = PackageAnnotationsResolver.create(Stream.of(), jdtParser);
    this.initWellKnownTypes(jdtParser.resolveBindings(wellKnownTypesBinaryNames));
  }

  public JdtEnvironment(PackageAnnotationsResolver packageAnnotationsResolver) {
    this.packageAnnotationsResolver = packageAnnotationsResolver;
  }

  @Nullable
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

  @Nullable
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

  @Nullable
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

  @Nullable
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

  public Variable createVariable(
      SourcePosition sourcePosition, IVariableBinding variableBinding, boolean inNullMarkedScope) {
    String name = variableBinding.getName();
    TypeDescriptor typeDescriptor =
        createTypeDescriptorWithNullability(
            variableBinding.getType(), variableBinding.getAnnotations(), inNullMarkedScope);
    if (!variableBinding.isParameter()) {
      // In JSpecify, variables do not inherit the nullability from the scope, instead they are
      // conceptually nullable but their nullability is eventually inferred from the assignments.
      typeDescriptor = typeDescriptor.toNullable();
    }
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

  public Expression createFieldAccess(Expression qualifier, IVariableBinding fieldBinding) {
    if (isArrayLengthBinding(fieldBinding)) {
      return ArrayLength.newBuilder().setArrayExpression(qualifier).build();
    }

    return FieldAccess.Builder.from(createFieldDescriptor(fieldBinding))
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
    return JdtAnnotationUtils.hasAnnotation(binding, UNCHECKED_CAST_ANNOTATION_NAME);
  }

  /** Helper method to work around JDT habit of returning raw collections. */
  @SuppressWarnings("rawtypes")
  public static <T> List<T> asTypedList(List jdtRawCollection) {
    @SuppressWarnings("unchecked")
    List<T> typedList = jdtRawCollection;
    return typedList;
  }

  /** Creates a DeclaredTypeDescriptor from a JDT TypeBinding. */
  public DeclaredTypeDescriptor createDeclaredTypeDescriptor(ITypeBinding typeBinding) {
    return createDeclaredTypeDescriptor(typeBinding, /* inNullMarkedScope= */ false);
  }

  /**
   * Creates a DeclaredTypeDescriptor from a JDT TypeBinding applying the nullability rules
   * according to the scope this descriptor appears in.
   */
  public DeclaredTypeDescriptor createDeclaredTypeDescriptor(
      ITypeBinding typeBinding, boolean inNullMarkedScope) {
    return createTypeDescriptor(typeBinding, inNullMarkedScope, DeclaredTypeDescriptor.class);
  }

  /** Creates a DeclaredTypeDescriptor from a JDT TypeBinding. */
  private <T extends TypeDescriptor> T createTypeDescriptor(
      ITypeBinding typeBinding, boolean inNullMarkedScope, Class<T> clazz) {
    return clazz.cast(createTypeDescriptor(typeBinding, inNullMarkedScope));
  }

  /** Creates a TypeDescriptor from a JDT TypeBinding. */
  public TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding) {
    return createTypeDescriptor(typeBinding, /* inNullMarkedScope= */ false);
  }

  /**
   * Creates a TypeDescriptor from a JDT TypeBinding applying the nullability rules according to the
   * scope this descriptor appears in.
   */
  public TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding, boolean inNullMarkedScope) {
    return createTypeDescriptorWithNullability(
        typeBinding, new IAnnotationBinding[0], inNullMarkedScope);
  }

  /** Creates a type descriptor for the given type binding, taking into account nullability. */
  @Nullable
  private TypeDescriptor createTypeDescriptorWithNullability(
      ITypeBinding typeBinding,
      IAnnotationBinding[] elementAnnotations,
      boolean inNullMarkedScope) {
    if (typeBinding == null) {
      return null;
    }

    if (typeBinding.isPrimitive()) {
      return PrimitiveTypes.get(typeBinding.getName());
    }

    if (typeBinding.isNullType()) {
      return TypeDescriptors.get().javaLangObject;
    }

    if (isIntersectionType(typeBinding)) {
      return createIntersectionType(typeBinding, inNullMarkedScope);
    }

    if (typeBinding.isTypeVariable() || typeBinding.isCapture() || typeBinding.isWildcardType()) {
      return createTypeVariable(
          typeBinding,
          inNullMarkedScope,
          getNullabilityAnnotation(typeBinding, elementAnnotations));
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

  private TypeDescriptor createTypeVariable(
      ITypeBinding typeBinding,
      boolean inNullMarkedScope,
      NullabilityAnnotation nullabilityAnnotation) {
    Supplier<TypeDescriptor> upperBoundTypeDescriptorFactory =
        () -> getUpperBoundTypeDescriptor(typeBinding, inNullMarkedScope);

    String uniqueKey = typeBinding.getKey();
    if (typeBinding.isWildcardType() && typeBinding.getBound() != null) {
      // HACK: Use the toString representation of the type but trim it to the first new line. After
      // the newline there is information that is unrelated to the identity of the wildcard that
      // changes throughout the compile. This is a very hacky way to preserve the identity of the
      // wildcards.Also add the nullability of the bound to the key because the same wildcard, e.g.
      // '? extends String` needs to be interpreted different depending on the context in which it
      // appears.
      TypeDescriptor boundTypeDescriptor =
          createTypeDescriptorWithNullability(
              typeBinding.getBound().getErasure(),
              typeBinding.getBound().getTypeAnnotations(),
              inNullMarkedScope);
      uniqueKey +=
          (boundTypeDescriptor.isNullable() ? "?" : "!")
              + (typeBinding.getBound().isUpperbound() ? "_^_" : "_v_")
              + Splitter.on('\n')
                  .splitToStream(typeBinding.getBound().toString())
                  .findFirst()
                  .get();
    }
    return TypeVariable.newBuilder()
        .setUpperBoundTypeDescriptorSupplier(upperBoundTypeDescriptorFactory)
        .setLowerBoundTypeDescriptor(getLowerBoundTypeDescriptor(typeBinding, inNullMarkedScope))
        .setWildcard(typeBinding.isWildcardType())
        .setCapture(typeBinding.isCapture())
        .setUniqueKey(uniqueKey)
        .setName(typeBinding.getName())
        .setKtVariance(KtInteropUtils.getKtVariance(typeBinding.getTypeAnnotations()))
        .setNullabilityAnnotation(nullabilityAnnotation)
        .build();
  }

  @Nullable
  private TypeDescriptor getLowerBoundTypeDescriptor(
      ITypeBinding typeBinding, boolean inNullMarkedScope) {
    // Lower bound of a capture binding must be accessed indirectly through wildcard.
    if (typeBinding.isCapture()) {
      return getLowerBoundTypeDescriptor(typeBinding.getWildcard(), inNullMarkedScope);
    }

    ITypeBinding bound = typeBinding.getBound();
    if (bound != null && !typeBinding.isUpperbound()) {
      return createTypeDescriptorWithNullability(
          bound, bound.getTypeAnnotations(), inNullMarkedScope);
    }

    return null;
  }

  private TypeDescriptor getUpperBoundTypeDescriptor(
      ITypeBinding typeBinding, boolean inNullMarkedScope) {
    if (typeBinding.isWildcardType()) {
      if (isUnbounded(typeBinding)) {
        return TypeDescriptors.get().javaLangObject;
      }

      // For wildcards get the upper bound with getBound() since getTypeBounds() *below* does not
      // return the right type in this case.
      ITypeBinding bound = typeBinding.getBound();
      if (bound != null) {
        TypeDescriptor boundTypeDescriptor =
            createTypeDescriptorWithNullability(
                bound, bound.getTypeAnnotations(), inNullMarkedScope);
        return typeBinding.isUpperbound()
            ? boundTypeDescriptor
            : TypeDescriptors.get()
                .javaLangObject
                // Use the nullability of the lower bound for the upper bound of a lower bounded
                // wildcard.
                .toNullable(boundTypeDescriptor.isNullable());
      }
    }
    ITypeBinding[] bounds = typeBinding.getTypeBounds();
    if (bounds == null || bounds.length == 0) {
      return TypeDescriptors.get().javaLangObject.toNullable(!inNullMarkedScope);
    }
    if (bounds.length == 1) {
      return createTypeDescriptorWithNullability(
          bounds[0], bounds[0].getTypeAnnotations(), inNullMarkedScope);
    }
    return createIntersectionType(typeBinding, inNullMarkedScope);
  }

  /**
   * Tries to distinguish between "? extends Object" and "?" to apply the right nullability to the
   * bound.
   */
  private boolean isUnbounded(ITypeBinding typeBinding) {
    ITypeBinding bound = typeBinding.getBound();

    if (bound != null) {
      return false;
    }

    // For a wildcard .getBound() might be null in scenarios where there is a bound, and in
    // those cases .getTypeBounds() returns the actual bound.
    ITypeBinding[] typeBounds = typeBinding.getTypeBounds();
    if (typeBounds == null || typeBounds.length == 0) {
      return true;
    }

    if (typeBounds.length == 1 && typeBounds[0].getQualifiedName().equals("java.lang.Object")) {
      // This is fragile, but the observation is that in these cases .getBound() is null and
      // there is only one type bound in .getTypeBounds() and that is j.l.Object.
      return true;
    }

    return false;
  }

  private static TypeDescriptor withNullability(TypeDescriptor typeDescriptor, boolean nullable) {
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

    switch (getNullabilityAnnotation(typeBinding, elementAnnotations)) {
      case NULLABLE:
        return true;
      case NOT_NULLABLE:
        return false;
      default:
        return !inNullMarkedScope;
    }
  }

  /** Return whether a type is annotated for nullablility and which type of annotation it has. */
  private static NullabilityAnnotation getNullabilityAnnotation(
      ITypeBinding typeBinding, IAnnotationBinding[] elementAnnotations) {
    checkArgument(!typeBinding.isPrimitive());

    Iterable<IAnnotationBinding> allAnnotations =
        Iterables.concat(
            Arrays.asList(elementAnnotations),
            Arrays.asList(typeBinding.getTypeAnnotations()),
            Arrays.asList(typeBinding.getAnnotations()));
    for (IAnnotationBinding annotation : allAnnotations) {
      String annotationName = annotation.getAnnotationType().getQualifiedName();

      if (Nullability.isNonNullAnnotation(annotationName)) {
        return NullabilityAnnotation.NOT_NULLABLE;
      }

      if (Nullability.isNullableAnnotation(annotationName)) {
        return NullabilityAnnotation.NULLABLE;
      }
    }

    return NullabilityAnnotation.NONE;
  }


  private static boolean isIntersectionType(ITypeBinding binding) {
    return binding.isIntersectionType()
        // JDT returns true for isIntersectionType() for type variables, wildcards and captures
        // with intersection type bounds.
        && !binding.isCapture()
        && !binding.isTypeVariable()
        && !binding.isWildcardType();
  }

  private List<String> getClassComponents(ITypeBinding typeBinding) {
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
  private String getBinaryNameFromTypeBinding(ITypeBinding typeBinding) {
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

  private ImmutableList<TypeDescriptor> getTypeArgumentTypeDescriptors(
      ITypeBinding typeBinding, boolean inNullMarkedScope) {
    return getTypeArgumentTypeDescriptors(typeBinding, inNullMarkedScope, TypeDescriptor.class);
  }

  private <T extends TypeDescriptor> ImmutableList<T> getTypeArgumentTypeDescriptors(
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
  public FieldDescriptor createFieldDescriptor(IVariableBinding variableBinding) {
    FieldDescriptor fieldDescriptor = cachedFieldDescriptorByVariableBinding.get(variableBinding);
    if (fieldDescriptor != null) {
      return fieldDescriptor;
    }

    checkArgument(!isArrayLengthBinding(variableBinding));

    // Always create the field descriptor consistently using the @NullMarked context of the
    // type declaration.
    TypeDeclaration enclosingTypeDeclaration =
        createDeclarationForType(variableBinding.getDeclaringClass().getTypeDeclaration());
    boolean inNullMarkedScope = enclosingTypeDeclaration.isNullMarked();

    boolean isStatic = isStatic(variableBinding);
    Visibility visibility = getVisibility(variableBinding);
    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(variableBinding.getDeclaringClass(), inNullMarkedScope);
    String fieldName = variableBinding.getName();

    TypeDescriptor thisTypeDescriptor =
        createTypeDescriptorWithNullability(
            variableBinding.getType(), variableBinding.getAnnotations(), inNullMarkedScope);

    if (variableBinding.isEnumConstant()) {
      // Enum fields are always non-nullable.
      thisTypeDescriptor = thisTypeDescriptor.toNonNullable();
    }

    FieldDescriptor declarationFieldDescriptor = null;
    if (variableBinding.getVariableDeclaration() != variableBinding) {
      declarationFieldDescriptor = createFieldDescriptor(variableBinding.getVariableDeclaration());
    }

    Object constantValue = variableBinding.getConstantValue();
    boolean isCompileTimeConstant = constantValue != null;
    if (isCompileTimeConstant) {
      thisTypeDescriptor = thisTypeDescriptor.toNonNullable();
    }
    boolean isFinal = isFinal(variableBinding);
    fieldDescriptor =
        FieldDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
            .setName(fieldName)
            .setTypeDescriptor(thisTypeDescriptor)
            .setStatic(isStatic)
            .setVisibility(visibility)
            .setOriginalJsInfo(JsInteropUtils.getJsInfo(variableBinding))
            .setOriginalKtInfo(KtInteropUtils.getKtInfo(variableBinding))
            .setFinal(isFinal)
            .setCompileTimeConstant(isCompileTimeConstant)
            .setConstantValue(
                constantValue != null ? Literal.fromValue(constantValue, thisTypeDescriptor) : null)
            .setDeclarationDescriptor(declarationFieldDescriptor)
            .setEnumConstant(variableBinding.isEnumConstant())
            .setUnusableByJsSuppressed(
                JsInteropAnnotationUtils.isUnusableByJsSuppressed(variableBinding))
            .setDeprecated(isDeprecated(variableBinding))
            .build();
    cachedFieldDescriptorByVariableBinding.put(variableBinding, fieldDescriptor);
    return fieldDescriptor;
  }

  /** Create a MethodDescriptor directly based on the given JDT method binding. */
  @Nullable
  public MethodDescriptor createMethodDescriptor(IMethodBinding methodBinding) {
    if (methodBinding == null) {
      return null;
    }

    MethodDescriptor methodDescriptor = cachedMethodDescriptorByMethodBinding.get(methodBinding);
    if (methodDescriptor != null) {
      return methodDescriptor;
    }

    // Always create the method descriptor consistently using the @NullMarked context of the
    // type declaration.
    TypeDeclaration enclosingTypeDeclaration =
        createDeclarationForType(methodBinding.getDeclaringClass().getTypeDeclaration());
    boolean inNullMarkedScope = enclosingTypeDeclaration.isNullMarked();

    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(methodBinding.getDeclaringClass(), inNullMarkedScope);

    boolean isStatic = isStatic(methodBinding);
    Visibility visibility = getVisibility(methodBinding);
    boolean isDefault = isDefaultMethod(methodBinding);
    JsInfo jsInfo = JsInteropUtils.getJsInfo(methodBinding);
    KtInfo ktInfo = KtInteropUtils.getKtInfo(methodBinding);

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
            : adjustForSyntheticEnumMethod(
                methodBinding,
                createTypeDescriptorWithNullability(
                    methodBinding.getReturnType(),
                    methodBinding.getAnnotations(),
                    inNullMarkedScope));

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
            .transform(
                t ->
                    createTypeDescriptorWithNullability(
                        t, t.getTypeAnnotations(), inNullMarkedScope))
            .transform(TypeVariable.class::cast)
            .transform(TypeVariable::toDeclaration);

    ImmutableList<TypeDescriptor> typeArgumentTypeDescriptors =
        convertTypeArguments(methodBinding.getTypeArguments(), inNullMarkedScope);

    ImmutableList.Builder<ParameterDescriptor> parameterDescriptorBuilder = ImmutableList.builder();
    // The descriptor needs to have the same number of parameters as its declaration, however in
    // some cases with lambdas (see b/231457578) jdt adds synthetic parameters at the beginning.
    // Hence we keep just the last n parameters, where n is the number of parameters in
    // the declaration binding.
    int firstNonSyntheticParameter =
        methodBinding.getParameterTypes().length
            - methodBinding.getMethodDeclaration().getParameterTypes().length;

    for (int i = firstNonSyntheticParameter; i < methodBinding.getParameterTypes().length; i++) {
      TypeDescriptor parameterTypeDescriptor =
          adjustForSyntheticEnumMethod(
              methodBinding,
              createTypeDescriptorWithNullability(
                  methodBinding.getParameterTypes()[i],
                  methodBinding.getParameterAnnotations(i),
                  inNullMarkedScope));

      parameterDescriptorBuilder.add(
          ParameterDescriptor.newBuilder()
              .setTypeDescriptor(parameterTypeDescriptor)
              .setJsOptional(JsInteropUtils.isJsOptional(methodBinding, i))
              .setVarargs(
                  i == methodBinding.getParameterTypes().length - 1 && methodBinding.isVarargs())
              .setDoNotAutobox(JsInteropUtils.isDoNotAutobox(methodBinding, i))
              .build());
    }

    ImmutableList<TypeDescriptor> exceptionTypeDescriptors =
        FluentIterable.from(methodBinding.getExceptionTypes()).stream()
            .map(this::createTypeDescriptor)
            .map(TypeDescriptor::toNonNullable)
            .collect(toImmutableList());

    boolean hasUncheckedCast = hasUncheckedCastAnnotation(methodBinding);
    methodDescriptor =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
            .setName(isConstructor ? null : methodName)
            .setParameterDescriptors(parameterDescriptorBuilder.build())
            .setDeclarationDescriptor(declarationMethodDescriptor)
            .setReturnTypeDescriptor(returnTypeDescriptor)
            .setExceptionTypeDescriptors(exceptionTypeDescriptors)
            .setTypeParameterTypeDescriptors(typeParameterTypeDescriptors)
            .setTypeArgumentTypeDescriptors(typeArgumentTypeDescriptors)
            .setOriginalJsInfo(jsInfo)
            .setOriginalKtInfo(ktInfo)
            .setKtObjcInfo(KtInteropUtils.getKtObjcInfo(methodBinding))
            .setWasmInfo(getWasmInfo(methodBinding))
            .setVisibility(visibility)
            .setStatic(isStatic)
            .setConstructor(isConstructor)
            .setNative(isNative)
            .setFinal(isFinal(methodBinding))
            .setDefaultMethod(isDefault)
            .setAbstract(Modifier.isAbstract(methodBinding.getModifiers()))
            .setSynchronized(Modifier.isSynchronized(methodBinding.getModifiers()))
            .setSynthetic(methodBinding.isSynthetic())
            .setEnumSyntheticMethod(isEnumSyntheticMethod(methodBinding))
            .setUnusableByJsSuppressed(
                JsInteropAnnotationUtils.isUnusableByJsSuppressed(methodBinding))
            .setSideEffectFree(isAnnotatedWithHasNoSideEffects(methodBinding))
            .setDeprecated(isDeprecated(methodBinding))
            .setUncheckedCast(hasUncheckedCast)
            .build();
    cachedMethodDescriptorByMethodBinding.put(methodBinding, methodDescriptor);
    return methodDescriptor;
  }

  /**
   * Makes parameters and returns of the synthetic enum methods ({@code Enum.valueOf} and {@code
   * Enum.values}) non-nullable.
   *
   * <p>Note that non-nullability is also applied to the component of array types to cover the
   * return of {@code Enum.values}.
   */
  private TypeDescriptor adjustForSyntheticEnumMethod(
      IMethodBinding methodBinding, TypeDescriptor typeDescriptor) {
    if (!isEnumSyntheticMethod(methodBinding)) {
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

  private ImmutableList<TypeDescriptor> convertTypeArguments(
      ITypeBinding[] typeArguments, boolean isNullMarked) {
    return JdtEnvironment.<ITypeBinding>asTypedList(Arrays.asList(typeArguments)).stream()
        .map(t -> createTypeDescriptor(t, isNullMarked))
        .collect(toImmutableList());
  }

  private static String getWasmInfo(ITypeBinding binding) {
    return JdtAnnotationUtils.getStringAttribute(
        JdtAnnotationUtils.findAnnotationBindingByName(
            binding.getAnnotations(), WASM_ANNOTATION_NAME),
        "value");
  }

  private static String getWasmInfo(IMethodBinding binding) {
    return JdtAnnotationUtils.getStringAttribute(
        JdtAnnotationUtils.findAnnotationBindingByName(
            binding.getAnnotations(), WASM_ANNOTATION_NAME),
        "value");
  }

  private static boolean isLocal(ITypeBinding typeBinding) {
    return typeBinding.isLocal();
  }

  private <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      List<ITypeBinding> typeBindings, boolean inNullMarkedScope, Class<T> clazz) {
    return typeBindings.stream()
        .map(typeBinding -> createTypeDescriptor(typeBinding, inNullMarkedScope, clazz))
        .collect(toImmutableList());
  }

  private <T extends TypeDescriptor> ImmutableList<T> createTypeDescriptors(
      ITypeBinding[] typeBindings, boolean inNullMarkedScope, Class<T> clazz) {
    return createTypeDescriptors(Arrays.asList(typeBindings), inNullMarkedScope, clazz);
  }

  public void initWellKnownTypes(Iterable<ITypeBinding> typesToResolve) {
    if (TypeDescriptors.isInitialized()) {
      return;
    }
    TypeDescriptors.SingletonBuilder builder = new TypeDescriptors.SingletonBuilder();
    // Add well-known reference types.`
    createDescriptorsFromBindings(typesToResolve).forEach(builder::addReferenceType);
    builder.buildSingleton();
  }

  public List<DeclaredTypeDescriptor> createDescriptorsFromBindings(
      Iterable<ITypeBinding> typeBindings) {
    var builder = ImmutableList.<DeclaredTypeDescriptor>builder();
    for (ITypeBinding typeBinding : typeBindings) {
      builder.add(createDeclaredTypeDescriptor(typeBinding));
    }
    return builder.build();
  }

  private TypeDescriptor createIntersectionType(
      ITypeBinding typeBinding, boolean inNullMarkedScope) {
    // Intersection types created with this method only occur in method bodies, default nullability
    // can be ignored.
    ImmutableList<TypeDescriptor> intersectedTypeDescriptors =
        createTypeDescriptors(typeBinding.getTypeBounds(), inNullMarkedScope, TypeDescriptor.class);
    return IntersectionTypeDescriptor.newBuilder()
        .setIntersectionTypeDescriptors(intersectedTypeDescriptors)
        .build();
  }

  private DeclaredTypeDescriptor createDeclaredType(
      final ITypeBinding typeBinding, boolean inNullMarkedScope) {

    DeclaredTypeDescriptor typeDescriptor = getCachedTypeDescriptor(inNullMarkedScope, typeBinding);
    if (typeDescriptor != null) {
      return typeDescriptor;
    }
    checkArgument(typeBinding.isClass() || typeBinding.isInterface() || typeBinding.isEnum());

    TypeDeclaration typeDeclaration = createDeclarationForType(typeBinding.getTypeDeclaration());

    DeclaredTypeDescriptor enclosingTypeDescriptor =
        createDeclaredTypeDescriptor(typeBinding.getDeclaringClass(), inNullMarkedScope);

    // Compute these even later
    typeDescriptor =
        DeclaredTypeDescriptor.newBuilder()
            .setTypeDeclaration(typeDeclaration)
            .setEnclosingTypeDescriptor(
                enclosingTypeDescriptor != null ? enclosingTypeDescriptor.toNonNullable() : null)
            // Create the super types in the @NullMarked context of the type
            .setSuperTypeDescriptorFactory(
                () ->
                    createDeclaredTypeDescriptor(
                        typeBinding.getSuperclass(), typeDeclaration.isNullMarked()))
            .setInterfaceTypeDescriptorsFactory(
                () ->
                    createTypeDescriptors(
                        typeBinding.getInterfaces(),
                        typeDeclaration.isNullMarked(),
                        DeclaredTypeDescriptor.class))
            .setTypeArgumentDescriptors(
                getTypeArgumentTypeDescriptors(typeBinding, inNullMarkedScope))
            .setDeclaredFieldDescriptorsFactory(
                () -> createFieldDescriptorsOrderedById(typeBinding.getDeclaredFields()))
            .setDeclaredMethodDescriptorsFactory(
                () -> createMethodDescriptors(typeBinding.getDeclaredMethods()))
            .setSingleAbstractMethodDescriptorFactory(() -> getFunctionInterfaceMethod(typeBinding))
            .build();
    putTypeDescriptorInCache(inNullMarkedScope, typeBinding, typeDescriptor);
    return typeDescriptor;
  }

  @Nullable
  private MethodDescriptor getFunctionInterfaceMethod(ITypeBinding typeBinding) {
    IMethodBinding functionalInterfaceMethod = typeBinding.getFunctionalInterfaceMethod();
    if (!typeBinding.isInterface() || functionalInterfaceMethod == null) {
      return null;
    }
    // typeBinding.getFunctionalInterfaceMethod returns in some cases the method declaration
    // instead of the method with the corresponding parameterization. Note: this is observed in
    // the case when a type is parameterized with a wildcard, e.g. JsFunction<?>.
    return createMethodDescriptor(
        Arrays.stream(typeBinding.getDeclaredMethods())
            .filter(
                methodBinding ->
                    methodBinding
                        .getMethodDeclaration()
                        .equals(functionalInterfaceMethod.getMethodDeclaration()))
            .findFirst()
            .orElse(functionalInterfaceMethod));
  }

  private DeclaredTypeDescriptor getCachedTypeDescriptor(
      boolean inNullMarkedScope, ITypeBinding typeBinding) {
    Map<ITypeBinding, DeclaredTypeDescriptor> cache =
        inNullMarkedScope
            ? cachedDeclaredTypeDescriptorByTypeBindingInNullMarkedScope
            : cachedDeclaredTypeDescriptorByTypeBindingOutOfNullMarkedScope;
    return cache.get(typeBinding);
  }

  private void putTypeDescriptorInCache(
      boolean inNullMarkedScope, ITypeBinding typeBinding, DeclaredTypeDescriptor typeDescriptor) {
    Map<ITypeBinding, DeclaredTypeDescriptor> cache =
        inNullMarkedScope
            ? cachedDeclaredTypeDescriptorByTypeBindingInNullMarkedScope
            : cachedDeclaredTypeDescriptorByTypeBindingOutOfNullMarkedScope;
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

  @Nullable
  private String getObjectiveCNamePrefix(ITypeBinding typeBinding) {
    checkArgument(!typeBinding.isPrimitive());
    String objectiveCNamePrefix = KtInteropAnnotationUtils.getKtObjectiveCName(typeBinding);
    boolean isTopLevelType = typeBinding.getDeclaringClass() == null;

    return objectiveCNamePrefix != null || !isTopLevelType
        ? objectiveCNamePrefix
        : packageAnnotationsResolver.getObjectiveCNamePrefix(typeBinding.getPackage().getName());
  }

  @Nullable
  private String getJsNamespace(ITypeBinding typeBinding) {
    checkArgument(!typeBinding.isPrimitive());
    String jsNamespace = JsInteropAnnotationUtils.getJsNamespace(typeBinding);
    boolean isTopLevelType = typeBinding.getDeclaringClass() == null;

    return jsNamespace != null || !isTopLevelType
        ? jsNamespace
        : packageAnnotationsResolver.getJsNameSpace(typeBinding.getPackage().getName());
  }

  @Nullable
  public TypeDeclaration createDeclarationForType(final ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }

    TypeDeclaration typeDeclaration = cachedTypeDeclarationByTypeBinding.get(typeBinding);
    if (typeDeclaration != null) {
      return typeDeclaration;
    }

    checkArgument(typeBinding.getTypeDeclaration() == typeBinding);
    checkArgument(!typeBinding.isArray());
    checkArgument(!typeBinding.isParameterizedType());
    checkArgument(!typeBinding.isTypeVariable());
    checkArgument(!typeBinding.isWildcardType());
    checkArgument(!typeBinding.isCapture());

    // Compute these first since they're reused in other calculations.
    String packageName =
        typeBinding.getPackage() == null ? null : typeBinding.getPackage().getName();
    boolean isAbstract = isAbstract(typeBinding);
    Kind kind = getKindFromTypeBinding(typeBinding);
    // TODO(b/341721484): Even though enums can not have the final modifier, turbine make them final
    // in the header jars.
    boolean isFinal = isFinal(typeBinding) && kind != Kind.ENUM;

    boolean isNullMarked = isNullMarked(typeBinding);
    IBinding declaringMemberBinding = getDeclaringMethodOrFieldBinding(typeBinding);

    typeDeclaration =
        TypeDeclaration.newBuilder()
            .setClassComponents(getClassComponents(typeBinding))
            .setEnclosingTypeDeclaration(createDeclarationForType(typeBinding.getDeclaringClass()))
            .setEnclosingMethodDescriptorFactory(
                () ->
                    createMethodDescriptor(
                        declaringMemberBinding instanceof IMethodBinding
                            ? (IMethodBinding) declaringMemberBinding
                            : null))
            .setSuperTypeDescriptorFactory(
                () -> createDeclaredTypeDescriptor(typeBinding.getSuperclass(), isNullMarked))
            .setInterfaceTypeDescriptorsFactory(
                () ->
                    createTypeDescriptors(
                        typeBinding.getInterfaces(), isNullMarked, DeclaredTypeDescriptor.class))
            .setUnparameterizedTypeDescriptorFactory(
                () -> createDeclaredTypeDescriptor(typeBinding, isNullMarked))
            .setHasAbstractModifier(isAbstract)
            .setKind(kind)
            .setAnnotation(typeBinding.isAnnotation())
            .setCapturingEnclosingInstance(capturesEnclosingInstance(typeBinding))
            .setFinal(isFinal)
            .setFunctionalInterface(
                !typeBinding.isAnnotation() && typeBinding.getFunctionalInterfaceMethod() != null)
            .setJsFunctionInterface(JsInteropUtils.isJsFunction(typeBinding))
            .setAnnotatedWithFunctionalInterface(isAnnotatedWithFunctionalInterface(typeBinding))
            .setAnnotatedWithAutoValue(isAnnotatedWithAutoValue(typeBinding))
            .setAnnotatedWithAutoValueBuilder(isAnnotatedWithAutoValueBuilder(typeBinding))
            .setTestClass(isTestClass(typeBinding))
            .setJsType(JsInteropUtils.isJsType(typeBinding))
            .setJsEnumInfo(JsInteropUtils.getJsEnumInfo(typeBinding))
            .setWasmInfo(getWasmInfo(typeBinding))
            .setNative(JsInteropUtils.isJsNativeType(typeBinding))
            .setAnonymous(typeBinding.isAnonymous())
            .setLocal(isLocal(typeBinding))
            .setSimpleJsName(getJsName(typeBinding))
            .setCustomizedJsNamespace(getJsNamespace(typeBinding))
            .setObjectiveCNamePrefix(getObjectiveCNamePrefix(typeBinding))
            .setKtTypeInfo(KtInteropUtils.getKtTypeInfo(typeBinding))
            .setKtObjcInfo(KtInteropUtils.getKtObjcInfo(typeBinding))
            .setNullMarked(isNullMarked)
            .setOriginalSimpleSourceName(typeBinding.getName())
            .setPackageName(packageName)
            .setTypeParameterDescriptors(
                getTypeArgumentTypeDescriptors(
                        typeBinding, /* inNullMarkedScope= */ isNullMarked, TypeVariable.class)
                    .stream()
                    .map(TypeVariable::toDeclaration)
                    .collect(toImmutableList()))
            .setVisibility(getVisibility(typeBinding))
            .setDeclaredMethodDescriptorsFactory(
                () -> createMethodDescriptors(typeBinding.getDeclaredMethods()))
            .setDeclaredFieldDescriptorsFactory(
                () -> createFieldDescriptorsOrderedById(typeBinding.getDeclaredFields()))
            .setMemberTypeDeclarationsFactory(
                () -> createTypeDeclarations(typeBinding.getDeclaredTypes()))
            .setUnusableByJsSuppressed(
                JsInteropAnnotationUtils.isUnusableByJsSuppressed(typeBinding))
            .setDeprecated(isDeprecated(typeBinding))
            .build();
    cachedTypeDeclarationByTypeBinding.put(typeBinding, typeDeclaration);
    return typeDeclaration;
  }

  private ImmutableList<MethodDescriptor> createMethodDescriptors(IMethodBinding[] methodBindings) {
    return Arrays.stream(methodBindings)
        .map(this::createMethodDescriptor)
        .collect(toImmutableList());
  }

  private ImmutableList<FieldDescriptor> createFieldDescriptorsOrderedById(
      IVariableBinding[] fieldBindings) {
    return Arrays.stream(fieldBindings)
        .sorted(Comparator.comparing(IVariableBinding::getVariableId))
        .map(this::createFieldDescriptor)
        .collect(toImmutableList());
  }

  private ImmutableList<TypeDeclaration> createTypeDeclarations(ITypeBinding[] typeBindings) {
    if (typeBindings == null) {
      return ImmutableList.of();
    }
    return Arrays.stream(typeBindings)
        .map(this::createDeclarationForType)
        .collect(ImmutableList.toImmutableList());
  }

  private boolean isNullMarked(ITypeBinding typeBinding) {
    return hasNullMarkedAnnotation(typeBinding)
        || packageAnnotationsResolver.isNullMarked(typeBinding.getPackage().getName());
  }

  /**
   * Returns true if {@code typeBinding} or one of its enclosing types has a @NullMarked annotation.
   */
  private static boolean hasNullMarkedAnnotation(ITypeBinding typeBinding) {
    if (hasNullMarkedAnnotation(Arrays.stream(typeBinding.getAnnotations()))) {
      return true;
    }
    return typeBinding.getDeclaringClass() != null
        && hasNullMarkedAnnotation(typeBinding.getDeclaringClass());
  }

  private static boolean hasNullMarkedAnnotation(Stream<IAnnotationBinding> annotations) {
    return annotations.anyMatch(
        a -> Nullability.isNullMarkedAnnotation(a.getAnnotationType().getQualifiedName()));
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

  private static boolean isTestClass(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.hasAnnotation(typeBinding, "org.junit.runner.RunWith")
        || JdtAnnotationUtils.hasAnnotation(
            typeBinding, "com.google.apps.xplat.testing.parameterized.RunParameterized");
  }

  private static boolean isAnnotatedWithHasNoSideEffects(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.hasAnnotation(methodBinding, HAS_NO_SIDE_EFFECTS_ANNOTATION_NAME);
  }
}
