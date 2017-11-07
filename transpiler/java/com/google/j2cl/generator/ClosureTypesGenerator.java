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
package com.google.j2cl.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.ast.HasParameters;
import com.google.j2cl.ast.Kind;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.common.J2clUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Transforms J2cl type abstractions into Closure JavaScript type system abstractions. */
class ClosureTypesGenerator {
  private final GenerationEnvironment environment;

  ClosureTypesGenerator(GenerationEnvironment environment) {
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
   * Returns the string representation of a collection of Closure types for use in a JsDoc
   * annotation.
   */
  public String getCommaSeparatedClosureTypesString(Collection<TypeDescriptor> typeDescriptors) {
    return getClosureTypes(typeDescriptors)
        .stream()
        .map(ClosureType::render)
        .collect(Collectors.joining(", "));
  }

  /**
   * Returns the @param JsDoc annotation for parameter at {@code index} in {@code
   * methodOrFunctionExpression}.
   */
  public String getJsDocForParameter(HasParameters hasParameters, int index) {
    MethodDescriptor methodDescriptor = hasParameters.getDescriptor();
    ParameterDescriptor parameterDescriptor = methodDescriptor.getParameterDescriptors().get(index);
    if (methodDescriptor.isJsMethodVarargs() && parameterDescriptor.isVarargs()) {
      // TODO(b/36141178): remove varargs that are typed with a type variable until type checking in
      // jscompiler is fixed. This OTI fix should be moved to FixTypeVariablesInMethods.
      parameterDescriptor = replaceTypeVariables(parameterDescriptor);
    }

    Variable parameter = hasParameters.getParameters().get(index);
    String parameterName = environment.aliasForVariable(parameter);
    return J2clUtils.format(
        "@param {%s} %s",
        toClosureTypeParameter(methodDescriptor, parameterDescriptor).render(), parameterName);
  }

  private ParameterDescriptor replaceTypeVariables(ParameterDescriptor parameterDescriptor) {
    TypeDescriptor parameterTypeDescriptor =
        parameterDescriptor
            .getTypeDescriptor()
            .specializeTypeVariables(
                typeDescriptor ->
                    typeDescriptor.isTypeVariable()
                        ? TypeDescriptor.Builder.from(typeDescriptor)
                            .setKind(Kind.WILDCARD_OR_CAPTURE)
                            .setUniqueKey(typeDescriptor.getUniqueId() + "??")
                            .build()
                        : typeDescriptor);

    return parameterDescriptor.toBuilder().setTypeDescriptor(parameterTypeDescriptor).build();
  }

  /** Returns the Closure type for a type descriptor. */
  private ClosureType getClosureType(TypeDescriptor typeDescriptor) {

    if (typeDescriptor.isPrimitive()) {
      return getClosureTypeForPrimitive(typeDescriptor);
    }

    if (typeDescriptor.isTypeVariable()) {
      return getClosureTypeForTypeVariable(typeDescriptor);
    }

    if (typeDescriptor.isWildCardOrCapture()) {
      return UNKNOWN;
    }

    if (typeDescriptor.isArray()) {
      return getClosureTypeForArray(typeDescriptor);
    }

    if (typeDescriptor.isJsFunctionInterface() || typeDescriptor.isJsFunctionImplementation()) {
      return getClosureTypeForJsFunction(typeDescriptor);
    }

    return withNullability(
        getClosureTypeForDeclaration(
            typeDescriptor.getTypeDeclaration(),
            getClosureTypes(typeDescriptor.getTypeArgumentDescriptors())),
        typeDescriptor.isNullable());
  }

  /** Returns the Closure type for a primitive type descriptor */
  private ClosureType getClosureTypeForPrimitive(TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isPrimitive());

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
  private static ClosureType getClosureTypeForTypeVariable(TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isTypeVariable());
    // Template variable like "C_T".

    // TODO(b/68715725): Clean up naming for type variables so that no special handling is needed
    // here.

    // skip the top level class component for better output readability.
    List<String> classComponents = typeDescriptor.getClassComponents();
    List<String> nameComponents =
        new ArrayList<>(classComponents.subList(1, classComponents.size()));

    // move the prefix in the simple name to the class name to avoid collisions between method-
    // level and class-level type variable and avoid variable name starts with a number.
    // concat class components to avoid collisions between type variables in inner/outer class.
    // use '_' instead of '$' because '$' is not allowed in template variable name in closure.
    String simpleName = typeDescriptor.getSimpleSourceName();
    nameComponents.set(
        nameComponents.size() - 1, simpleName.substring(simpleName.indexOf('_') + 1));
    String prefix = simpleName.substring(0, simpleName.indexOf('_') + 1);

    return new ClosureNamedType(prefix + Joiner.on('_').join(nameComponents));
  }

  /** Returns the Closure type for an array type descriptor. */
  private ClosureType getClosureTypeForArray(TypeDescriptor typeDescriptor) {
    checkArgument(typeDescriptor.isArray());
    return withNullability(
        new ClosureNamedType("Array", getClosureType(typeDescriptor.getComponentTypeDescriptor())),
        typeDescriptor.isNullable());
  }

