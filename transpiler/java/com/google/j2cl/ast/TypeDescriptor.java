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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.ast.TypeDescriptors.BootstrapType;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.common.HasJsNameInfo;
import com.google.j2cl.ast.common.HasReadableDescription;
import com.google.j2cl.ast.common.JsUtils;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
 * A usage-site reference to a type.
 *
 * <p>This class is mostly a bag of precomputed properties, and the details of how those properties
 * are created live in several creation functions in JdtUtils and TypeDescriptors.
 *
 * <p>A couple of properties are lazily calculated via the DescriptorFactory and interface, since
 * eagerly calculating them would lead to infinite loops of Descriptor creation.
 *
 * <p>Since these are all usage-site references, when there are type variables they are always
 * thought of as type arguments.
 */
@AutoValue
@Visitable
public abstract class TypeDescriptor extends Node
    implements Comparable<TypeDescriptor>,
        HasJsNameInfo,
        HasReadableDescription,
        HasUnusableByJsSuppression {

  /**
   * References to some descriptors need to be deferred in some cases since it will cause infinite
   * loops.
   */
  public interface DescriptorFactory<T> {
    T get(TypeDescriptor typeDescriptor);
  }

  public static final Comparator<TypeDescriptor> MORE_SPECIFIC_INTERFACES_FIRST =
      (thisTypeDescriptor, thatTypeDescriptor) ->
          Integer.compare(
              thatTypeDescriptor.getMaxInterfaceDepth(), thisTypeDescriptor.getMaxInterfaceDepth());

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeDescriptor.visit(processor, this);
  }

  @Override
  public int compareTo(TypeDescriptor that) {
    return getUniqueId().compareTo(that.getUniqueId());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TypeDescriptor) {
      return getUniqueId().equals(((TypeDescriptor) o).getUniqueId());
    }
    return false;
  }

  public boolean hasSameRawType(TypeDescriptor other) {
    // TODO(rluble): compare using getRawTypeDescriptor once raw TypeDescriptors are constructed
    // correctly. Raw TypeDescriptors are constructed in one of two ways, 1) from a JDT RAW
    // TypeDescriptor and 2) from a TypeDescriptor by removing type variables. These two ways are
    // not consistent, in particular the second form does not propagate the removal of type
    // variables inward. These two construction end up with different data but with the same unique
    // id, so the first one that is constructed will be interned and used everywhere.
    // Using getRawTypeDescriptor here triggers the second (incorrect) construction and causes
    // the wrong information be used in some cases.

    // For type variables, wildcards and captures we still need to do getRawTypeDescriptor to get
    // the bound.
    TypeDescriptor thisTypeDescriptor =
        isTypeVariable() || isWildCardOrCapture() ? getRawTypeDescriptor() : this;
    other =
        other.isTypeVariable() || other.isWildCardOrCapture()
            ? other.getRawTypeDescriptor()
            : other;
    return thisTypeDescriptor.getQualifiedSourceName().equals(other.getQualifiedSourceName());
  }

  /** Returns the simple binary name like "Outer$Inner". Used for file naming purposes. */
  @Memoized
  public String getSimpleBinaryName() {
    return Joiner.on('$').join(getClassComponents());
  }

  /**
   * Returns the fully package qualified binary name like "com.google.common.Outer$Inner".
   *
   * <p>Used for generated class metadata (per JLS), file overview, file path, unique id calculation
   * and other similar scenarios.
   */
  @Memoized
  public String getQualifiedBinaryName() {
    return Joiner.on(".").skipNulls().join(getPackageName(), getSimpleBinaryName());
  }

  /** Returns the globally unique qualified name by which this type should be defined/imported. */
  public String getModuleName() {
    return getQualifiedJsName();
  }

  public String getImplModuleName() {
    return isNative() || isExtern() ? getModuleName() : getModuleName() + "$impl";
  }

  @Override
  public boolean isUnusableByJsSuppressed() {
    return getTypeDeclaration().isUnusableByJsSuppressed();
  }

  /** Returns the fully package qualified name like "com.google.common". */
  @Nullable
  @Memoized
  public String getPackageName() {
    return hasTypeDeclaration() ? getTypeDeclaration().getPackageName() : null;
  }

  /**
   * Returns a list of Strings representing the current type's simple name and enclosing type simple
   * names. For example for "com.google.foo.Outer" the class components are ["Outer"] and for
   * "com.google.foo.Outer.Inner" the class components are ["Outer", "Inner"].
   */
  public abstract ImmutableList<String> getClassComponents();

  @Nullable
  public abstract TypeDescriptor getEnclosingTypeDescriptor();

  @Nullable
  public abstract TypeDescriptor getComponentTypeDescriptor();

  @Nullable
  public abstract TypeDescriptor getLeafTypeDescriptor();

  public abstract int getDimensions();

  public abstract ImmutableList<TypeDescriptor> getTypeArgumentDescriptors();

  public abstract ImmutableList<TypeDescriptor> getUnionedTypeDescriptors();

  public Visibility getVisibility() {
    return getTypeDeclaration().getVisibility();
  }

  public abstract Kind getKind();

  public boolean isAbstract() {
    return getTypeDeclaration().isAbstract();
  }

  public boolean isFinal() {
    return getTypeDeclaration().isFinal();
  }

  public boolean isFunctionalInterface() {
    return getTypeDeclaration().isFunctionalInterface();
  }

  public boolean isJsFunctionImplementation() {
    return hasTypeDeclaration() && getTypeDeclaration().isJsFunctionImplementation();
  }

  public boolean isJsFunctionInterface() {
    return hasTypeDeclaration() && getTypeDeclaration().isJsFunctionInterface();
  }

  public boolean isJsType() {
    return getTypeDeclaration().isJsType();
  }

  /**
   * Returns whether the described type is a nested type (i.e. it is defined inside the body of some
   * enclosing type) but is not a member type because it's location in the body is not in the
   * declaration scope of the enclosing type. For example:
   *
   * <p><code> class Foo { void bar() { class Baz {} } } </code>
   *
   * <p>or
   *
   * <p><code> class Foo { void bar() { Comparable comparable = new Comparable() { ... } } } </code>
   */
  public boolean isLocal() {
    return hasTypeDeclaration() && getTypeDeclaration().isLocal();
  }

  public boolean isAnonymous() {
    return hasTypeDeclaration() && getTypeDeclaration().isAnonymous();
  }

  @Override
  public boolean isNative() {
    return hasTypeDeclaration() && getTypeDeclaration().isNative();
  }

  /** Returns true for arrays where raw JavaScript array representation is enough. */
  public boolean isUntypedArray() {
    //TODO(b/36179585): Either have the same semantics as GWT for untyped arrays or update the
    // jsinterop spec to reflect the new behavior.
    if (!isArray() || getDimensions() != 1) {
      return false;
    }
    return getLeafTypeDescriptor().isNative()
        || TypeDescriptors.isJavaLangObject(getLeafTypeDescriptor());
  }

  public abstract boolean isNullable();

  public boolean hasJsConstructor() {
    return getTypeDeclaration().hasJsConstructor();
  }

  /* PRIVATE AUTO_VALUE PROPERTIES */

  @Nullable
  abstract String getUniqueKey();

  @Nullable
  abstract DescriptorFactory<MethodDescriptor> getConcreteJsFunctionMethodDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableMap<String, MethodDescriptor>>
      getDeclaredMethodDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<FieldDescriptor>> getDeclaredFieldDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<TypeDescriptor>> getInterfaceTypeDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<MethodDescriptor> getJsFunctionMethodDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<TypeDescriptor> getRawTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<TypeDescriptor> getBoundTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<TypeDescriptor> getSuperTypeDescriptorFactory();

  /**
   * Returns the JavaScript name for this class. This is same as simple source name unless modified
   * by JsType.
   */
  @Override
  @Nullable
  @Memoized
  public String getSimpleJsName() {
    return hasTypeDeclaration()
        ? getTypeDeclaration().getSimpleJsName()
        : AstUtils.getSimpleSourceName(getClassComponents());
  }

  @Override
  @Nullable
  @Memoized
  public String getJsNamespace() {
    return hasTypeDeclaration() ? getTypeDeclaration().getJsNamespace() : null;
  }

  /** Returns true if the class captures its enclosing instance */
  public boolean isCapturingEnclosingInstance() {
    return getTypeDeclaration().isCapturingEnclosingInstance();
  }

  public boolean hasTypeArguments() {
    return !getTypeArgumentDescriptors().isEmpty();
  }

  public boolean isPrimitive() {
    return getKind() == Kind.PRIMITIVE;
  }

  public boolean isTypeVariable() {
    return getKind() == Kind.TYPE_VARIABLE;
  }

  /** Returns whether the described type is a union. */
  public boolean isUnion() {
    return getKind() == Kind.UNION;
  }

  public boolean isWildCardOrCapture() {
    return getKind() == Kind.WILDCARD_OR_CAPTURE;
  }

  /** Returns whether the described type is an array. */
  public boolean isArray() {
    return getKind() == Kind.ARRAY;
  }

  /** Returns whether the described type is a class. */
  public boolean isClass() {
    return getKind() == Kind.CLASS;
  }

  /** Returns whether the described type is an interface. */
  public boolean isInterface() {
    return getKind() == Kind.INTERFACE;
  }

  /** Returns whether the described type is an interface. */
  public boolean isIntersection() {
    return getKind() == Kind.INTERSECTION;
  }

  /** Returns whether the described type is an enum. */
  public boolean isEnum() {
    return getKind() == Kind.ENUM;
  }

  @Memoized
  public boolean isExtern() {
    return isNative() && hasExternNamespace();
  }

  public boolean isStarOrUnknown() {
    return hasTypeDeclaration() && getTypeDeclaration().isStarOrUnknown();
  }

  private boolean hasExternNamespace() {
    checkArgument(isNative());
    // A native type descriptor is an extern if its namespace is the global namespace or if
    // it inherited the namespace from its (enclosing) extern type.
    return JsUtils.isGlobal(getJsNamespace())
        || (getEnclosingTypeDescriptor() != null
            && getEnclosingTypeDescriptor().isExtern()
            && getJsNamespace().equals(getEnclosingTypeDescriptor().getQualifiedJsName()));
  }

  @Memoized
  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return getConcreteJsFunctionMethodDescriptorFactory().get(this);
  }

  /**
   * Returns a list of the type descriptors of interfaces that are explicitly implemented directly
   * on this type.
   */
  @Memoized
  public ImmutableList<TypeDescriptor> getInterfaceTypeDescriptors() {
    return getInterfaceTypeDescriptorsFactory().get(this);
  }

  /**
   * Returns a set of the type descriptors of interfaces that are explicitly implemented either
   * directly on this type or on some super type or super interface.
   */
  @Memoized
  public Set<TypeDescriptor> getTransitiveInterfaceTypeDescriptors() {
    Set<TypeDescriptor> typeDescriptors = new LinkedHashSet<>();

    // Recursively gather from super interfaces.
    for (TypeDescriptor interfaceTypeDescriptor : getInterfaceTypeDescriptors()) {
      typeDescriptors.add(interfaceTypeDescriptor);
      typeDescriptors.addAll(interfaceTypeDescriptor.getTransitiveInterfaceTypeDescriptors());
    }

    // Recursively gather from super type.
    TypeDescriptor superTypeDescriptor = getSuperTypeDescriptor();
    if (superTypeDescriptor != null) {
      typeDescriptors.addAll(superTypeDescriptor.getTransitiveInterfaceTypeDescriptors());
    }

    return typeDescriptors;
  }

  @Memoized
  @Nullable
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return getJsFunctionMethodDescriptorFactory().get(this);
  }

  /**
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  @Memoized
  @Nullable
  public TypeDescriptor getRawTypeDescriptor() {
    return getRawTypeDescriptorFactory().get(this);
  }

  /** Returns the bound for a type variable. */
  @Memoized
  @Nullable
  public TypeDescriptor getBoundTypeDescriptor() {
    checkState(isTypeVariable() || isWildCardOrCapture());
    TypeDescriptor boundTypeDescriptor = getBoundTypeDescriptorFactory().get(this);
    return boundTypeDescriptor != null ? boundTypeDescriptor : TypeDescriptors.get().javaLangObject;
  }

  public boolean isSupertypeOf(TypeDescriptor that) {
    return that.getRawSuperTypesIncludingSelf().contains(this.getRawTypeDescriptor());
  }

  public boolean isSubtypeOf(TypeDescriptor that) {
    return getRawSuperTypesIncludingSelf().contains(that.getRawTypeDescriptor());
  }

  @Memoized
  Set<TypeDescriptor> getRawSuperTypesIncludingSelf() {
    Set<TypeDescriptor> allRawSupertypesIncludingSelf = new LinkedHashSet<>();
    allRawSupertypesIncludingSelf.add(getRawTypeDescriptor());
    if (getSuperTypeDescriptor() != null) {
      allRawSupertypesIncludingSelf.addAll(
          getSuperTypeDescriptor().getRawSuperTypesIncludingSelf());
    }
    for (TypeDescriptor interfaceTypeDescriptor : getInterfaceTypeDescriptors()) {
      allRawSupertypesIncludingSelf.addAll(interfaceTypeDescriptor.getRawSuperTypesIncludingSelf());
    }
    return allRawSupertypesIncludingSelf;
  }

  /** Returns all type variables that appear in the type arguments slot(s). */
  @Memoized
  public Set<TypeDescriptor> getAllTypeVariables() {
    Set<TypeDescriptor> typeVariables = new LinkedHashSet<>();
    getAllTypeVariables(this, typeVariables);
    return typeVariables;
  }

  private static void getAllTypeVariables(
      TypeDescriptor typeDescriptor, Set<TypeDescriptor> typeVariables) {
    if (typeDescriptor.isTypeVariable()) {
      typeVariables.add(typeDescriptor);
    }
    for (TypeDescriptor typeArgumentTypeDescriptor : typeDescriptor.getTypeArgumentDescriptors()) {
      getAllTypeVariables(typeArgumentTypeDescriptor, typeVariables);
    }
    checkArgument(!typeDescriptor.isUnion() || typeVariables.isEmpty());
  }

  @SuppressWarnings("ReferenceEquality")
  @Memoized
  public List<TypeDescriptor> getIntersectedTypeDescriptors() {
    checkState(isIntersection());
    TypeDescriptor superType = getSuperTypeDescriptor();
    // TODO(rluble): Reexamine this code after upgrading JDT to 4.7, where intersection types
    // are surfaced. Technically if one explicitly includes j.l.Object in the intersection type
    // then j.l.Object should be the first member of the intersection. In that case j2cl is not
    // consistent with Java.
    if (superType == TypeDescriptors.get().javaLangObject || superType == null) {
      return getInterfaceTypeDescriptors();
    }
    List<TypeDescriptor> types = new ArrayList<>();
    // First add the supertype and then the interfaces to be consistent with type erasure (JLS 4.6,
    // 13.1). Classes can only appear in leftmost position and the erasure is the leftmost bound.
    types.add(superType);
    types.addAll(getInterfaceTypeDescriptors());
    return types;
  }

  /**
   * Returns the qualified JavaScript name of the type. Same as {@link #getQualifiedSourceName}
   * unless it is modified by JsType/JsPacakge.
   *
   * <p>This is used for driving module name (hence importing etc.), long alias name, mangled name
   * generation and other similar scenarios.
   */
  public String getQualifiedJsName() {
    if (JsUtils.isGlobal(getJsNamespace())) {
      return getSimpleJsName();
    }
    return getJsNamespace() + "." + getSimpleJsName();
  }

  /**
   * Returns the unqualified simple source name like "Inner". Used when a readable name is required
   * to refer to the type like a short alias, Debug/Error output, etc.
   */
  @Memoized
  public String getSimpleSourceName() {
    return AstUtils.getSimpleSourceName(getClassComponents());
  }

  /**
   * Returns the fully package qualified source name like "com.google.common.Outer.Inner". Used in
   * places where original name is useful (like aliasing, identifying the corressponding java type,
   * Debug/Error output, etc.
   */
  @Memoized
  public String getQualifiedSourceName() {
    return Joiner.on(".")
        .skipNulls()
        .join(getPackageName(), Joiner.on(".").join(getClassComponents()));
  }

  @Memoized
  @Nullable
  public TypeDescriptor getSuperTypeDescriptor() {
    return getSuperTypeDescriptorFactory().get(this);
  }

  @Memoized
  public String getShortAliasName() {
    // Add "$" prefix for bootstrap types and primitive types.
    if (BootstrapType.typeDescriptors.contains(TypeDescriptors.toNullable(this)) || isPrimitive()) {
      return "$" + getSimpleSourceName();
    }
    return getSimpleSourceName();
  }

  public String getLongAliasName() {
    return getQualifiedSourceName().replace("_", "__").replace('.', '_');
  }

  @Nullable
  public abstract TypeDeclaration getTypeDeclaration();

  public boolean hasTypeDeclaration() {
    boolean hasClassDeclaration = getTypeDeclaration() != null;
    checkState(hasClassDeclaration == (isClass() || isInterface() || isEnum() || isPrimitive()));
    return hasClassDeclaration;
  }

  /** A unique string for a give type. Used for interning. */
  @Memoized
  public String getUniqueId() {
    String uniqueKey = MoreObjects.firstNonNull(getUniqueKey(), getQualifiedBinaryName());
    String prefix = isNullable() ? "?" : "!";
    return prefix
        + uniqueKey
        + TypeDescriptor.createTypeArgumentsUniqueId(getTypeArgumentDescriptors());
  }

  private static String createTypeArgumentsUniqueId(List<TypeDescriptor> typeArgumentDescriptors) {
    if (typeArgumentDescriptors == null || typeArgumentDescriptors.isEmpty()) {
      return "";
    }
    return J2clUtils.format(
        "<%s>",
        typeArgumentDescriptors.stream().map(TypeDescriptor::getUniqueId).collect(joining(", ")));
  }

  @Override
  @Memoized
  public int hashCode() {
    return Objects.hashCode(getUniqueId());
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
    for (TypeDescriptor implementedInterface : getInterfaceTypeDescriptors()) {
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


  @Override
  public String toString() {
    return getUniqueId();
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    // TODO: Actually provide a real readable description.
    if (isAnonymous()) {
      if (getInterfaceTypeDescriptors().isEmpty()) {
        return "new " + getSuperTypeDescriptor().getReadableDescription();
      } else {
        return "new " + getInterfaceTypeDescriptors().get(0).getReadableDescription();
      }
    } else if (isLocal()) {
      return getSimpleSourceName().replaceFirst("\\$\\d+", "");
    }
    return getSimpleSourceName();
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_TypeDescriptor.Builder()
        // Default values.
        .setNullable(true)
        .setDimensions(0)
        .setUnionedTypeDescriptors(Collections.emptyList())
        .setTypeArgumentDescriptors(Collections.emptyList())
        .setBoundTypeDescriptorFactory(() -> null)
        .setConcreteJsFunctionMethodDescriptorFactory(() -> null)
        .setDeclaredMethodDescriptorsFactory(ImmutableMap::of)
        .setDeclaredFieldDescriptorsFactory(() -> ImmutableList.of())
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.of())
        .setJsFunctionMethodDescriptorFactory(() -> null)
        .setRawTypeDescriptorFactory(() -> null)
        .setSuperTypeDescriptorFactory(() -> null)
        .setTypeDeclaration(null);
  }

  /** Builder for a TypeDescriptor. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setUniqueKey(String key);

    public abstract Builder setClassComponents(String... classComponents);

    public abstract Builder setClassComponents(List<String> classComponents);

    public abstract Builder setEnclosingTypeDescriptor(TypeDescriptor enclosingTypeDescriptor);

    public abstract Builder setComponentTypeDescriptor(TypeDescriptor componentTypeDescriptor);

    public abstract Builder setLeafTypeDescriptor(TypeDescriptor leafTypeDescriptor);

    public abstract Builder setDimensions(int dimensions);

    public abstract Builder setKind(Kind kind);

    public abstract Builder setNullable(boolean isNullable);

    public abstract Builder setTypeArgumentDescriptors(
        Iterable<TypeDescriptor> typeArgumentDescriptors);

    public abstract Builder setUnionedTypeDescriptors(List<TypeDescriptor> unionedTypeDescriptors);

    public abstract Builder setBoundTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> boundTypeDescriptorFactory);

    public Builder setBoundTypeDescriptorFactory(
        Supplier<TypeDescriptor> boundTypeDescriptorFactory) {
      return setBoundTypeDescriptorFactory(typeDescriptor -> boundTypeDescriptorFactory.get());
    }

    public abstract Builder setConcreteJsFunctionMethodDescriptorFactory(
        DescriptorFactory<MethodDescriptor> concreteJsFunctionMethodDescriptorFactory);

    public Builder setConcreteJsFunctionMethodDescriptorFactory(
        Supplier<MethodDescriptor> concreteJsFunctionMethodDescriptorFactory) {
      return setConcreteJsFunctionMethodDescriptorFactory(
          typeDescriptor -> concreteJsFunctionMethodDescriptorFactory.get());
    }

    public abstract Builder setInterfaceTypeDescriptorsFactory(
        DescriptorFactory<ImmutableList<TypeDescriptor>> interfaceTypeDescriptorsFactory);

    public Builder setInterfaceTypeDescriptorsFactory(
        Supplier<ImmutableList<TypeDescriptor>> interfaceTypeDescriptorsFactory) {
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
        DescriptorFactory<TypeDescriptor> rawTypeDescriptorFactory);

    public Builder setRawTypeDescriptorFactory(Supplier<TypeDescriptor> rawTypeDescriptorFactory) {
      return setRawTypeDescriptorFactory(typeDescriptor -> rawTypeDescriptorFactory.get());
    }

    public abstract Builder setSuperTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> superTypeDescriptorFactory);

    public Builder setSuperTypeDescriptorFactory(
        Supplier<TypeDescriptor> superTypeDescriptorFactory) {
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

    private static final ThreadLocalInterner<TypeDescriptor> interner = new ThreadLocalInterner<>();

    abstract TypeDescriptor autoBuild();

    @SuppressWarnings("ReferenceEquality")
    public TypeDescriptor build() {
      TypeDescriptor typeDescriptor = autoBuild();

      checkState(!typeDescriptor.isTypeVariable() || typeDescriptor.isNullable());
      checkState(!typeDescriptor.isPrimitive() || !typeDescriptor.isNullable());

      // Can not be both a JsFunction implementation and js function interface
      checkState(
          !typeDescriptor.isJsFunctionImplementation() || !typeDescriptor.isJsFunctionInterface());

      // Can not be both a JsFunction implementation and a functional interface
      checkState(
          !typeDescriptor.isJsFunctionImplementation() || !typeDescriptor.isFunctionalInterface());

      // TODO(tdeegan): Complete the precondition checks to make sure we are never building a
      // type descriptor that does not make sense.
      TypeDescriptor internedTypeDescriptor = interner.intern(typeDescriptor);

      // Make sure there is only one global namespace TypeDescriptor (see b/32903150).
      checkArgument(
          internedTypeDescriptor.getQualifiedJsName() == null
              || !internedTypeDescriptor.getQualifiedJsName().isEmpty()
              || TypeDescriptors.GLOBAL_NAMESPACE == null
              || internedTypeDescriptor == TypeDescriptors.GLOBAL_NAMESPACE,
          "Attempt to build type descriptor %s for the global scope that is not %s.",
          internedTypeDescriptor,
          TypeDescriptors.GLOBAL_NAMESPACE);
      return internedTypeDescriptor;
    }

    public static Builder from(final TypeDescriptor typeDescriptor) {
      return typeDescriptor.toBuilder();
    }
  }

  /**
   * A mapping that fully describes the final specialized type argument value for every super type
   * or interface of the current type.
   *
   * <p>For example given:
   *
   * <pre>
   * class A<A1, A2> {}
   * class B<B1> extends A<String, B1>
   * class C<C1> extends B<C1>
   * </pre>
   *
   * <p>If the current type is C then the resulting mappings are:
   *
   * <pre>
   * - A1 -> String
   * - A2 -> C1
   * - B1 -> C1
   * </pre>
   */
  @Memoized
  public Map<TypeDescriptor, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    Map<TypeDescriptor, TypeDescriptor> specializedTypeArgumentByTypeParameters = new HashMap<>();

    Map<TypeDescriptor, TypeDescriptor> immediateSpecializedTypeArgumentByTypeParameters =
        new HashMap<>();

    TypeDescriptor superTypeDescriptor = getSuperTypeDescriptor();
    List<TypeDescriptor> superTypeOrInterfaceDescriptors = new ArrayList<>();
    if (superTypeDescriptor != null) {
      superTypeOrInterfaceDescriptors.add(superTypeDescriptor);
    }
    superTypeOrInterfaceDescriptors.addAll(getInterfaceTypeDescriptors());

    for (TypeDescriptor superTypeOrInterfaceDescriptor : superTypeOrInterfaceDescriptors) {
      TypeDeclaration superTypeOrInterfaceDeclaration =
          superTypeOrInterfaceDescriptor.getTypeDeclaration();

      ImmutableList<TypeDescriptor> typeParameterDescriptors =
          superTypeOrInterfaceDeclaration.getTypeParameterDescriptors();
      ImmutableList<TypeDescriptor> typeArgumentDescriptors =
          superTypeOrInterfaceDescriptor.getTypeArgumentDescriptors();

      boolean specializedTypeIsRaw = typeArgumentDescriptors.isEmpty();
      for (int i = 0; i < typeParameterDescriptors.size(); i++) {
        TypeDescriptor typeParameterDescriptor = typeParameterDescriptors.get(i);
        TypeDescriptor typeArgumentDescriptor =
            specializedTypeIsRaw
                ? typeParameterDescriptor.getBoundTypeDescriptor().getRawTypeDescriptor()
                : typeArgumentDescriptors.get(i);
        immediateSpecializedTypeArgumentByTypeParameters.put(
            typeParameterDescriptor, typeArgumentDescriptor);
      }
      specializedTypeArgumentByTypeParameters.putAll(
          immediateSpecializedTypeArgumentByTypeParameters);

      Map<TypeDescriptor, TypeDescriptor> superSpecializedTypeArgumentByTypeParameters =
          superTypeOrInterfaceDeclaration
              .getUnsafeTypeDescriptor()
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

  public TypeDescriptor specializeTypeVariables(
      Map<TypeDescriptor, TypeDescriptor> applySpecializedTypeArgumentByTypeParameters) {
    return specializeTypeVariables(
        TypeDescriptors.mappingFunctionFromMap(applySpecializedTypeArgumentByTypeParameters));
  }

  public TypeDescriptor specializeTypeVariables(
      Function<TypeDescriptor, TypeDescriptor> replacingTypeDescriptorByTypeVariable) {
    if (replacingTypeDescriptorByTypeVariable == Function.<TypeDescriptor>identity()) {
      return this;
    }
    switch (getKind()) {
      case PRIMITIVE:
        return this;
      case ARRAY:
        return TypeDescriptors.getForArray(
            getLeafTypeDescriptor().specializeTypeVariables(replacingTypeDescriptorByTypeVariable),
            getDimensions(),
            isNullable());
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
                    .map(t -> t.specializeTypeVariables(replacingTypeDescriptorByTypeVariable))
                    .collect(toImmutableList()))
            .setJsFunctionMethodDescriptorFactory(
                () ->
                    getJsFunctionMethodDescriptor() != null
                        ? getJsFunctionMethodDescriptor()
                            .specializeTypeVariables(replacingTypeDescriptorByTypeVariable)
                        : null)
            .setConcreteJsFunctionMethodDescriptorFactory(
                () ->
                    getConcreteJsFunctionMethodDescriptor() != null
                        ? getConcreteJsFunctionMethodDescriptor()
                            .specializeTypeVariables(replacingTypeDescriptorByTypeVariable)
                        : null)
            .build();
      case TYPE_VARIABLE:
      case WILDCARD_OR_CAPTURE:
        return replacingTypeDescriptorByTypeVariable.apply(this);
      case UNION:
        return TypeDescriptors.createUnion(
            getUnionedTypeDescriptors()
                .stream()
                .map(
                    typeDescriptor ->
                        typeDescriptor.specializeTypeVariables(
                            replacingTypeDescriptorByTypeVariable))
                .collect(ImmutableList.toImmutableList()),
            getSuperTypeDescriptor() != null
                ? getSuperTypeDescriptor()
                    .specializeTypeVariables(replacingTypeDescriptorByTypeVariable)
                : null);
      default:
        throw new IllegalStateException("Intersection types should not need to be specialized");
    }
  }

  /** Returns the height of the largest inheritance chain of any interface implemented here. */
  public int getMaxInterfaceDepth() {
    return hasTypeDeclaration() ? getTypeDeclaration().getMaxInterfaceDepth() : 1;
  }
}
