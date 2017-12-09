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
package com.google.j2cl.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A usage-site reference to a declared type, i.e. a class, an interface, an enum, a type variable,
 * or a wildcard.
 *
 * <p>Some properties are lazily calculated since type relationships are a graph (not a tree) and
 * this class is a value type. Those properties are set through {@code DescriptorFactory}.
 */
@AutoValue
@Visitable
public abstract class DeclaredTypeDescriptor extends TypeDescriptor
    implements HasUnusableByJsSuppression {

  /**
   * References to some descriptors need to be deferred in some cases since it will cause infinite
   * loops.
   */
  public interface DescriptorFactory<T> {
    T get(DeclaredTypeDescriptor typeDescriptor);
  }

  @Override
  public boolean hasSameRawType(TypeDescriptor other) {
    // TODO(rluble): compare using toRawTypeDescriptor once raw TypeDescriptors are constructed
    // correctly. Raw TypeDescriptors are constructed in one of two ways, 1) from a JDT RAW
    // TypeDescriptor and 2) from a TypeDescriptor by removing type variables. These two ways are
    // not consistent, in particular the second form does not propagate the removal of type
    // variables inward. These two construction end up with different data but with the same unique
    // id, so the first one that is constructed will be interned and used everywhere.
    // Using toRawTypeDescriptor here triggers the second (incorrect) construction and causes
    // the wrong information be used in some cases.

    // For type variables, wildcards and captures we still need to do toRawTypeDescriptor to get
    // the bound.
    TypeDescriptor thisTypeDescriptor =
        isTypeVariable() || isWildCardOrCapture() ? toRawTypeDescriptor() : this;
    other =
        other.isTypeVariable() || other.isWildCardOrCapture() ? other.toRawTypeDescriptor() : other;
    return thisTypeDescriptor.getQualifiedSourceName().equals(other.getQualifiedSourceName());
  }

  @Override
  public boolean isUnusableByJsSuppressed() {
    return getTypeDeclaration().isUnusableByJsSuppressed();
  }

  @Override
  public abstract ImmutableList<String> getClassComponents();

  @Nullable
  public abstract DeclaredTypeDescriptor getEnclosingTypeDescriptor();

  public abstract ImmutableList<TypeDescriptor> getTypeArgumentDescriptors();

  public boolean isFinal() {
    return getTypeDeclaration().isFinal();
  }

  public boolean isFunctionalInterface() {
    return hasTypeDeclaration() && getTypeDeclaration().isFunctionalInterface();
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return hasTypeDeclaration() && getTypeDeclaration().isJsFunctionImplementation();
  }

  @Override
  public boolean isJsFunctionInterface() {
    return hasTypeDeclaration() && getTypeDeclaration().isJsFunctionInterface();
  }

  @Override
  public boolean isNative() {
    return hasTypeDeclaration() && getTypeDeclaration().isNative();
  }

  @Override
  public abstract boolean isNullable();

  public boolean hasJsConstructor() {
    return getTypeDeclaration().hasJsConstructor();
  }

  /* PRIVATE AUTO_VALUE PROPERTIES */

  @Nullable
  abstract String getUniqueKey();

  abstract Kind getKind();

  @Nullable
  abstract DescriptorFactory<ImmutableMap<String, MethodDescriptor>>
      getDeclaredMethodDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<MethodDescriptor> getSingleAbstractMethodDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<MethodDescriptor> getJsFunctionMethodDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<FieldDescriptor>> getDeclaredFieldDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<DeclaredTypeDescriptor>>
      getInterfaceTypeDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<DeclaredTypeDescriptor> getRawTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<TypeDescriptor> getBoundTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<DeclaredTypeDescriptor> getSuperTypeDescriptorFactory();

  public boolean hasTypeArguments() {
    return !getTypeArgumentDescriptors().isEmpty();
  }

  @Override
  public boolean isTypeVariable() {
    return getKind() == Kind.TYPE_VARIABLE;
  }

  @Override
  public boolean isWildCardOrCapture() {
    return getKind() == Kind.WILDCARD_OR_CAPTURE;
  }

  /** Returns whether the described type is a class. */
  @Override
  public boolean isClass() {
    return getKind() == Kind.CLASS;
  }

  /** Returns whether the described type is an interface. */
  @Override
  public boolean isInterface() {
    return getKind() == Kind.INTERFACE;
  }

  /** Returns whether the described type is an enum. */
  public boolean isEnum() {
    return getKind() == Kind.ENUM;
  }

  /**
   * Returns a list of the type descriptors of interfaces that are explicitly implemented directly
   * on this type.
   */
  @Memoized
  public ImmutableList<DeclaredTypeDescriptor> getInterfaceTypeDescriptors() {
    return getInterfaceTypeDescriptorsFactory().get(this);
  }

  /**
   * Returns a set of the type descriptors of interfaces that are explicitly implemented either
   * directly on this type or on some super type or super interface.
   */
  @Memoized
  public Set<DeclaredTypeDescriptor> getTransitiveInterfaceTypeDescriptors() {
    Set<DeclaredTypeDescriptor> typeDescriptors = new LinkedHashSet<>();

    // Recursively gather from super interfaces.
    for (DeclaredTypeDescriptor interfaceTypeDescriptor : getInterfaceTypeDescriptors()) {
      typeDescriptors.add(interfaceTypeDescriptor);
      typeDescriptors.addAll(interfaceTypeDescriptor.getTransitiveInterfaceTypeDescriptors());
    }

    // Recursively gather from super type.
    DeclaredTypeDescriptor superTypeDescriptor = getSuperTypeDescriptor();
    if (superTypeDescriptor != null) {
      typeDescriptors.addAll(superTypeDescriptor.getTransitiveInterfaceTypeDescriptors());
    }

    return typeDescriptors;
  }

  @Override
  @Memoized
  @Nullable
  public DeclaredTypeDescriptor toRawTypeDescriptor() {
    return getRawTypeDescriptorFactory().get(this);
  }

  @Memoized
  @Nullable
  public TypeDescriptor getBoundTypeDescriptor() {
    checkState(isTypeVariable() || isWildCardOrCapture());
    TypeDescriptor boundTypeDescriptor = getBoundTypeDescriptorFactory().get(this);
    return boundTypeDescriptor != null ? boundTypeDescriptor : TypeDescriptors.get().javaLangObject;
  }

  @Nullable
  @Memoized
  public MethodDescriptor getSingleAbstractMethodDescriptor() {
    return getSingleAbstractMethodDescriptorFactory().get(this);
  }

  /** Returns the single declared constructor fo this class. */
  @Memoized
  public MethodDescriptor getSingleConstructor() {
    return getDeclaredMethodDescriptors()
        .stream()
        .filter(MethodDescriptor::isConstructor)
        .collect(MoreCollectors.onlyElement());
  }

  @Memoized
  @Nullable
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return getJsFunctionMethodDescriptorFactory().get(this);
  }

  @Override
  @Memoized
  public DeclaredTypeDescriptor getFunctionalInterface() {
    return isFunctionalInterface()
        ? this
        : getInterfaceTypeDescriptors()
            .stream()
            .filter(DeclaredTypeDescriptor::isFunctionalInterface)
            .findFirst()
            .orElse(null);
  }

  @Override
  @Memoized
  public TypeDeclaration getMetadataTypeDeclaration() {
    DeclaredTypeDescriptor rawTypeDescriptor = toRawTypeDescriptor();

    if (rawTypeDescriptor.isNative()) {
      return TypeDescriptors.createOverlayImplementationTypeDeclaration(rawTypeDescriptor);
    }

    if (rawTypeDescriptor.isJsFunctionInterface()) {
      return BootstrapType.JAVA_SCRIPT_FUNCTION.getDeclaration();
    }

    return rawTypeDescriptor.getTypeDeclaration();
  }

  @Memoized
  @Override
  public DeclaredTypeDescriptor toUnparameterizedTypeDescriptor() {
    return getTypeDeclaration().toUnparamterizedTypeDescriptor();
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    TypeDescriptor thatRawTypeDescriptor = that.toRawTypeDescriptor();
    return thatRawTypeDescriptor instanceof DeclaredTypeDescriptor
        && isSubtypeOf(thatRawTypeDescriptor);
  }

  private boolean isSubtypeOf(TypeDescriptor that) {
    return getRawSuperTypesIncludingSelf().contains(that.toRawTypeDescriptor());
  }

  private Set<DeclaredTypeDescriptor> getRawSuperTypesIncludingSelf() {
    Set<DeclaredTypeDescriptor> allRawSupertypesIncludingSelf = new LinkedHashSet<>();
    allRawSupertypesIncludingSelf.add(toRawTypeDescriptor());
    if (getSuperTypeDescriptor() != null) {
      allRawSupertypesIncludingSelf.addAll(
          getSuperTypeDescriptor().getRawSuperTypesIncludingSelf());
    }
    for (DeclaredTypeDescriptor interfaceTypeDescriptor : getInterfaceTypeDescriptors()) {
      allRawSupertypesIncludingSelf.addAll(interfaceTypeDescriptor.getRawSuperTypesIncludingSelf());
    }
    return allRawSupertypesIncludingSelf;
  }

  @Override
  @Memoized
  public Set<TypeDescriptor> getAllTypeVariables() {
    Set<TypeDescriptor> typeVariables = new LinkedHashSet<>();
    collectAllTypeVariables(this, typeVariables);
    return typeVariables;
  }

  private static void collectAllTypeVariables(
      DeclaredTypeDescriptor typeDescriptor, Set<TypeDescriptor> typeVariables) {
    if (typeDescriptor.isTypeVariable()) {
      typeVariables.add(typeDescriptor);
    }
    for (TypeDescriptor typeArgumentTypeDescriptor : typeDescriptor.getTypeArgumentDescriptors()) {
      typeVariables.addAll(typeArgumentTypeDescriptor.getAllTypeVariables());
    }

    if (typeDescriptor.isJsFunctionInterface() || typeDescriptor.isJsFunctionImplementation()) {
      // Type variables that might appear referenced in a JsDoc annotation might need to be
      // processed by passes that handle incompatibilities with Closure Type system. The closure
      // type for JsFunction interfaces and implementations includes the types referenced
      // by the functional methods which might introduce additional type variables, so those
      // need to be included.
      for (TypeDescriptor jsFunctionTypeParameter :
          typeDescriptor.getJsFunctionMethodDescriptor().getTypeParameterTypeDescriptors()) {
        typeVariables.addAll(jsFunctionTypeParameter.getAllTypeVariables());
      }
    }
  }


  /**
   * Returns the qualified JavaScript name of the type. Same as {@link #getQualifiedSourceName}
   * unless it is modified by JsType/JsPackage.
   *
   * <p>This is used for driving module name (hence importing etc.), long alias name, mangled name
   * generation and other similar scenarios.
   */
  public String getQualifiedJsName() {
    return getTypeDeclaration().getQualifiedJsName();
  }

  @Memoized
  @Override
  public String getQualifiedSourceName() {
    if (hasTypeDeclaration()) {
      return getTypeDeclaration().getQualifiedSourceName();
    }
    return super.getQualifiedSourceName();
  }

  @Override
  @Memoized
  public String getQualifiedBinaryName() {
    if (hasTypeDeclaration()) {
      return getTypeDeclaration().getQualifiedBinaryName();
    }
    return getSimpleBinaryName();
  }

  @Memoized
  @Nullable
  public DeclaredTypeDescriptor getSuperTypeDescriptor() {
    return getSuperTypeDescriptorFactory().get(this);
  }

  @Nullable
  public abstract TypeDeclaration getTypeDeclaration();

  private boolean hasTypeDeclaration() {
    return getTypeDeclaration() != null;
  }

  @Memoized
  @Override
  public String getUniqueId() {
    String uniqueKey = MoreObjects.firstNonNull(getUniqueKey(), getQualifiedBinaryName());
    String prefix = isNullable() ? "?" : "!";
    return prefix + uniqueKey + createTypeArgumentsUniqueId(getTypeArgumentDescriptors());
  }

  private static String createTypeArgumentsUniqueId(List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeArgumentDescriptors.isEmpty()) {
      return "";
    }
    return typeArgumentDescriptors
        .stream()
        .map(TypeDescriptor::getUniqueId)
        .collect(joining(", ", "<", ">"));
  }

  /**
   * The list of methods declared in the type from the JDT. Note: this does not include methods we
   * synthesize and add to the type like bridge methods.
   */
  @Memoized
  Map<String, MethodDescriptor> getDeclaredMethodDescriptorsBySignature() {
    return getDeclaredMethodDescriptorsFactory().get(this);
  }

  /**
   * The list of methods in the type from the JDT. Note: this does not include methods we synthesize
   * and add to the type like bridge methods.
   */
  @Memoized
  Map<String, MethodDescriptor> getMethodDescriptorsBySignature() {
    // TODO(rluble): update this code to handle package private methods, bridges and verify that it
    // correctly handles default methods.
    Map<String, MethodDescriptor> methodDescriptorsBySignature = new LinkedHashMap<>();

    // Add all methods declared in the current type itself
    methodDescriptorsBySignature.putAll(getDeclaredMethodDescriptorsBySignature());

    // Add all the methods from the super class.
    if (getSuperTypeDescriptor() != null) {
      AstUtils.updateMethodsBySignature(
          methodDescriptorsBySignature, getSuperTypeDescriptor().getMethodDescriptors());
    }

    // Finally add the methods that appear in super interfaces.
    for (DeclaredTypeDescriptor implementedInterface : getInterfaceTypeDescriptors()) {
      AstUtils.updateMethodsBySignature(
          methodDescriptorsBySignature, implementedInterface.getMethodDescriptors());
    }
    return methodDescriptorsBySignature;
  }

  /**
   * The list of methods declared in the type. Note: this does not include methods synthetic methods
   * (like bridge methods) nor supertype methods that are not overridden in the type.
   */
  @Memoized
  public Collection<MethodDescriptor> getDeclaredMethodDescriptors() {
    return getDeclaredMethodDescriptorsBySignature().values();
  }

  /**
   * The list of fields declared in the type. Note: this does not include methods synthetic fields
   * (like captures) nor supertype fields.
   */
  @Memoized
  public Collection<FieldDescriptor> getDeclaredFieldDescriptors() {
    return getDeclaredFieldDescriptorsFactory().get(this);
  }

  @Memoized
  public Collection<MemberDescriptor> getDeclaredMemberDescriptors() {
    return ImmutableSet.<MemberDescriptor>builder()
        .addAll(getDeclaredMethodDescriptors())
        .addAll(getDeclaredFieldDescriptors())
        .build();
  }

  /**
   * Retrieves the method descriptor with name {@code name} and the corresponding parameter types if
   * there is a method with that signature.
   */
  public MethodDescriptor getMethodDescriptorByName(
      String methodName, TypeDescriptor... parameters) {
    return getMethodDescriptorsBySignature()
        .get(MethodDescriptor.getSignature(methodName, parameters));
  }

  /** The list of all methods available on a given type. */
  public Collection<MethodDescriptor> getMethodDescriptors() {
    return getMethodDescriptorsBySignature().values();
  }

  /** Returns the default (parameterless) constructor for the type.. */
  @Memoized
  public MethodDescriptor getDefaultConstructorMethodDescriptor() {
    return getDeclaredMethodDescriptors()
        .stream()
        .filter(MethodDescriptor::isConstructor)
        .filter(methodDescriptor -> methodDescriptor.getParameterTypeDescriptors().isEmpty())
        .findFirst()
        .orElse(null);
  }

  /** Returns the JsConstructors for this class. */
  @Memoized
  @Nullable
  public List<MethodDescriptor> getJsConstructorMethodDescriptors() {
    return getDeclaredMethodDescriptors()
        .stream()
        .filter(MethodDescriptor::isJsConstructor)
        .collect(toImmutableList());
  }

  /**
   * Returns the corresponding primitive type if the {@code setTypeDescriptor} is a boxed type;
   * throws an exception otherwise.
   */
  @Memoized
  @Override
  public PrimitiveTypeDescriptor toUnboxedType() {
    checkState(TypeDescriptors.isBoxedType(this));
    return checkNotNull(TypeDescriptors.getPrimitiveTypeFromBoxType(this));
  }

  @Override
  public DeclaredTypeDescriptor toNullable() {
    if (isNullable()) {
      return this;
    }

    return DeclaredTypeDescriptor.Builder.from(this).setNullable(true).build();
  }

  @Override
  public DeclaredTypeDescriptor toNonNullable() {
    if (isTypeVariable()) {
      // TODO(b/68726480): Model nullability of type variable references correctly.
      // There is no distinction between a reference and a declaration for type variables, and
      // comparison between type variables is done by reference, there can only either be marked
      // nullable.
      return this;
    }
    if (!isNullable()) {
      return this;
    }

    return DeclaredTypeDescriptor.Builder.from(this).setNullable(false).build();
  }

  @Override
  public boolean canBeReferencedExternally() {
    if (isWildCardOrCapture() || isTypeVariable()) {
      return toRawTypeDescriptor().canBeReferencedExternally();
    }

    if (getTypeDeclaration().isJsType()
        || isJsFunctionInterface()
        || TypeDescriptors.isBoxedTypeAsJsPrimitives(this)) {
      return true;
    }

    return getDeclaredMemberDescriptors()
        .stream()
        .filter(Predicates.not(MemberDescriptor::isOrOverridesJavaLangObjectMethod))
        .anyMatch(MemberDescriptor::isJsMember);
  }

  @Memoized
  @Override
  public Map<TypeDescriptor, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    Map<TypeDescriptor, TypeDescriptor> specializedTypeArgumentByTypeParameters = new HashMap<>();

    Map<TypeDescriptor, TypeDescriptor> immediateSpecializedTypeArgumentByTypeParameters =
        new HashMap<>();

    DeclaredTypeDescriptor superTypeDescriptor = getSuperTypeDescriptor();
    List<DeclaredTypeDescriptor> superTypeOrInterfaceDescriptors = new ArrayList<>();
    if (superTypeDescriptor != null) {
      superTypeOrInterfaceDescriptors.add(superTypeDescriptor);
    }
    superTypeOrInterfaceDescriptors.addAll(getInterfaceTypeDescriptors());

    for (DeclaredTypeDescriptor superTypeOrInterfaceDescriptor : superTypeOrInterfaceDescriptors) {
      TypeDeclaration superTypeOrInterfaceDeclaration =
          superTypeOrInterfaceDescriptor.getTypeDeclaration();

      ImmutableList<DeclaredTypeDescriptor> typeParameterDescriptors =
          superTypeOrInterfaceDeclaration.getTypeParameterDescriptors();
      ImmutableList<TypeDescriptor> typeArgumentDescriptors =
          superTypeOrInterfaceDescriptor.getTypeArgumentDescriptors();

      boolean specializedTypeIsRaw = typeArgumentDescriptors.isEmpty();
      for (int i = 0; i < typeParameterDescriptors.size(); i++) {
        DeclaredTypeDescriptor typeParameterDescriptor = typeParameterDescriptors.get(i);
        TypeDescriptor typeArgumentDescriptor =
            specializedTypeIsRaw
                ? typeParameterDescriptor.getBoundTypeDescriptor().toRawTypeDescriptor()
                : typeArgumentDescriptors.get(i);
        immediateSpecializedTypeArgumentByTypeParameters.put(
            typeParameterDescriptor, typeArgumentDescriptor);
      }
      specializedTypeArgumentByTypeParameters.putAll(
          immediateSpecializedTypeArgumentByTypeParameters);

      Map<TypeDescriptor, TypeDescriptor> superSpecializedTypeArgumentByTypeParameters =
          superTypeOrInterfaceDeclaration
              .toUnparamterizedTypeDescriptor()
              .getSpecializedTypeArgumentByTypeParameters();

      for (Entry<TypeDescriptor, TypeDescriptor> entry :
          superSpecializedTypeArgumentByTypeParameters.entrySet()) {
        TypeDescriptor typeArgumentDescriptor = entry.getValue();

        typeArgumentDescriptor =
            typeArgumentDescriptor.specializeTypeVariables(
                immediateSpecializedTypeArgumentByTypeParameters);

        specializedTypeArgumentByTypeParameters.put(entry.getKey(), typeArgumentDescriptor);
      }
    }

    return specializedTypeArgumentByTypeParameters;
  }

  @Override
  public TypeDescriptor specializeTypeVariables(
      Function<TypeDescriptor, TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    if (replacementTypeArgumentByTypeVariable == Function.<TypeDescriptor>identity()) {
      return this;
    }
    switch (getKind()) {
      case CLASS:
      case INTERFACE:
      case ENUM:
        if (getTypeArgumentDescriptors().isEmpty()
            && !isJsFunctionInterface()
            && !isJsFunctionImplementation()) {
          return this;
        }

        return Builder.from(this)
            .setTypeArgumentDescriptors(
                getTypeArgumentDescriptors()
                    .stream()
                    .map(t -> t.specializeTypeVariables(replacementTypeArgumentByTypeVariable))
                    .collect(toImmutableList()))
            .setJsFunctionMethodDescriptorFactory(
                () ->
                    getJsFunctionMethodDescriptor() != null
                        ? getJsFunctionMethodDescriptor()
                            .specializeTypeVariables(replacementTypeArgumentByTypeVariable)
                        : null)
            .build();
      case TYPE_VARIABLE:
      case WILDCARD_OR_CAPTURE:
        return replacementTypeArgumentByTypeVariable.apply(this);
    }
    throw new AssertionError(getKind());
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    if (hasTypeDeclaration()) {
      return getTypeDeclaration().getReadableDescription();
    }
    return getSimpleSourceName();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_DeclaredTypeDescriptor.visit(processor, this);
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_DeclaredTypeDescriptor.Builder()
        // Default values.
        .setNullable(true)
        .setTypeArgumentDescriptors(ImmutableList.of())
        .setBoundTypeDescriptorFactory(() -> null)
        .setDeclaredMethodDescriptorsFactory(ImmutableMap::of)
        .setDeclaredFieldDescriptorsFactory(() -> ImmutableList.of())
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.of())
        .setJsFunctionMethodDescriptorFactory(() -> null)
        .setSuperTypeDescriptorFactory(() -> null)
        .setTypeDeclaration(null);
  }

  /** Builder for a TypeDescriptor. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setUniqueKey(String key);

    public abstract Builder setClassComponents(List<String> classComponents);

    public abstract Builder setEnclosingTypeDescriptor(
        DeclaredTypeDescriptor enclosingTypeDescriptor);

    public abstract Builder setKind(Kind kind);

    public abstract Builder setNullable(boolean isNullable);

    public abstract Builder setTypeArgumentDescriptors(
        Iterable<TypeDescriptor> typeArgumentDescriptors);

    public abstract Builder setBoundTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> boundTypeDescriptorFactory);

    public Builder setBoundTypeDescriptorFactory(
        Supplier<TypeDescriptor> boundTypeDescriptorFactory) {
      return setBoundTypeDescriptorFactory(typeDescriptor -> boundTypeDescriptorFactory.get());
    }

    public abstract Builder setInterfaceTypeDescriptorsFactory(
        DescriptorFactory<ImmutableList<DeclaredTypeDescriptor>> interfaceTypeDescriptorsFactory);

    public Builder setInterfaceTypeDescriptorsFactory(
        Supplier<ImmutableList<DeclaredTypeDescriptor>> interfaceTypeDescriptorsFactory) {
      return setInterfaceTypeDescriptorsFactory(
          typeDescriptor -> interfaceTypeDescriptorsFactory.get());
    }

    public abstract Builder setJsFunctionMethodDescriptorFactory(
        DescriptorFactory<MethodDescriptor> jsFunctionMethodDescriptorFactory);

    public Builder setJsFunctionMethodDescriptorFactory(
        Supplier<MethodDescriptor> jsFunctionMethodDescriptorFactory) {
      return setJsFunctionMethodDescriptorFactory(
          typeDescriptor -> jsFunctionMethodDescriptorFactory.get());
    }

    public abstract Builder setRawTypeDescriptorFactory(
        DescriptorFactory<DeclaredTypeDescriptor> rawTypeDescriptorFactory);

    public Builder setRawTypeDescriptorFactory(
        Supplier<DeclaredTypeDescriptor> rawTypeDescriptorFactory) {
      return setRawTypeDescriptorFactory(typeDescriptor -> rawTypeDescriptorFactory.get());
    }

    public abstract Builder setSingleAbstractMethodDescriptorFactory(
        DescriptorFactory<MethodDescriptor> singleAbstractMethodDescriptorFactory);

    public Builder setSingleAbstractMethodDescriptorFactory(
        Supplier<MethodDescriptor> singleAbstractMethodDescriptorFactory) {
      return setSingleAbstractMethodDescriptorFactory(
          typeDescriptor -> singleAbstractMethodDescriptorFactory.get());
    }

    public abstract Builder setSuperTypeDescriptorFactory(
        DescriptorFactory<DeclaredTypeDescriptor> superTypeDescriptorFactory);

    public Builder setSuperTypeDescriptorFactory(
        Supplier<DeclaredTypeDescriptor> superTypeDescriptorFactory) {
      return setSuperTypeDescriptorFactory(typeDescriptor -> superTypeDescriptorFactory.get());
    }

    public abstract Builder setDeclaredMethodDescriptorsFactory(
        DescriptorFactory<ImmutableMap<String, MethodDescriptor>> declaredMethodDescriptorsFactory);

    public Builder setDeclaredMethodDescriptorsFactory(
        Supplier<ImmutableMap<String, MethodDescriptor>> declaredMethodDescriptorsFactory) {
      return setDeclaredMethodDescriptorsFactory(
          typeDescriptor -> declaredMethodDescriptorsFactory.get());
    }

    public abstract Builder setDeclaredFieldDescriptorsFactory(
        DescriptorFactory<ImmutableList<FieldDescriptor>> declaredFieldDescriptorsFactory);

    public Builder setDeclaredFieldDescriptorsFactory(
        Supplier<ImmutableList<FieldDescriptor>> declaredFieldDescriptorsFactory) {
      return setDeclaredFieldDescriptorsFactory(
          typeDescriptor -> declaredFieldDescriptorsFactory.get());
    }

    public abstract Builder setTypeDeclaration(TypeDeclaration typeDeclaration);

    private static final ThreadLocalInterner<DeclaredTypeDescriptor> interner =
        new ThreadLocalInterner<>();

    abstract DeclaredTypeDescriptor autoBuild();

    @SuppressWarnings("ReferenceEquality")
    public DeclaredTypeDescriptor build() {
      DeclaredTypeDescriptor typeDescriptor = autoBuild();

      checkState(
          typeDescriptor.getEnclosingTypeDescriptor() == null
              || (!typeDescriptor.getEnclosingTypeDescriptor().isTypeVariable()
                  && !typeDescriptor.getEnclosingTypeDescriptor().isWildCardOrCapture()));

      checkState(!typeDescriptor.isTypeVariable() || typeDescriptor.isNullable());
      checkState(
          typeDescriptor.hasTypeDeclaration()
              == (typeDescriptor.isClass()
                  || typeDescriptor.isInterface()
                  || typeDescriptor.isEnum()));

      DeclaredTypeDescriptor internedTypeDescriptor = interner.intern(typeDescriptor);

      // Some native standard TypeDescriptors are created BEFORE TypeDescriptors is initialized.
      if (TypeDescriptors.isInitialized()) {
        // Make sure there is only one global namespace TypeDescriptor (see b/32903150).
        checkArgument(
            internedTypeDescriptor.getTypeDeclaration() == null
                || internedTypeDescriptor.getTypeDeclaration().getQualifiedJsName() == null
                || !internedTypeDescriptor.getTypeDeclaration().getQualifiedJsName().isEmpty()
                || TypeDescriptors.get().globalNamespace == null
                || internedTypeDescriptor == TypeDescriptors.get().globalNamespace,
            "Attempt to build type descriptor %s for the global scope that is not %s.",
            internedTypeDescriptor,
            TypeDescriptors.get().globalNamespace);
      }
      return internedTypeDescriptor;
    }

    public static Builder from(final DeclaredTypeDescriptor typeDescriptor) {
      return typeDescriptor.toBuilder();
    }
  }
}
