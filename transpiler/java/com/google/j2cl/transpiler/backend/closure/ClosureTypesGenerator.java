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
package com.google.j2cl.transpiler.backend.closure;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Transforms J2cl type abstractions into Closure JavaScript type system abstractions. */
class ClosureTypesGenerator {
  private final ClosureGenerationEnvironment environment;

  ClosureTypesGenerator(ClosureGenerationEnvironment environment) {
    this.environment = environment;
  }

  /**
   * Returns the string representation of a Closure type of a type descriptor for use in a JsDoc
   * annotation.
   */
  public String getClosureTypeString(TypeDescriptor typeDescriptor) {
    checkArgument(!typeDescriptor.isIntersection());
    return getClosureType(typeDescriptor).render();
  }

  /**
   * Returns the {@code JsDoc} annotation for parameter at {@code index} in {@code
   * methodOrFunctionExpression}.
   */
  public String getJsDocForParameter(MethodLike methodLike, int index) {
    MethodDescriptor methodDescriptor = methodLike.getDescriptor();
    ParameterDescriptor parameterDescriptor = methodDescriptor.getParameterDescriptors().get(index);
    return toClosureTypeParameter(
            methodDescriptor, parameterDescriptor, parameterDescriptor.getTypeDescriptor())
        .render();
  }

  /** Returns the Closure type for a type descriptor. */
  private ClosureType getClosureType(TypeDescriptor typeDescriptor) {

    if (typeDescriptor.isPrimitive()) {
      return getClosureTypeForPrimitive((PrimitiveTypeDescriptor) typeDescriptor);
    }

    if (typeDescriptor instanceof TypeVariable) {
      return getClosureTypeForTypeVariable((TypeVariable) typeDescriptor);
    }

    if (typeDescriptor.isArray()) {
      return getClosureTypeForArray((ArrayTypeDescriptor) typeDescriptor);
    }

    if (typeDescriptor.isUnion()) {
      return getClosureTypeForUnion((UnionTypeDescriptor) typeDescriptor);
    }

    if (typeDescriptor.isIntersection()) {
      return getClosureTypeForIntersection((IntersectionTypeDescriptor) typeDescriptor);
    }

    DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;

    // TODO(b/118615488): Surface enum boxed types so that this hack is not needed.
    declaredTypeDescriptor = replaceJsEnumArguments(declaredTypeDescriptor);

    if (declaredTypeDescriptor.isJsFunctionInterface()) {
      return getClosureTypeForJsFunction(declaredTypeDescriptor);
    }

    return withNullability(
        getClosureTypeForDeclaration(
            declaredTypeDescriptor.getTypeDeclaration(),
            getClosureTypes(declaredTypeDescriptor.getTypeArgumentDescriptors())),
        typeDescriptor.isNullable());
  }

  /**
   * Replaces non-native JsEnums by the boxed class counterpart in arguments of types descriptors.
   *
   * <p>The replacement does not need to be done recursively since it happens at the rendering of
   * the type which will be called recursively to render each type argument. This is only done in
   * declared type descriptors; there is no need to replace in places like bounds in type variables
   * since those are not currently supported to be JsEnums.
   */
  private DeclaredTypeDescriptor replaceJsEnumArguments(DeclaredTypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isBoxedEnum(typeDescriptor)) {
      // Don't replace the type parameters if it is already represented as a boxed JsEnum. Boxed
      // JsEnum types might appear already in the AST as a result of transformations performed by
      // passes that surface the actual boxing methods from the runtime library.
      return typeDescriptor;
    }
    ImmutableList<TypeDescriptor> replacedTypeArguments =
        typeDescriptor.getTypeArgumentDescriptors().stream()
            .map(t -> AstUtils.isNonNativeJsEnum(t) ? TypeDescriptors.getEnumBoxType(t) : t)
            .collect(toImmutableList());

    if (replacedTypeArguments.equals(typeDescriptor.getTypeArgumentDescriptors())) {
      // If there was no replacement avoid re-parametrizing the type.
      return typeDescriptor;
    }

