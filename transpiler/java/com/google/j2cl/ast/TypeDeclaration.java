/*
 * Copyright 2017 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.common.HasJsNameInfo;
import com.google.j2cl.ast.common.HasReadableDescription;
import com.google.j2cl.ast.common.JsUtils;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A declaration-site reference to a type.
 *
 * <p>This class is mostly a bag of precomputed properties, and the details of how those properties
 * are created live in several creation functions in JdtUtils and TypeDeclarations.
 *
 * <p>A couple of properties are lazily calculated via the DescriptorFactory and interface, since
 * eagerly calculating them would lead to infinite loops of Descriptor creation.
 *
 * <p>Since these are all declaration-site references, when there are type variables they are always
 * thought of as type parameters.
 */
@AutoValue
@Visitable
public abstract class TypeDeclaration extends Node
    implements HasJsNameInfo, HasReadableDescription, HasUnusableByJsSuppression {
  /**
   * References to some descriptors need to be deferred in some cases since it will cause infinite
   * loops.
   */
  public interface DescriptorFactory<T> {
    T get(TypeDeclaration typeDeclaration);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeDeclaration.visit(processor, this);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TypeDeclaration) {
      return getUniqueId().equals(((TypeDeclaration) o).getUniqueId());
    }
    return false;
  }

  public boolean declaresDefaultMethods() {
    return isInterface()
        && getDeclaredMethodDescriptors().stream().anyMatch(MethodDescriptor::isDefaultMethod);
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
  public abstract TypeDeclaration getEnclosingTypeDeclaration();

  public abstract ImmutableList<TypeDescriptor> getTypeParameterDescriptors();

  public abstract Visibility getVisibility();

  public abstract Kind getKind();

  public abstract boolean isAbstract();

  public abstract boolean isFinal();

  public abstract boolean isFunctionalInterface();

  public abstract boolean isJsFunctionImplementation();

  public abstract boolean isJsFunctionInterface();

  public abstract boolean isJsType();

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
  public abstract boolean isLocal();

  public abstract boolean isAnonymous();

  @Override
  public abstract boolean isNative();

  public boolean hasJsConstructor() {
    return !getJsConstructorMethodDescriptors().isEmpty();
  }

  public boolean isJsConstructorSubtype() {
    TypeDescriptor superTypeDescriptor = getSuperTypeDescriptor();
    return superTypeDescriptor != null && superTypeDescriptor.hasJsConstructor();
  }



  /* PRIVATE AUTO_VALUE PROPERTIES */

  @Nullable
  abstract DescriptorFactory<ImmutableList<TypeDescriptor>> getInterfaceTypeDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<TypeDescriptor> getRawTypeDescriptorFactory();

  abstract DescriptorFactory<TypeDescriptor> getUnsafeTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<TypeDescriptor> getSuperTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableMap<String, MethodDescriptor>>
      getDeclaredMethodDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<FieldDescriptor>> getDeclaredFieldDescriptorsFactory();

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
  public abstract boolean isCapturingEnclosingInstance();

  public boolean hasTypeParameters() {
    return !getTypeParameterDescriptors().isEmpty();
  }

  /** Returns whether the described type is a class. */
  public boolean isClass() {
    return getKind() == Kind.CLASS;
  }

  /** Returns whether the described type is an interface. */
  public boolean isInterface() {
    return getKind() == Kind.INTERFACE;
  }

  /** Returns whether the described type is an enum. */
  public boolean isEnum() {
    return getKind() == Kind.ENUM;
  }

  public boolean isExtern() {
    return JsUtils.isGlobal(getJsNamespace()) && isNative();
  }

  public boolean isStarOrUnknown() {
    return getSimpleJsName().equals("*") || getSimpleJsName().equals("?");
  }

  @Memoized
  public TypeDescriptor getOverlayImplementationTypeDescriptor() {
    return TypeDescriptors.createOverlayImplementationClassTypeDescriptor(
        getUnsafeTypeDescriptor());
  }

  public boolean hasOverlayImplementationType() {
    return (isJsType() && isNative()) || declaresDefaultMethods();
  }

  /**
   * Returns a list of the type descriptors of interfaces that are explicitly implemented directly
   * on this type.
   */
  @Memoized
  public ImmutableList<TypeDescriptor> getInterfaceTypeDescriptors() {
    return getInterfaceTypeDescriptorsFactory().get(this);
  }

  /** Returns the height of the largest inheritance chain of any interface implemented here. */
  @Memoized
  public int getMaxInterfaceDepth() {
    return 1
        + getInterfaceTypeDescriptors()
            .stream()
            .mapToInt(
                interfaceTypeDescriptor ->
                    interfaceTypeDescriptor
                        .getTypeDeclaration()
                        .getUnsafeTypeDescriptor()
                        .getMaxInterfaceDepth())
            .max()
            .orElse(0);
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

  /**
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  @Memoized
  public @Nullable TypeDescriptor getRawTypeDescriptor() {
    return getRawTypeDescriptorFactory().get(this);
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
  public @Nullable TypeDescriptor getSuperTypeDescriptor() {
    return getSuperTypeDescriptorFactory().get(this);
  }

  /**
   * Returns the usage site TypeDescriptor corresponding to this declaration site TypeDeclaration.
   *
   * <p>A completely correct solution would specialize type parameters into type arguments and
   * cascade those changes into declared methods and modifications to the method declaration site of
   * declared methods. But our AST is not in a position to do all of that. Instead we trust that a
   * real JDT usage site TypeBinding has already been processed somewhere and we attempt to retrieve
   * the matching TypeDescriptor.
   */
  @Memoized
  public TypeDescriptor getUnsafeTypeDescriptor() {
    return getUnsafeTypeDescriptorFactory().get(this);
  }

  /** A unique string for a give type. Used for interning. */
  @Memoized
  public String getUniqueId() {
    String uniqueKey = getQualifiedBinaryName();
    return uniqueKey + TypeDeclaration.createTypeParametersUniqueId(getTypeParameterDescriptors());
  }

  private static String createTypeParametersUniqueId(
      List<TypeDescriptor> typeParameterDescriptors) {
    if (typeParameterDescriptors == null || typeParameterDescriptors.isEmpty()) {
      return "";
    }
    return J2clUtils.format(
        "<%s>",
        typeParameterDescriptors.stream().map(TypeDescriptor::getUniqueId).collect(joining(", ")));
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
   * Returns true if {@code TypeDescriptor} declares a method with the same signature as {@code
   * methodDescriptor} in its body.
   */
  private boolean isOverriddenHere(MethodDescriptor methodDescriptor) {
    for (MethodDescriptor declaredMethodDescriptor : getDeclaredMethodDescriptors()) {
      if (methodDescriptor.isOverride(declaredMethodDescriptor)) {
        return true;
      }
    }
    return false;
  }

  /**
   * The list of methods declared in the type. Note: this does not include methods synthetic methods
   * (like bridge methods) nor supertype methods that are not overridden in the type.
   */
  @Memoized
  public Collection<MethodDescriptor> getDeclaredMethodDescriptors() {
    return getDeclaredMethodDescriptorsBySignature().values();
  }

  /** Returns the JsConstructor for this class if any. */
  @Memoized
  @Nullable
  public List<MethodDescriptor> getJsConstructorMethodDescriptors() {
    return getDeclaredMethodDescriptors()
        .stream()
        .filter(MethodDescriptor::isJsConstructor)
        .collect(ImmutableList.toImmutableList());
  }
  /**
   * The list of fields declared in the type. Note: this does not include methods synthetic fields
   * (like captures) nor supertype fields.
   */
  @Memoized
  public Collection<FieldDescriptor> getDeclaredFieldDescriptors() {
    return getDeclaredFieldDescriptorsFactory().get(this);
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

  /**
   * Returns the method descriptors in this type's interfaces that are accidentally overridden.
   *
   * <p>'Accidentally overridden' means the type itself does not have its own declared overriding
   * method and the method it inherits does not really override, but just has the same signature as
   * the overridden method.
   */
  @Memoized
  public List<MethodDescriptor> getAccidentallyOverriddenMethodDescriptors() {
    List<MethodDescriptor> accidentalOverriddenMethods = new ArrayList<>();

    Set<TypeDescriptor> transitiveSuperTypeInterfaceTypeDescriptors =
        getSuperTypeDescriptor() != null
            ? getSuperTypeDescriptor().getTransitiveInterfaceTypeDescriptors()
            : ImmutableSet.of();
    for (TypeDescriptor superInterfaceTypeDescriptor :
        Sets.difference(
            getTransitiveInterfaceTypeDescriptors(), transitiveSuperTypeInterfaceTypeDescriptors)) {
      accidentalOverriddenMethods.addAll(
          getNotOverriddenMethodDescriptors(superInterfaceTypeDescriptor));
    }

    return accidentalOverriddenMethods;
  }

  /**
   * Builds and caches a mapping from method override signature to matching method descriptors from
   * the entire super-type hierarchy. This map can *greatly* speed up method override checks.
   */
  @Memoized
  Multimap<String, MethodDescriptor> getMethodDescriptorsByOverrideSignature() {
    Multimap<String, MethodDescriptor> methodDescriptorsByOverrideSignature =
        LinkedHashMultimap.create();

    for (MethodDescriptor declaredMethodDescriptor : getDeclaredMethodDescriptors()) {
      if (declaredMethodDescriptor.isConstructor() || declaredMethodDescriptor.isStatic()) {
        continue;
      }

      methodDescriptorsByOverrideSignature.put(
          declaredMethodDescriptor.getOverrideSignature(), declaredMethodDescriptor);
    }

    // Recurse into immediate super class and interfaces for overridden method.
    if (getSuperTypeDescriptor() != null) {
      methodDescriptorsByOverrideSignature.putAll(
          getSuperTypeDescriptor().getTypeDeclaration().getMethodDescriptorsByOverrideSignature());
    }

    for (TypeDescriptor interfaceTypeDescriptor : getInterfaceTypeDescriptors()) {
      methodDescriptorsByOverrideSignature.putAll(
          interfaceTypeDescriptor.getTypeDeclaration().getMethodDescriptorsByOverrideSignature());
    }

    return methodDescriptorsByOverrideSignature;
  }

  /**
   * Returns a set of the method descriptors of methods in this type's super hierarchy that are
   * overridden by {@code methodDescriptor}.
   */
  public Set<MethodDescriptor> getOverriddenMethodDescriptors(MethodDescriptor methodDescriptor) {
    Set<MethodDescriptor> overriddenMethodDescriptors =
        (Set<MethodDescriptor>)
            getMethodDescriptorsByOverrideSignature().get(methodDescriptor.getOverrideSignature());

    Set<MethodDescriptor> overriddenMethodDescriptorsExceptSelf = new HashSet<>();
    overriddenMethodDescriptorsExceptSelf.addAll(overriddenMethodDescriptors);
    overriddenMethodDescriptorsExceptSelf.remove(methodDescriptor);
    return overriddenMethodDescriptorsExceptSelf;
  }

  /** Returns the method descriptors that are declared in a particular super type but not here. */
  private List<MethodDescriptor> getNotOverriddenMethodDescriptors(
      TypeDescriptor superTypeDescriptor) {
    return superTypeDescriptor
        .getDeclaredMethodDescriptors()
        .stream()
        .filter(methodDescriptor -> !isOverriddenHere(methodDescriptor))
        .collect(toImmutableList());
  }

  /**
   * Returns the method descriptor of the nearest method in this type's super classes that overrides
   * (regularly or accidentally) {@code methodDescriptor}.
   */
  public MethodDescriptor getOverridingMethodDescriptorInSuperclasses(
      MethodDescriptor methodDescriptor) {
    TypeDescriptor superTypeDescriptor = getSuperTypeDescriptor();
    while (superTypeDescriptor != null) {
      for (MethodDescriptor superMethodDescriptor :
          superTypeDescriptor.getDeclaredMethodDescriptors()) {
        if (superMethodDescriptor.isConstructor() || superMethodDescriptor.isStatic()) {
          continue;
        }

        // TODO(stalcup): exclude package private method, and add a test for it.
        if (superMethodDescriptor.isOverride(methodDescriptor)) {
          return superMethodDescriptor;
        }
      }
      superTypeDescriptor = superTypeDescriptor.getSuperTypeDescriptor();
    }
    return null;
  }

  @Override
  public String toString() {
    return getUniqueId();
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    // TODO(stalcup): Actually provide a real readable description.
    return getSimpleSourceName();
  }

  abstract Builder toBuilder();

  public static TypeDeclaration replaceTypeParameterDescriptors(
      TypeDeclaration originalTypeDeclaration,
      Iterable<TypeDescriptor> typeParameterTypeDescriptors) {
    return Builder.from(originalTypeDeclaration)
        .setTypeParameterDescriptors(typeParameterTypeDescriptors)
        .setUnsafeTypeDescriptorFactory(
            () -> {
              TypeDescriptor originalTypeDescriptor =
                  originalTypeDeclaration.getUnsafeTypeDescriptor();
              return TypeDescriptor.Builder.from(originalTypeDescriptor)
                  .setTypeArgumentDescriptors(typeParameterTypeDescriptors)
                  .build();
            })
        .build();
  }

  static TypeDeclaration createExactly(
      final TypeDescriptor superTypeDescriptor,
      final String packageName,
      final List<String> classComponents,
      final List<TypeDescriptor> typeParameterDescriptors,
      final String jsNamespace,
      final String jsName,
      final Kind kind,
      final boolean isNative,
      final boolean isJsType) {
    Supplier<TypeDescriptor> rawTypeDescriptorFactory =
        () ->
            TypeDescriptors.createExactly(
                superTypeDescriptor != null ? superTypeDescriptor.getRawTypeDescriptor() : null,
                packageName,
                classComponents,
                Collections.emptyList(),
                jsNamespace,
                jsName,
                kind,
                isNative,
                isJsType);

    return newBuilder()
        .setClassComponents(classComponents)
        .setJsType(isJsType)
        .setNative(isNative)
        .setSimpleJsName(jsName)
        .setJsNamespace(jsNamespace)
        .setPackageName(packageName)
        .setRawTypeDescriptorFactory(rawTypeDescriptorFactory)
        .setSuperTypeDescriptorFactory(() -> superTypeDescriptor)
        .setUnsafeTypeDescriptorFactory(
            () ->
                TypeDescriptors.createExactly(
                    superTypeDescriptor,
                    packageName,
                    classComponents,
                    typeParameterDescriptors,
                    jsNamespace,
                    jsName,
                    kind,
                    isNative,
                    isJsType))
        .setTypeParameterDescriptors(typeParameterDescriptors)
        .setVisibility(Visibility.PUBLIC)
        .setKind(kind)
        .build();
  }

  public static Builder newBuilder() {
    return new AutoValue_TypeDeclaration.Builder()
        // Default values.
        .setVisibility(Visibility.PUBLIC)
        .setAbstract(false)
        .setAnonymous(false)
        .setNative(false)
        .setCapturingEnclosingInstance(false)
        .setFinal(false)
        .setFunctionalInterface(false)
        .setJsFunctionInterface(false)
        .setJsFunctionImplementation(false)
        .setJsType(false)
        .setLocal(false)
        .setUnusableByJsSuppressed(false)
        .setTypeParameterDescriptors(Collections.emptyList())
        .setDeclaredMethodDescriptorsFactory(ImmutableMap::of)
        .setDeclaredFieldDescriptorsFactory(() -> ImmutableList.of())
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.of())
        .setRawTypeDescriptorFactory(() -> null)
        .setSuperTypeDescriptorFactory(() -> null);
  }

  /** Builder for a TypeDeclaration. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAnonymous(boolean isAnonymous);

    public abstract Builder setClassComponents(List<String> classComponents);

    public abstract Builder setEnclosingTypeDeclaration(TypeDeclaration enclosingTypeDeclaration);

    public abstract Builder setAbstract(boolean isAbstract);

    public abstract Builder setKind(Kind kind);

    public abstract Builder setCapturingEnclosingInstance(boolean capturingEnclosingInstance);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setFunctionalInterface(boolean isFunctionalInterface);

    public abstract Builder setJsFunctionInterface(boolean isJsFunctionInterface);

    public abstract Builder setJsFunctionImplementation(boolean jsFunctionImplementation);

    public abstract Builder setJsType(boolean isJsType);

    public abstract Builder setUnusableByJsSuppressed(boolean isUnusableByJsSuppressed);

    public abstract Builder setLocal(boolean local);

    public abstract Builder setNative(boolean isNative);

    public abstract Builder setTypeParameterDescriptors(
        Iterable<TypeDescriptor> typeParameterDescriptors);

    public abstract Builder setVisibility(Visibility visibility);

    public abstract Builder setPackageName(String packageName);

    public abstract Builder setSimpleJsName(String simpleJsName);

    public abstract Builder setJsNamespace(String jsNamespace);

    public abstract Builder setInterfaceTypeDescriptorsFactory(
        DescriptorFactory<ImmutableList<TypeDescriptor>> interfaceTypeDescriptorsFactory);

    public Builder setInterfaceTypeDescriptorsFactory(
        Supplier<ImmutableList<TypeDescriptor>> interfaceTypeDescriptorsFactory) {
      return setInterfaceTypeDescriptorsFactory(
          typeDescriptor -> interfaceTypeDescriptorsFactory.get());
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

    public abstract Builder setUnsafeTypeDescriptorFactory(
        DescriptorFactory<TypeDescriptor> unsafeTypeDescriptorFactory);

    public Builder setUnsafeTypeDescriptorFactory(
        Supplier<TypeDescriptor> unsafeTypeDescriptorFactory) {
      return setUnsafeTypeDescriptorFactory(typeDescriptor -> unsafeTypeDescriptorFactory.get());
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

    // Builder accessors to aid construction.
    abstract String getPackageName();

    abstract ImmutableList<String> getClassComponents();

    abstract Optional<String> getSimpleJsName();

    abstract Optional<String> getJsNamespace();

    abstract TypeDeclaration getEnclosingTypeDeclaration();

    abstract boolean isNative();

    private String calculateJsNamespace() {
      TypeDeclaration enclosingTypeDeclaration = getEnclosingTypeDeclaration();
      if (enclosingTypeDeclaration != null) {
        if (!isNative() && enclosingTypeDeclaration.isNative()) {
          // When there is a type nested within a native type, it's important not to generate a name
          // like "Array.1" (like would happen if the outer native type was claiming to be native
          // Array and the nested type was anonymous) since this is almost guaranteed to collide
          // with other people also creating nested classes within a native type that claims to be
          // native Array.
          return enclosingTypeDeclaration.getQualifiedSourceName();
        }
        // Use the parent namespace.
        return enclosingTypeDeclaration.getQualifiedJsName();
      }
      // Use the java package namespace.
      return getPackageName();
    }

    private static final ThreadLocalInterner<TypeDeclaration> interner =
        new ThreadLocalInterner<>();

    abstract TypeDeclaration autoBuild();

    public TypeDeclaration build() {
      if (!getSimpleJsName().isPresent()) {
        setSimpleJsName(AstUtils.getSimpleSourceName(getClassComponents()));
      }

      if (!getJsNamespace().isPresent()) {
        setJsNamespace(calculateJsNamespace());
      }

      TypeDeclaration typeDeclaration = autoBuild();

      checkState(
          typeDeclaration.getKind() == Kind.CLASS
              || typeDeclaration.getKind() == Kind.ENUM
              || typeDeclaration.getKind() == Kind.INTERFACE
              || typeDeclaration.getKind() == Kind.PRIMITIVE);

      // Can not be both a JsFunction implementation and js function interface
      checkState(
          !typeDeclaration.isJsFunctionImplementation()
              || !typeDeclaration.isJsFunctionInterface());

      // Can not be both a JsFunction implementation and a functional interface
      checkState(
          !typeDeclaration.isJsFunctionImplementation()
              || !typeDeclaration.isFunctionalInterface());

      checkState(
          typeDeclaration
              .getTypeParameterDescriptors()
              .stream()
              .allMatch(TypeDescriptor::isTypeVariable));
      return interner.intern(typeDeclaration);
    }

    public static Builder from(TypeDeclaration typeDeclaration) {
      return typeDeclaration.toBuilder();
    }
  }
}
