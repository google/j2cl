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
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
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
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A usage-site reference to a declared type, i.e. a class, an interface or an enum.
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
  public boolean isUnusableByJsSuppressed() {
    return getTypeDeclaration().isUnusableByJsSuppressed();
  }

  public boolean isDeprecated() {
    return getTypeDeclaration().isDeprecated();
  }

  @Override
  public ImmutableList<String> getClassComponents() {
    return getTypeDeclaration().getClassComponents();
  }

  @Nullable
  public abstract DeclaredTypeDescriptor getEnclosingTypeDescriptor();

  public abstract ImmutableList<TypeDescriptor> getTypeArgumentDescriptors();

  public boolean isFinal() {
    return getTypeDeclaration().isFinal();
  }

  public boolean isFunctionalInterface() {
    return getTypeDeclaration().isFunctionalInterface();
  }

  @Override
  public boolean isJsType() {
    return getTypeDeclaration().isJsType();
  }

  @Override
  public boolean isJsFunctionImplementation() {
    return getTypeDeclaration().isJsFunctionImplementation();
  }

  @Override
  public boolean isJsFunctionInterface() {
    return getTypeDeclaration().isJsFunctionInterface();
  }

  @Override
  public boolean isJsEnum() {
    return getTypeDeclaration().isJsEnum();
  }

  @Override
  public boolean isNative() {
    return getTypeDeclaration().isNative();
  }

  @Override
  public JsEnumInfo getJsEnumInfo() {
    return getTypeDeclaration().getJsEnumInfo();
  }

  @Override
  public abstract boolean isNullable();

  public boolean hasJsConstructor() {
    return getTypeDeclaration().hasJsConstructor();
  }

  /* PRIVATE AUTO_VALUE PROPERTIES */

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
  abstract DescriptorFactory<DeclaredTypeDescriptor> getSuperTypeDescriptorFactory();

  public boolean hasTypeArguments() {
    return !getTypeArgumentDescriptors().isEmpty();
  }

  @Override
  public boolean isClass() {
    return getTypeDeclaration().isClass();
  }

  @Override
  public boolean isInterface() {
    return getTypeDeclaration().isInterface();
  }

  @Override
  public boolean isEnum() {
    return getTypeDeclaration().isEnum();
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
  public TypeDeclaration getMetadataTypeDeclaration() {
    return getTypeDeclaration().getMetadataTypeDeclaration();
  }

  public DeclaredTypeDescriptor getOverlayImplementationTypeDescriptor() {
    return getTypeDeclaration()
        .getOverlayImplementationTypeDeclaration()
        .toUnparameterizedTypeDescriptor();
  }

  public boolean hasOverlayImplementationType() {
    return getTypeDeclaration().hasOverlayImplementationType();
  }

  @Override
  public DeclaredTypeDescriptor toRawTypeDescriptor() {
    return getTypeDeclaration().toRawTypeDescriptor();
  }

  @Override
  public DeclaredTypeDescriptor toUnparameterizedTypeDescriptor() {
    return getTypeDeclaration().toUnparameterizedTypeDescriptor();
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    TypeDescriptor thatRawTypeDescriptor = that.toRawTypeDescriptor();
    return thatRawTypeDescriptor instanceof DeclaredTypeDescriptor
        && isSubtypeOf((DeclaredTypeDescriptor) thatRawTypeDescriptor);
  }

  public boolean isSubtypeOf(DeclaredTypeDescriptor that) {
    return getTypeDeclaration().isSubtypeOf(that.getTypeDeclaration());
  }

  public boolean extendsNativeClass() {
    return getTypeDeclaration().extendsNativeClass();
  }


  @Override
  @Memoized
  public Set<TypeVariable> getAllTypeVariables() {
    Set<TypeVariable> typeVariables = new LinkedHashSet<>();
    collectAllTypeVariables(this, typeVariables);
    return typeVariables;
  }

  private static void collectAllTypeVariables(
      DeclaredTypeDescriptor typeDescriptor, Set<TypeVariable> typeVariables) {
    for (TypeDescriptor typeArgumentTypeDescriptor : typeDescriptor.getTypeArgumentDescriptors()) {
      typeVariables.addAll(typeArgumentTypeDescriptor.getAllTypeVariables());
    }

    if (typeDescriptor.isJsFunctionInterface() || typeDescriptor.isJsFunctionImplementation()) {
      if (typeDescriptor.getJsFunctionMethodDescriptor() == null) {
        // An invalid abstract JsFunction implementation might not have a JsFunction method
        // descriptor, and will be rejected during by JsInteropRestrictionChecker. But this method
        // might be called in the process and would throw NPE below.
        return;
      }
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
    return getTypeDeclaration().getQualifiedSourceName();
  }

  @Override
  @Memoized
  public String getQualifiedBinaryName() {
    return getTypeDeclaration().getQualifiedBinaryName();
  }

  @Memoized
  @Nullable
  public DeclaredTypeDescriptor getSuperTypeDescriptor() {
    return getSuperTypeDescriptorFactory().get(this);
  }

  public abstract TypeDeclaration getTypeDeclaration();

  @Memoized
  @Override
  public String getUniqueId() {
    String uniqueKey = getQualifiedBinaryName();
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
    if (!isNullable()) {
      return this;
    }

    return DeclaredTypeDescriptor.Builder.from(this).setNullable(false).build();
  }

  @Override
  public boolean canBeReferencedExternally() {
    if (isParameterizedByNonNativeJsEnum()) {
      return false;
    }

    if (getTypeDeclaration().isJsType()
        || isJsFunctionInterface()
        || TypeDescriptors.isBoxedTypeAsJsPrimitives(this)
        || TypeDescriptors.isJavaLangObject(this)) {
      return true;
    }

    // TODO(b/79210574): reconsider whether types with only static JsMembers are actually
    // referenceable externally.
    return getDeclaredMemberDescriptors()
        .stream()
        .filter(Predicates.not(MemberDescriptor::isOrOverridesJavaLangObjectMethod))
        .anyMatch(MemberDescriptor::isJsMember);
  }

  private boolean isParameterizedByNonNativeJsEnum() {
    for (TypeDescriptor typeArgument : getTypeArgumentDescriptors()) {
      if (AstUtils.isNonNativeJsEnum(typeArgument)) {
        return true;
      }
      if (typeArgument instanceof DeclaredTypeDescriptor) {
        DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeArgument;
        if (declaredTypeDescriptor.isParameterizedByNonNativeJsEnum()) {
          return true;
        }
      }
    }
    return false;
  }

  @Memoized
  @Override
  public Map<TypeVariable, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    Map<TypeVariable, TypeDescriptor> specializedTypeArgumentByTypeParameters = new HashMap<>();

    Map<TypeVariable, TypeDescriptor> immediateSpecializedTypeArgumentByTypeParameters =
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

      ImmutableList<TypeVariable> typeParameterDescriptors =
          superTypeOrInterfaceDeclaration.getTypeParameterDescriptors();
      ImmutableList<TypeDescriptor> typeArgumentDescriptors =
          superTypeOrInterfaceDescriptor.getTypeArgumentDescriptors();

      boolean specializedTypeIsRaw = typeArgumentDescriptors.isEmpty();
      for (int i = 0; i < typeParameterDescriptors.size(); i++) {
        TypeVariable typeParameterDescriptor = typeParameterDescriptors.get(i);
        TypeDescriptor typeArgumentDescriptor =
            specializedTypeIsRaw
                ? typeParameterDescriptor.getBoundTypeDescriptor().toRawTypeDescriptor()
                : typeArgumentDescriptors.get(i);
        immediateSpecializedTypeArgumentByTypeParameters.put(
            typeParameterDescriptor, typeArgumentDescriptor);
      }
      specializedTypeArgumentByTypeParameters.putAll(
          immediateSpecializedTypeArgumentByTypeParameters);

      Map<TypeVariable, TypeDescriptor> superSpecializedTypeArgumentByTypeParameters =
          superTypeOrInterfaceDeclaration
              .toUnparameterizedTypeDescriptor()
              .getSpecializedTypeArgumentByTypeParameters();

      for (Entry<TypeVariable, TypeDescriptor> entry :
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
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    if (AstUtils.isIdentityFunction(replacementTypeArgumentByTypeVariable)) {
      return this;
    }
    if (getTypeArgumentDescriptors().isEmpty()
        // TODO(b/70853239): See why this is needed.
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
  }

  @Override
  public String getReadableDescription() {
    return getTypeDeclaration().getReadableDescription()
        + (hasTypeArguments()
            ? getTypeArgumentDescriptors().stream()
                .map(TypeDescriptor::getReadableDescription)
                .collect(Collectors.joining(", ", "<", ">"))
            : "");
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
        .setDeclaredMethodDescriptorsFactory(ImmutableMap::of)
        .setDeclaredFieldDescriptorsFactory(() -> ImmutableList.of())
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.of())
        .setJsFunctionMethodDescriptorFactory(() -> null)
        .setSuperTypeDescriptorFactory(() -> null);
  }

  /** Builder for a TypeDescriptor. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setEnclosingTypeDescriptor(
        DeclaredTypeDescriptor enclosingTypeDescriptor);

    public abstract Builder setNullable(boolean isNullable);

    public abstract Builder setTypeArgumentDescriptors(
        Iterable<? extends TypeDescriptor> typeArgumentDescriptors);

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
          typeDescriptor.isClass() || typeDescriptor.isInterface() || typeDescriptor.isEnum());

      DeclaredTypeDescriptor internedTypeDescriptor = interner.intern(typeDescriptor);

      // Some native standard TypeDescriptors are created BEFORE TypeDescriptors is initialized.
      if (TypeDescriptors.isInitialized()) {
        // Make sure there is only one global namespace TypeDescriptor (see b/32903150).
        checkArgument(
            internedTypeDescriptor.getTypeDeclaration().getQualifiedJsName() == null
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