  /** Returns the Closure type for a @JsFunction type descriptor. */
  private ClosureType getClosureTypeForJsFunction(TypeDescriptor typeDescriptor) {
    checkArgument(
        typeDescriptor.isJsFunctionInterface() || typeDescriptor.isJsFunctionImplementation());
    MethodDescriptor functionalMethodDescriptor =
        typeDescriptor.getConcreteJsFunctionMethodDescriptor();
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
    boolean isJsVarargs = parameterDescriptor.isVarargs() && methodDescriptor.isJsMethodVarargs();
    boolean isOptional = parameterDescriptor.isJsOptional();
    TypeDescriptor parameterTypeDescriptor =
        isJsVarargs
            ? parameterDescriptor.getTypeDescriptor().getComponentTypeDescriptor()
            : parameterDescriptor.getTypeDescriptor();
    return new ClosureFunctionType.Parameter(
        isJsVarargs, isOptional, getClosureType(parameterTypeDescriptor));
  }

  /** Returns Closure types for collection of type descriptors. */
  private ImmutableList<ClosureType> getClosureTypes(Collection<TypeDescriptor> typeDescriptors) {
    return typeDescriptors.stream().map(this::getClosureType).collect(toImmutableList());
  }

  /** Returns the Closure type for a type declaration. */
  private ClosureType getClosureTypeForDeclaration(
      TypeDeclaration typeDeclaration, List<ClosureType> typeParameters) {

    TypeDescriptor typeDescriptor = typeDeclaration.getRawTypeDescriptor();
    if (TypeDescriptors.isJavaLangString(typeDescriptor)) {
      return STRING.toNullable();
    }
    if (TypeDescriptors.isJavaLangObject(typeDescriptor)) {
      return ANY.toNullable();
    }
    if (TypeDescriptors.isJavaLangVoid(typeDescriptor)) {
      return VOID.toNullable();
    }
    if (TypeDescriptors.isJavaLangDouble(typeDescriptor)) {
      return NUMBER.toNullable();
    }
    if (TypeDescriptors.isJavaLangBoolean(typeDescriptor)) {
      return BOOLEAN.toNullable();
    }

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

    return new ClosureNamedType(environment.aliasForType(typeDeclaration), typeParameters);
  }

  private static ClosureType withNullability(ClosureType type, boolean nullable) {
    return nullable ? type.toNullable() : type.toNonNullable();
  }

  /**
   * Abstraction of the Closure JavaScript type system.
   *
   * <p>{@see https://github.com/google/closure-compiler/wiki/Types-in-the-Closure-Type-System}
   *
   * <pre>
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
   * </pre>
   */
  private abstract static class ClosureType {

    abstract boolean isNullable();

    abstract String render();

    abstract ClosureType toNullable();

    abstract ClosureType toNonNullable();
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

    @Override
    ClosureType toNullable() {
      return isNullable ? this : new ClosureWildcardDecoratedType(this);
    }

    @Override
    ClosureType toNonNullable() {
      return isNullable ? new ClosureBangDecoratedType(this) : this;
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

  /** Represents named types which are nullable unless decorated. */
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
              : typeParameters
                  .stream()
                  .map(ClosureType::render)
                  .collect(Collectors.joining(", ", "<", ">")));
    }

    @Override
    ClosureType toNullable() {
      return this;
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
      return types.stream().map(ClosureType::render).collect(Collectors.joining("|", "(", ")"));
    }

    @Override
    ClosureType toNullable() {
      return isNullable() ? this : new ClosureWildcardDecoratedType(this);
    }

    @Override
    ClosureType toNonNullable() {
      return isNullable() ? new ClosureBangDecoratedType(this) : this;
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
        return J2clUtils.format(
            "%s%s%s", isVarargs ? "..." : "", closureType.render(), isOptional ? "=" : "");
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
      return J2clUtils.format(
          "function(%s):%s",
          parameters.stream().map(Parameter::render).collect(Collectors.joining(", ")),
          returnClosureType.render());
    }

    @Override
    ClosureType toNullable() {
      return new ClosureWildcardDecoratedType(this);
    }

    @Override
    ClosureType toNonNullable() {
      return this;
    }
  }

  // Primitive types.
  private static final ClosurePrimitiveType UNDEFINED =
      new ClosurePrimitiveType("undefined", false);
  private static final ClosurePrimitiveType NULL = new ClosurePrimitiveType("null", true);
  private static final ClosurePrimitiveType STRING = new ClosurePrimitiveType("string", false);
  private static final ClosurePrimitiveType NUMBER = new ClosurePrimitiveType("number", false);
  private static final ClosurePrimitiveType VOID = new ClosurePrimitiveType("void", false);
  private static final ClosurePrimitiveType BOOLEAN = new ClosurePrimitiveType("boolean", false);

  /** Special top or any type. */
  private static final ClosurePrimitiveType ANY = new ClosurePrimitiveType("*", true);

  /** Special "Untyped" type. */
  private static final ClosureUnknownType UNKNOWN = new ClosureUnknownType();

  /**
   * Map from the strings representing special types (constants, primitive types and untyped) to
   * their corresponding ClosureType to handle JsType(isNative=true) when their names have been
   * explicitly set to one of these.
   */
  private static final Map<String, ClosureType> specialClosureTypesByName =
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
}
