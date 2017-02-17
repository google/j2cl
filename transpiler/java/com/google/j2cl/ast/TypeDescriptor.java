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
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.common.HasJsNameInfo;
import com.google.j2cl.ast.common.HasReadableDescription;
import com.google.j2cl.ast.common.JsUtils;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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
    implements Comparable<TypeDescriptor>, HasJsNameInfo, HasReadableDescription {

  /**
   * References to some descriptors need to be deferred in some cases since it will cause infinite
   * loops.
   */
  public interface DescriptorFactory<T> {
    T get(TypeDescriptor typeDescriptor);
  }

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
  public String getSimpleBinaryName() {
    return Joiner.on('$').join(getClassComponents());
  }

  /**
   * Returns the fully package qualified binary name like "com.google.common.Outer$Inner".
   *
   * <p>Used for generated class metadata (per JLS), file overview, file path, unique id calculation
   * and other similar scenarios.
   */
  // TODO(rluble): add memoization to improve performance and remove the manual memoization in
  // DescriptorFactory.
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

  /** Returns the fully package qualified name like "com.google.common". */
  @Nullable
  public abstract String getPackageName();

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

  public abstract boolean isJsFunctionImplementation();

  public abstract boolean isJsFunctionInterface();

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
    return getTypeDeclaration().isLocal();
  }

  @Override
  public abstract boolean isNative();

  public abstract boolean isNullable();

  public boolean isJsConstructorClassOrSubclass() {
    return getTypeDeclaration().isJsConstructorClassOrSubclass();
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
  public abstract String getSimpleJsName();

  @Override
  @Nullable
  public abstract String getJsNamespace();

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

  public boolean isExtern() {
    return isNative() && hasExternNamespace();
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

  public MethodDescriptor getConcreteJsFunctionMethodDescriptor() {
    return getConcreteJsFunctionMethodDescriptorFactory().get(this);
  }

  /**
   * Returns a list of the type descriptors of interfaces that are explicitly implemented directly
   * on this type.
   */
  public ImmutableList<TypeDescriptor> getInterfaceTypeDescriptors() {
    return getInterfaceTypeDescriptorsFactory().get(this);
  }

  /**
   * Returns a set of the type descriptors of interfaces that are explicitly implemented either
   * directly on this type or on some super type or super interface.
   */
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

  public MethodDescriptor getJsFunctionMethodDescriptor() {
    return getJsFunctionMethodDescriptorFactory().get(this);
  }

  /**
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  public TypeDescriptor getRawTypeDescriptor() {
    return getRawTypeDescriptorFactory().get(this);
  }

  /** Returns the bound for a type variable. */
  public TypeDescriptor getBoundTypeDescriptor() {
    checkState(isTypeVariable() || isWildCardOrCapture());
    return getBoundTypeDescriptorFactory().get(this);
  }

  public boolean isSupertypeOf(TypeDescriptor that) {
    return that.getRawSuperTypesIncludingSelf().contains(this.getRawTypeDescriptor());
  }

  public boolean isSubtypeOf(TypeDescriptor that) {
    return getRawSuperTypesIncludingSelf().contains(that.getRawTypeDescriptor());
  }

  private Set<TypeDescriptor> allRawSupertypesIncludingSelf = null;

  private Set<TypeDescriptor> getRawSuperTypesIncludingSelf() {
    if (allRawSupertypesIncludingSelf == null) {
      allRawSupertypesIncludingSelf = new LinkedHashSet<>();
      allRawSupertypesIncludingSelf.add(getRawTypeDescriptor());
      if (getSuperTypeDescriptor() != null) {
        allRawSupertypesIncludingSelf.addAll(
            getSuperTypeDescriptor().getRawSuperTypesIncludingSelf());
      }
      for (TypeDescriptor interfaceTypeDescriptor : getInterfaceTypeDescriptors()) {
        allRawSupertypesIncludingSelf.addAll(
            interfaceTypeDescriptor.getRawSuperTypesIncludingSelf());
      }
    }
    return allRawSupertypesIncludingSelf;
  }

  /** Returns all type variables that appear in the type arguments slot(s). */
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
  public String getSimpleSourceName() {
    return AstUtils.getSimpleSourceName(getClassComponents());
  }

  /**
   * Returns the fully package qualified source name like "com.google.common.Outer.Inner". Used in
   * places where original name is useful (like aliasing, identifying the corressponding java type,
   * Debug/Error output, etc.
   */
  public String getQualifiedSourceName() {
    return Joiner.on(".")
        .skipNulls()
        .join(getPackageName(), Joiner.on(".").join(getClassComponents()));
  }

  public TypeDescriptor getSuperTypeDescriptor() {
    return getSuperTypeDescriptorFactory().get(this);
  }

  @Nullable
  public abstract TypeDeclaration getTypeDeclaration();

  /** A unique string for a give type. Used for interning. */
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
  public int hashCode() {
    return Objects.hashCode(getUniqueId());
  }

  private Map<String, MethodDescriptor> methodDescriptorsBySignature;

  /**
   * The list of methods declared in the type from the JDT. Note: this does not include methods we
   * synthesize and add to the type like bridge methods.
   */
  private Map<String, MethodDescriptor> getDeclaredMethodDescriptorsBySignature() {
    return getDeclaredMethodDescriptorsFactory().get(this);
  }

  /**
   * The list of methods in the type from the JDT. Note: this does not include methods we synthesize
   * and add to the type like bridge methods.
   */
  private Map<String, MethodDescriptor> getMethodDescriptorsBySignature() {
    // TODO(rluble): update this code to handle package private methods, bridges and verify that it
    // correctly handles default methods.
    if (methodDescriptorsBySignature == null) {
      methodDescriptorsBySignature = new LinkedHashMap<>();

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
    }
    return methodDescriptorsBySignature;
  }

  /**
   * The list of methods declared in the type from the JDT. Note: this does not include methods we
   * synthesize and add to the type like bridge methods.
   */
  public Collection<MethodDescriptor> getDeclaredMethodDescriptors() {
    return getDeclaredMethodDescriptorsBySignature().values();
  }

  /**
   * Retrieves the method descriptor with name {@code name} and the corresponding parameter types if
   * there is a method with that signature.
   */
  public MethodDescriptor getMethodDescriptorByName(
      String methodName, TypeDescriptor... parameters) {
    return getMethodDescriptorsBySignature()
        .get(MethodDescriptors.getSignature(methodName, parameters));
  }

  /** The list of all methods available on a given type. */
  public Collection<MethodDescriptor> getMethodDescriptors() {
    return getMethodDescriptorsBySignature().values();
  }

  @Override
  public String toString() {
    return getUniqueId();
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    // TODO: Actually provide a real readable description.
    return getSimpleSourceName();
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_TypeDescriptor.Builder()
        // Default values.
        .setNative(false)
        .setJsFunctionInterface(false)
        .setJsFunctionImplementation(false)
        .setNullable(true)
        .setDimensions(0)
        .setUnionedTypeDescriptors(Collections.emptyList())
        .setTypeArgumentDescriptors(Collections.emptyList())
        .setBoundTypeDescriptorFactory(() -> null)
        .setConcreteJsFunctionMethodDescriptorFactory(() -> null)
        .setDeclaredMethodDescriptorsFactory(ImmutableMap::of)
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

    public abstract Builder setJsFunctionInterface(boolean isJsFunctionInterface);

    public abstract Builder setJsFunctionImplementation(boolean isJsFunctionImplementation);

    public abstract Builder setNative(boolean isNative);

    public abstract Builder setNullable(boolean isNullable);

    public abstract Builder setTypeArgumentDescriptors(
        Iterable<TypeDescriptor> typeArgumentDescriptors);

    public abstract Builder setUnionedTypeDescriptors(List<TypeDescriptor> unionedTypeDescriptors);

    public abstract Builder setPackageName(String packageName);

    public abstract Builder setSimpleJsName(String simpleJsName);

    public abstract Builder setJsNamespace(String jsNamespace);

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

    public abstract Builder setTypeDeclaration(TypeDeclaration typeDeclaration);

    // Builder accessors to aid construction.
    abstract String getPackageName();

    abstract ImmutableList<String> getClassComponents();

    abstract Optional<String> getSimpleJsName();

    abstract Optional<String> getJsNamespace();

    abstract DescriptorFactory<MethodDescriptor> getConcreteJsFunctionMethodDescriptorFactory();

    abstract DescriptorFactory<ImmutableList<TypeDescriptor>> getInterfaceTypeDescriptorsFactory();

    abstract DescriptorFactory<MethodDescriptor> getJsFunctionMethodDescriptorFactory();

    abstract DescriptorFactory<TypeDescriptor> getRawTypeDescriptorFactory();

    abstract DescriptorFactory<TypeDescriptor> getBoundTypeDescriptorFactory();

    abstract DescriptorFactory<TypeDescriptor> getSuperTypeDescriptorFactory();

    abstract DescriptorFactory<ImmutableMap<String, MethodDescriptor>>
        getDeclaredMethodDescriptorsFactory();

    abstract TypeDescriptor getEnclosingTypeDescriptor();

    abstract boolean isNative();

    private String calculateJsNamespace() {
      TypeDescriptor enclosingTypeDescriptor = getEnclosingTypeDescriptor();
      if (enclosingTypeDescriptor != null) {
        if (!isNative() && enclosingTypeDescriptor.isNative()) {
          // When there is a type nested within a native type, it's important not to generate a name
          // like "Array.1" (like would happen if the outer native type was claiming to be native
          // Array and the nested type was anonymous) since this is almost guaranteed to collide
          // with other people also creating nested classes within a native type that claims to be
          // native Array.
          return enclosingTypeDescriptor.getQualifiedSourceName();
        }
        // Use the parent namespace.
        return enclosingTypeDescriptor.getQualifiedJsName();
      }
      // Use the java package namespace.
      return getPackageName();
    }

    private static final ThreadLocalInterner<TypeDescriptor> interner = new ThreadLocalInterner<>();

    private static <T> DescriptorFactory<T> createMemoizingFactory(DescriptorFactory<T> factory) {
      // TODO(rluble): replace this by AutoValue @Memoize on the corresponding properties.
      return new DescriptorFactory<T>() {
        Map<TypeDescriptor, T> cachedValues = new HashMap<>();

        @Override
        public T get(TypeDescriptor selfTypeDescriptor) {
          return cachedValues.computeIfAbsent(
              selfTypeDescriptor, (TypeDescriptor k) -> factory.get(k));
        }
      };
    }

    abstract TypeDescriptor autoBuild();

    public TypeDescriptor build() {
      if (!getSimpleJsName().isPresent()) {
        setSimpleJsName(AstUtils.getSimpleSourceName(getClassComponents()));
      }

      if (!getJsNamespace().isPresent()) {
        setJsNamespace(calculateJsNamespace());
      }
      // Make all descriptor factories memoizing.
      setBoundTypeDescriptorFactory(createMemoizingFactory(getBoundTypeDescriptorFactory()));
      setConcreteJsFunctionMethodDescriptorFactory(
          createMemoizingFactory(getConcreteJsFunctionMethodDescriptorFactory()));
      setDeclaredMethodDescriptorsFactory(
          createMemoizingFactory(getDeclaredMethodDescriptorsFactory()));
      setInterfaceTypeDescriptorsFactory(
          createMemoizingFactory(getInterfaceTypeDescriptorsFactory()));
      setJsFunctionMethodDescriptorFactory(
          createMemoizingFactory(getJsFunctionMethodDescriptorFactory()));
      setSuperTypeDescriptorFactory(createMemoizingFactory(getSuperTypeDescriptorFactory()));
      setRawTypeDescriptorFactory(createMemoizingFactory(getRawTypeDescriptorFactory()));

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
  private Map<TypeDescriptor, TypeDescriptor> specializedTypeArgumentByTypeParameters;

  public Map<TypeDescriptor, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    if (specializedTypeArgumentByTypeParameters == null) {
      specializedTypeArgumentByTypeParameters = new HashMap<>();
      TypeDescriptor javaLangObject = TypeDescriptors.get().javaLangObject;

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
              specializedTypeIsRaw ? javaLangObject : typeArgumentDescriptors.get(i);
          specializedTypeArgumentByTypeParameters.put(
              typeParameterDescriptor, typeArgumentDescriptor);
        }

        Map<TypeDescriptor, TypeDescriptor> superSpecializedTypeArgumentByTypeParameters =
            superTypeOrInterfaceDeclaration
                .getUnsafeTypeDescriptor()
                .getSpecializedTypeArgumentByTypeParameters();

        for (Entry<TypeDescriptor, TypeDescriptor> entry :
            superSpecializedTypeArgumentByTypeParameters.entrySet()) {
          if (specializedTypeArgumentByTypeParameters.containsKey(entry.getValue())) {
            specializedTypeArgumentByTypeParameters.put(
                entry.getKey(), specializedTypeArgumentByTypeParameters.get(entry.getValue()));
          } else {
            specializedTypeArgumentByTypeParameters.put(entry.getKey(), entry.getValue());
          }
        }
      }
    }

    return specializedTypeArgumentByTypeParameters;
  }

  public TypeDescriptor specializeTypeVariables(
      Map<TypeDescriptor, TypeDescriptor> applySpecializedTypeArgumentByTypeParameters) {
    if (applySpecializedTypeArgumentByTypeParameters.isEmpty()) {
      return this;
    }

    return TypeDescriptors.replaceTypeArgumentDescriptors(
        this,
        getTypeArgumentDescriptors()
            .stream()
            .map(
                t ->
                    applySpecializedTypeArgumentByTypeParameters.containsKey(t)
                        ? applySpecializedTypeArgumentByTypeParameters.get(t)
                        : t.specializeTypeVariables(applySpecializedTypeArgumentByTypeParameters))
            .collect(toImmutableList()));
  }
}