    // Construct a map from the type variables that are the type parameters of the class to the new
    // values and specialize to replace the type arguments.
    ImmutableMap<TypeVariable, TypeDescriptor> specializationMap =
        Streams.zip(
                typeDescriptor.getTypeDeclaration().getTypeParameterDescriptors().stream(),
                replacedTypeArguments.stream(),
                Maps::immutableEntry)
            .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    return (DeclaredTypeDescriptor)
        typeDescriptor.getDeclarationDescriptor().specializeTypeVariables(specializationMap);
  }

  /** Returns the Closure type for a primitive type descriptor */
  private ClosureType getClosureTypeForPrimitive(PrimitiveTypeDescriptor typeDescriptor) {

    if (TypeDescriptors.isPrimitiveLong(typeDescriptor)) {
      return getClosureType(BootstrapType.NATIVE_LONG.getDescriptor()).toNonNullable();
    }
    if (TypeDescriptors.isPrimitiveBoolean(typeDescriptor)) {
      return BOOLEAN;
    }
    if (TypeDescriptors.isPrimitiveVoid(typeDescriptor)) {
      return VOID;
    }
    return NUMBER;
  }

  /** Returns the template variable name for a type variable for use in JsDoc annotations. */
  private ClosureType getClosureTypeForTypeVariable(TypeVariable typeVariable) {
    if (typeVariable.isWildcardOrCapture()) {
      return UNKNOWN;
    }

    return new ClosureNamedType(environment.getUniqueNameForVariable(typeVariable.toDeclaration()));
  }

  /** Returns the Closure type for an array type descriptor. */
  private ClosureType getClosureTypeForArray(ArrayTypeDescriptor typeDescriptor) {
    return withNullability(
        new ClosureNamedType("Array", getClosureType(typeDescriptor.getComponentTypeDescriptor())),
        typeDescriptor.isNullable());
  }

  /** Returns the Closure type for a union type descriptor. */
  private ClosureType getClosureTypeForUnion(UnionTypeDescriptor typeDescriptor) {
    return withNullability(
        new ClosureUnionType(getClosureTypes(typeDescriptor.getUnionTypeDescriptors())),
        typeDescriptor.isNullable());
  }

  /** Returns the Closure type for an intersection type descriptor. */
  private ClosureType getClosureTypeForIntersection(IntersectionTypeDescriptor typeDescriptor) {
    // Intersection types do not have an equivalent in the Closure type system. Following the
    // approach that Java takes regarding erasure and intersection types, the Closure type for
    // an intersection type will be the closure type for the first type in the intersection.
    // Whenever there are accesses to members of other types in the intersection, the appropriate
    // Closure casts will be emitted.
    return getClosureType(typeDescriptor.getFirstType());
  }

  /** Returns the Closure type for a @JsFunction type descriptor. */
  private ClosureType getClosureTypeForJsFunction(DeclaredTypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isJsFunctionInterface());

    if (typeDescriptor.isRaw()) {
      // The closure type system treats raw types in a similar manner to Java; where if you use
      // just the type name, all the templates in the type declaration are considered unknown (?).
      // But function types are emitted with type parameters already propagated into the arguments
      // and return type (they are not templated types in closure); So here we construct the Java
      // type with wildcards for each type argument, so that they are propagated in the JsFunction
      // method signature which we then utilize to construct the function type in closure. This in
      // practice results in constructing a "function(...)" type with unknowns (?) in all the right
      // places.
      Set<TypeVariable> typeParameterDescriptors =
          new HashSet<>(typeDescriptor.getTypeDeclaration().getTypeParameterDescriptors());
      typeDescriptor =
          typeDescriptor
              .getDeclarationDescriptor()
              .specializeTypeVariables(
                  t ->
                      typeParameterDescriptors.contains(t)
                          ? TypeVariable.createWildcardWithUpperBound(t.toRawTypeDescriptor())
                          : t);
    }

    MethodDescriptor functionalMethodDescriptor = typeDescriptor.getJsFunctionMethodDescriptor();
    checkState(functionalMethodDescriptor.getTypeParameterTypeDescriptors().isEmpty());
    return withNullability(
        new ClosureFunctionType(
            toClosureTypeParameters(functionalMethodDescriptor),
            getClosureType(functionalMethodDescriptor.getReturnTypeDescriptor())),
        typeDescriptor.isNullable());
  }

  private ImmutableList<ClosureFunctionType.Parameter> toClosureTypeParameters(
      MethodDescriptor methodDescriptor) {
    return methodDescriptor
        .getParameterDescriptors()
        .stream()
        .map(parameterDescriptor -> toClosureTypeParameter(methodDescriptor, parameterDescriptor))
        .collect(toImmutableList());
  }

  private ClosureFunctionType.Parameter toClosureTypeParameter(
      MethodDescriptor methodDescriptor, ParameterDescriptor parameterDescriptor) {
    return toClosureTypeParameter(
        methodDescriptor, parameterDescriptor, parameterDescriptor.getTypeDescriptor());
  }

  private ClosureFunctionType.Parameter toClosureTypeParameter(
      MethodDescriptor methodDescriptor,
      ParameterDescriptor parameterDescriptor,
      TypeDescriptor parameterTypeDescriptor) {
    boolean isJsVarargs = parameterDescriptor.isVarargs() && methodDescriptor.isJsMethodVarargs();
    // Only emit a parameter as optional (i.e. {X=} instead of {X}) if the method is actually
    // a JsMethod or JsFunction. A method might have been marked as a JsMethod but if it overrides
    // a JsMethod specializing the signature, it is emitted as a non-js method with a bridge.
    // This now non-js method would still have the parameter marked as JsOptional (since this is
    // a JsMethod in the source) but it can not be emitted as optional in closure because it might
    // be followed by a regular Java varargs parameter which is not optional nor a js varargs.
    boolean isOptional =
        parameterDescriptor.isJsOptional()
            && (methodDescriptor.isJsMember() || methodDescriptor.isJsFunction());
    parameterTypeDescriptor =
        isJsVarargs
            ? ((ArrayTypeDescriptor) parameterTypeDescriptor).getComponentTypeDescriptor()
            : parameterTypeDescriptor;
    return new ClosureFunctionType.Parameter(
        isJsVarargs, isOptional, getClosureType(parameterTypeDescriptor));
  }

  /** Returns Closure types for collection of type descriptors. */
  private ImmutableList<ClosureType> getClosureTypes(
      Collection<? extends TypeDescriptor> typeDescriptors) {
    return typeDescriptors.stream().map(this::getClosureType).collect(toImmutableList());
  }

  /** Returns the Closure type for a type declaration. */
  private ClosureType getClosureTypeForDeclaration(
      TypeDeclaration typeDeclaration, List<ClosureType> typeParameters) {

    ClosureType closureType = maybeGetStandardClosureType(typeDeclaration);
    if (closureType != null) {
      return closureType;
    }

    TypeDescriptor typeDescriptor = typeDeclaration.toRawTypeDescriptor();
    if (TypeDescriptors.isJavaLangComparable(typeDescriptor)) {
      return new ClosureUnionType(
          new ClosureNamedType(environment.aliasForType(typeDeclaration), typeParameters),
          BOOLEAN,
          NUMBER,
          STRING);
    }

    if (TypeDescriptors.isJavaLangCharSequence(typeDescriptor)) {
      return new ClosureUnionType(
          new ClosureNamedType(environment.aliasForType(typeDeclaration), typeParameters), STRING);
    }

    if (TypeDescriptors.isJavaLangNumber(typeDescriptor)) {
      return new ClosureUnionType(
          new ClosureNamedType(environment.aliasForType(typeDeclaration), typeParameters), NUMBER);
    }

    if (TypeDescriptors.isJavaLangCloneable(typeDescriptor)
        || TypeDescriptors.isJavaIoSerializable(typeDescriptor)) {
      return new ClosureUnionType(
          new ClosureNamedType(environment.aliasForType(typeDeclaration), typeParameters), ARRAY);
    }

    if (specialClosureTypesByName.containsKey(typeDeclaration.getQualifiedJsName())) {
      // Java types are nullable by default as well as most Closure types with some exceptions.
      // Here we represent those type specially so we can handle their nullability properly.
      return specialClosureTypesByName.get(typeDeclaration.getQualifiedJsName());
    }

    if (typeDeclaration.getQualifiedJsName().equals("Object") && typeParameters.size() == 1) {
      // If the type is the native 'Object' class and is being given exactly one type parameter
      // then it is being used as a map. Expand it to two type parameters where the first one
      // (the implicit key type parameter) is a 'string'. This special case exists to replicate the
      // same special case that already exists in the JSCompiler optimizing backend, and to
      // generalize it to work everywhere (including when types are referenced via an alias).
      return new ClosureNamedType(
          environment.aliasForType(typeDeclaration),
          ImmutableList.<ClosureType>builder().add(STRING).addAll(typeParameters).build());
    }

    if (typeDeclaration.isJsEnum()) {
      // We need to be always explicit about Enums nullability since we may not know nullability
      // in the case of native ones and also it is inconsistent when aliased (b/156407201).
      return new ClosureNamedTypeWithUnknownNullability(environment.aliasForType(typeDeclaration))
          .toNullable();
    }

    return new ClosureNamedType(environment.aliasForType(typeDeclaration), typeParameters);
  }

  public static ClosureType maybeGetStandardClosureType(TypeDeclaration typeDeclaration) {
    return closureTypeByTypeDeclaration.get().get(typeDeclaration);
  }

  private static ClosureType withNullability(ClosureType type, boolean nullable) {
    return nullable ? type.toNullable() : type.toNonNullable();
  }

  /**
   * Abstraction of the Closure JavaScript type system.
   *
   * <p>{@see https://github.com/google/closure-compiler/wiki/Types-in-the-Closure-Type-System}
   *
   * <pre>{@code
   * // All types
   *  < type >  := '?'
   *            | < type-expression >
   *
   * // All types except ? which can not be decorated
   *  < type-minus-? > := '*'
   *                  | < primitive-type >                                      // primitive-type
   *                  | < identifier > ( '<' < type > ( ',' < type > )* '>' )?  // named type
   *                  | '!' < type-minus-? >                                    // ! decorated type
   *                  | '?' < type-minus-? >                                    // ? decorated type
   *                  | '(' < type > ('|' < type > ) + ')'                      // union type
   *                  | 'function(' (< type > (',' < type > )*)* '):' < type >  // function type
   *                  | '(' < type-minus-? >  ')'
   * }</pre>
   */
  private abstract static class ClosureType {

    abstract boolean isNullable();

    abstract String render();

    ClosureType toNullable() {
      return isNullable() ? this : new ClosureWildcardDecoratedType(this);
    }

    ClosureType toNonNullable() {
      return isNullable() ? new ClosureBangDecoratedType(this) : this;
    }
  }

  /** Represents primitive types (which includes special constants). */
  private static class ClosurePrimitiveType extends ClosureType {
    private final String type;
    private final boolean isNullable;

    ClosurePrimitiveType(String type, boolean isNullable) {
      this.type = type;
      this.isNullable = isNullable;
    }

    @Override
    boolean isNullable() {
      return isNullable;
    }

    @Override
    String render() {
      return type;
    }
  }

  /** Represents the "untyped" Closure type "?". */
  private static class ClosureUnknownType extends ClosureType {
    @Override
    String render() {
      return "?";
    }

    @Override
    boolean isNullable() {
      return true;
    }

    // The untyped type can not be decorated and is always nullable.
    @Override
    ClosureType toNullable() {
      return this;
    }

    // The untyped type can not be decorated and is always nullable.
    @Override
    ClosureType toNonNullable() {
      return this;
    }
  }

  /** Represents named types which are by default nullable. */
  private static class ClosureNamedType extends ClosureType {
    private final String name;
    private final ImmutableList<ClosureType> typeParameters;

    ClosureNamedType(String name, ClosureType... typeParameters) {
      this(name, Arrays.asList(typeParameters));
    }

    ClosureNamedType(String name, Iterable<ClosureType> typeParameters) {
      this.name = name;
      this.typeParameters = ImmutableList.copyOf(typeParameters);
    }

    @Override
    boolean isNullable() {
      return true;
    }

    @Override
    String render() {
      return name
          + (typeParameters.isEmpty()
              ? ""
              : typeParameters.stream().map(ClosureType::render).collect(joining(", ", "<", ">")));
    }
  }

  private static class ClosureNamedTypeWithUnknownNullability extends ClosureNamedType {
    ClosureNamedTypeWithUnknownNullability(String name) {
      super(name);
    }

    @Override
    ClosureType toNullable() {
      return new ClosureWildcardDecoratedType(this);
    }

    @Override
    ClosureType toNonNullable() {
      return new ClosureBangDecoratedType(this);
    }
  }

  /** Represents union types. */
  private static class ClosureUnionType extends ClosureType {
    private final ImmutableList<ClosureType> types;

    ClosureUnionType(ClosureType... types) {
      this(Arrays.asList(types));
    }

    ClosureUnionType(Iterable<ClosureType> types) {
      this.types = ImmutableList.copyOf(types);
    }

    @Override
    boolean isNullable() {
      return types.stream().anyMatch(ClosureType::isNullable);
    }

    @Override
    String render() {
      return types.stream().map(ClosureType::render).collect(joining("|", "(", ")"));
    }
  }

  /**
   * Represents a type that has been made nullable by adding a "{@code ?}" decoration.
   *
   * <p>Decorating a type by this way, e.g. "{@code ?t}" is equivalent to the union type "{@code
   * (t|null)}" but more economical in code size.
   */
  private static class ClosureWildcardDecoratedType extends ClosureType {
    private final ClosureType type;

    ClosureWildcardDecoratedType(ClosureType type) {
      this.type = type;
    }

    @Override
    boolean isNullable() {
      return true;
    }

    @Override
    String render() {
      return "?" + type.render();
    }

    @Override
    ClosureType toNullable() {
      return this;
    }

    @Override
    ClosureType toNonNullable() {
      return type.toNonNullable();
    }
  }

  /** Represents a type that has been made non-nullable by adding a "{@code !}" decoration. */
  private static class ClosureBangDecoratedType extends ClosureType {
    private final ClosureType type;

    ClosureBangDecoratedType(ClosureType type) {
      this.type = type;
    }

    @Override
    boolean isNullable() {
      return false;
    }

    @Override
    String render() {
      return "!" + type.render();
    }

    @Override
    ClosureType toNullable() {
      return type.toNullable();
    }

    @Override
    ClosureType toNonNullable() {
      return this;
    }
  }

  /** Represents function types. */
  private static class ClosureFunctionType extends ClosureType {
    private static class Parameter {
      private final boolean isVarargs;
      private final boolean isOptional;
      private final ClosureType closureType;

      Parameter(boolean isVarargs, boolean isOptional, ClosureType closureType) {
        checkArgument(!(isVarargs && isOptional));
        this.isVarargs = isVarargs;
        this.isOptional = isOptional;
        this.closureType = closureType;
      }

      String render() {
        Object[] args =
            new Object[] {isVarargs ? "..." : "", closureType.render(), isOptional ? "=" : ""};
        return String.format("%s%s%s", args);
      }
    }

    private final ImmutableList<Parameter> parameters;
    private final ClosureType returnClosureType;

    ClosureFunctionType(Iterable<Parameter> parameters, ClosureType returnClosureType) {
      this.parameters = ImmutableList.copyOf(parameters);
      this.returnClosureType = returnClosureType;
    }

    @Override
    boolean isNullable() {
      return false;
    }

    @Override
    public String render() {
      return String.format(
          "function(%s):%s",
          parameters.stream().map(Parameter::render).collect(joining(", ")),
          returnClosureType.render());
    }
  }

  /* CLOSURE BUILT-IN TYPES */

  private static final ClosurePrimitiveType UNDEFINED =
      new ClosurePrimitiveType("undefined", false);
  private static final ClosurePrimitiveType NULL = new ClosurePrimitiveType("null", true);
  private static final ClosurePrimitiveType STRING = new ClosurePrimitiveType("string", false);
  private static final ClosurePrimitiveType NUMBER = new ClosurePrimitiveType("number", false);
  private static final ClosurePrimitiveType VOID = new ClosurePrimitiveType("void", false);
  private static final ClosurePrimitiveType BOOLEAN = new ClosurePrimitiveType("boolean", false);
  private static final ClosurePrimitiveType ARRAY = new ClosurePrimitiveType("Array", true);
  private static final ClosurePrimitiveType ANY = new ClosurePrimitiveType("*", true);
  private static final ClosureUnknownType UNKNOWN = new ClosureUnknownType();

  /**
   * Map from the strings representing special types (constants, primitive types and untyped) to
   * their corresponding ClosureType to handle JsType(isNative=true) when their names have been
   * explicitly set to one of these.
   */
  private static final ImmutableMap<String, ClosureType> specialClosureTypesByName =
      ImmutableMap.<String, ClosureType>builder()
          .put(UNDEFINED.render(), UNDEFINED)
          .put(NULL.render(), NULL)
          .put(ANY.render(), ANY)
          .put(UNKNOWN.render(), UNKNOWN)
          .put(STRING.render(), STRING)
          .put(NUMBER.render(), NUMBER)
          .put(BOOLEAN.render(), BOOLEAN)
          .put(VOID.render(), VOID)
          .build();

  /**
   * Map from typed eclarations that are mapped into closure native types to the corresponding type
   */
  private static final ThreadLocal<Map<TypeDeclaration, ClosureType>> closureTypeByTypeDeclaration =
      ThreadLocal.withInitial(
          () -> {
            ImmutableMap.Builder<TypeDeclaration, ClosureType> builder =
                ImmutableMap.<TypeDeclaration, ClosureType>builder()
                    .put(
                        TypeDescriptors.get().javaLangObject.getTypeDeclaration(), ANY.toNullable())
                    .put(
                        TypeDescriptors.get().javaLangString.getTypeDeclaration(),
                        STRING.toNullable())
                    .put(
                        TypeDescriptors.get().javaLangDouble.getTypeDeclaration(),
                        NUMBER.toNullable())
                    .put(
                        TypeDescriptors.get().javaLangBoolean.getTypeDeclaration(),
                        BOOLEAN.toNullable())
                    .put(
                        TypeDescriptors.get().javaLangVoid.getTypeDeclaration(), VOID.toNullable());
            DeclaredTypeDescriptor nothing = TypeDescriptors.get().kotlinNothing;
            if (nothing != null) {
              builder.put(
                  TypeDescriptors.get().kotlinNothing.getTypeDeclaration(),
                  UNKNOWN.toNonNullable());
            }
            return builder.build();
          });
}
