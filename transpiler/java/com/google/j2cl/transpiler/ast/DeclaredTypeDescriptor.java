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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.common.collect.MoreCollectors.toOptional;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.j2cl.common.ThreadLocalInterner;
import com.google.j2cl.transpiler.ast.FieldDescriptor.FieldOrigin;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * A usage-site reference to a declared type, i.e. a class, an interface or an enum.
 *
 * <p>Some properties are lazily calculated since type relationships are a graph (not a tree) and
 * this class is a value type. Those properties are set through {@code DescriptorFactory}.
 */
@AutoValue
public abstract class DeclaredTypeDescriptor extends TypeDescriptor
    implements HasUnusableByJsSuppression {

  /**
   * References to some descriptors need to be deferred in some cases since it will cause infinite
   * loops.
   */
  public interface DescriptorFactory<T> {
    T get(DeclaredTypeDescriptor typeDescriptor);
  }

  @Nullable
  public abstract DeclaredTypeDescriptor getEnclosingTypeDescriptor();

  public abstract ImmutableList<TypeDescriptor> getTypeArgumentDescriptors();

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

  @Override
  public boolean isFunctionalInterface() {
    return getTypeDeclaration().isFunctionalInterface();
  }

  @Override
  public boolean isAnnotatedWithFunctionalInterface() {
    return getTypeDeclaration().isAnnotatedWithFunctionalInterface();
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

  public boolean isFinal() {
    return getTypeDeclaration().isFinal();
  }

  @Override
  public boolean isNative() {
    return getTypeDeclaration().isNative();
  }

  @Override
  public abstract boolean isNullable();

  public boolean isStarOrUnknown() {
    return getTypeDeclaration().isStarOrUnknown();
  }

  @Override
  public boolean isNoopCast() {
    return getTypeDeclaration().isNoopCast();
  }

  @Override
  public boolean isUnusableByJsSuppressed() {
    return getTypeDeclaration().isUnusableByJsSuppressed();
  }

  public boolean isDeprecated() {
    return getTypeDeclaration().isDeprecated();
  }

  @Override
  public JsEnumInfo getJsEnumInfo() {
    return getTypeDeclaration().getJsEnumInfo();
  }

  public boolean hasJsConstructor() {
    return getTypeDeclaration().hasJsConstructor();
  }

  public boolean hasTypeArguments() {
    return !getTypeArgumentDescriptors().isEmpty();
  }

  /* PRIVATE AUTO_VALUE PROPERTIES */
  @Nullable
  abstract DescriptorFactory<ImmutableList<MethodDescriptor>> getDeclaredMethodDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<MethodDescriptor> getSingleAbstractMethodDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<FieldDescriptor>> getDeclaredFieldDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<DeclaredTypeDescriptor>>
      getInterfaceTypeDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<DeclaredTypeDescriptor> getSuperTypeDescriptorFactory();

  /**
   * Returns a list of the type descriptors of interfaces that are explicitly implemented directly
   * on this type.
   */
  @Memoized
  public ImmutableList<DeclaredTypeDescriptor> getInterfaceTypeDescriptors() {
    return getInterfaceTypeDescriptorsFactory().get(this);
  }

  @Nullable
  @Memoized
  public MethodDescriptor getSingleAbstractMethodDescriptor() {
    return getSingleAbstractMethodDescriptorFactory().get(this);
  }

  /** Returns the single declared constructor fo this class. */
  @Memoized
  public MethodDescriptor getSingleConstructor() {
    return getDeclaredMethodDescriptors().stream()
        .filter(MethodDescriptor::isConstructor)
        .collect(onlyElement());
  }

  @Memoized
  @Nullable
  public MethodDescriptor getJsFunctionMethodDescriptor() {
    // TODO(b/208830469): When a function expression is a JsFunction we remove the type parameters
    // from the method descriptor. This is due to closure not supporting template variables in
    // function types. This removal instead should happen in the backend when emitting the type.
    return getDeclaredMethodDescriptors().stream()
        .filter(MethodDescriptor::isJsFunction)
        .findFirst()
        .map(MethodDescriptor::withoutTypeParameters)
        .orElse(null);
  }

  @Nullable
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
  public boolean isRaw() {
    return !getTypeDeclaration().getTypeParameterDescriptors().isEmpty()
        && getTypeArgumentDescriptors().isEmpty();
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

  @Override
  public boolean isSameBaseType(TypeDescriptor other) {
    if (!(other instanceof DeclaredTypeDescriptor)) {
      return false;
    }
    DeclaredTypeDescriptor otherDeclaredType = (DeclaredTypeDescriptor) other;
    return getTypeDeclaration().equals(otherDeclaredType.getTypeDeclaration());
  }

  public boolean isSubtypeOf(DeclaredTypeDescriptor that) {
    return getTypeDeclaration().isSubtypeOf(that.getTypeDeclaration());
  }

  public boolean isInSamePackage(DeclaredTypeDescriptor other) {
    return other.getTypeDeclaration().isInSamePackage(getTypeDeclaration());
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

  public String getSimpleSourceName() {
    return getTypeDeclaration().getSimpleSourceName();
  }

  /**
   * Returns the fully package qualified source name like "com.google.common.Outer.Inner". Used in
   * places where original name is useful (like aliasing, identifying the corressponding java type,
   * Debug/Error output, etc.
   */
  public String getQualifiedSourceName() {
    return getTypeDeclaration().getQualifiedSourceName();
  }

  /**
   * Returns the fully package qualified binary name like "com.google.common.Outer$Inner".
   *
   * <p>Used for generated class metadata (per JLS), file overview, file path, unique id calculation
   * and other similar scenarios.
   */
  public String getQualifiedBinaryName() {
    return getTypeDeclaration().getQualifiedBinaryName();
  }

  @Override
  public String getMangledName() {
    return getTypeDeclaration().getMangledName();
  }

  @Memoized
  @Nullable
  public DeclaredTypeDescriptor getSuperTypeDescriptor() {
    return getSuperTypeDescriptorFactory().get(this);
  }

  public abstract TypeDeclaration getTypeDeclaration();

  /** Returns the class initializer method descriptor for a particular type */
  @Memoized
  public MethodDescriptor getClinitMethodDescriptor() {
    return MethodDescriptor.newBuilder()
        .setName(MethodDescriptor.CLINIT_METHOD_NAME)
        .setEnclosingTypeDescriptor(this)
        .setOrigin(MethodOrigin.SYNTHETIC_CLASS_INITIALIZER)
        .setJsInfo(isNative() ? JsInfo.RAW_OVERLAY : JsInfo.RAW)
        .setStatic(true)
        .build();
  }

  /** Returns the instance initializer method descriptor for a particular type */
  @Memoized
  public MethodDescriptor getInitMethodDescriptor() {
    return MethodDescriptor.newBuilder()
        .setName(MethodDescriptor.INIT_METHOD_NAME)
        .setEnclosingTypeDescriptor(this)
        .setOrigin(MethodOrigin.SYNTHETIC_INSTANCE_INITIALIZER)
        .setVisibility(Visibility.PRIVATE)
        .build();
  }

  /** Returns the method descriptor for $isInstance. */
  @Memoized
  public MethodDescriptor getIsInstanceMethodDescriptor() {
    return MethodDescriptor.newBuilder()
        .setName(MethodDescriptor.IS_INSTANCE_METHOD_NAME)
        .setEnclosingTypeDescriptor(getMetadataTypeDeclaration().toUnparameterizedTypeDescriptor())
        .setParameterTypeDescriptors(TypeDescriptors.getUnknownType())
        .setReturnTypeDescriptor(PrimitiveTypes.BOOLEAN)
        .setOrigin(MethodOrigin.INSTANCE_OF_SUPPORT_METHOD)
        .setStatic(true)
        .build();
  }

  /** Returns the method descriptor for $markImplementor. */
  @Memoized
  public MethodDescriptor getMarkImplementorMethodDescriptor() {
    return MethodDescriptor.newBuilder()
        .setName(MethodDescriptor.MARK_IMPLEMENTOR_METHOD_NAME)
        .setEnclosingTypeDescriptor(getMetadataTypeDeclaration().toUnparameterizedTypeDescriptor())
        .setParameterTypeDescriptors(TypeDescriptors.get().nativeFunction)
        .setReturnTypeDescriptor(PrimitiveTypes.VOID)
        .setOrigin(MethodOrigin.INSTANCE_OF_SUPPORT_METHOD)
        .setStatic(true)
        .build();
  }

  /** Returns the field descriptor for the instanceof checking marker field. */
  public FieldDescriptor getIsInstanceMarkerField() {
    checkState(isInterface() || isJsFunctionImplementation());
    return FieldDescriptor.newBuilder()
        .setName(isJsFunctionImplementation() ? "$is" : "$implements")
        .setEnclosingTypeDescriptor(this)
        .setTypeDescriptor(PrimitiveTypes.BOOLEAN)
        .setOrigin(FieldOrigin.INSTANCE_OF_SUPPORT_FIELD)
        .build();
  }

  /** Returns the method descriptor for $copy. */
  @Memoized
  public MethodDescriptor getCopyMethodDescriptor() {
    return MethodDescriptor.newBuilder()
        .setName(MethodDescriptor.COPY_METHOD_NAME)
        .setEnclosingTypeDescriptor(
            getMetadataConstructorReference()
                .getReferencedTypeDeclaration()
                .toUnparameterizedTypeDescriptor())
        .setParameterTypeDescriptors(
            TypeDescriptors.getUnknownType(), TypeDescriptors.getUnknownType())
        .setReturnTypeDescriptor(PrimitiveTypes.VOID)
        .setOrigin(MethodOrigin.INSTANCE_OF_SUPPORT_METHOD)
        .setStatic(true)
        .build();
  }

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
   * The list of methods declared in the type. Note: this does not include methods synthetic methods
   * (like bridge methods) nor supertype methods that are not overridden in the type.
   */
  @Memoized
  public Collection<MethodDescriptor> getDeclaredMethodDescriptors() {
    return getDeclaredMethodDescriptorsFactory().get(this);
  }

  /**
   * The list of fields declared in the type. Note: this does not include synthetic fields (like
   * captures) nor supertype fields.
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

  /** Retrieves the field descriptor named {@code name} if it exists, {@code null} otherwise. */
  @Nullable
  public FieldDescriptor getFieldDescriptor(String name) {
    return getDeclaredFieldDescriptors().stream()
        .filter(f -> f.getName().equals(name))
        .collect(toOptional())
        .orElse(null);
  }

  /**
   * Retrieves the method descriptor with name {@code name} and the corresponding parameter types if
   * there is a method with that signature.
   */
  @Nullable
  public MethodDescriptor getMethodDescriptor(String methodName, TypeDescriptor... parameters) {
    String targetSignature = MethodDescriptor.buildMethodSignature(methodName, parameters);
    return getMethodDescriptors().stream()
        .filter(Predicates.not(MethodDescriptor::isGeneralizingdBridge))
        .filter(m -> m.getSignature().equals(targetSignature))
        .collect(toOptional())
        .orElse(null);
  }

  /**
   * Returns a method descriptor for a method declered in this type named {@code name}.
   *
   * <p>Fails if not method with {@code name} is declared in the type or if there are multiple
   * overloads declared in the type with the same {@code name}.
   */
  public MethodDescriptor getMethodDescriptorByName(String name) {
    return getDeclaredMethodDescriptors().stream()
        .filter(m -> m.getName().equals(name))
        .collect(onlyElement());
  }

  /** The list of all methods available on a given type. */
  private ImmutableList<MethodDescriptor> getMethodDescriptors() {
    return Streams.concat(
            getPolymorphicMethods().stream(),
            getDeclaredMethodDescriptors().stream()
                .filter(Predicates.not(MethodDescriptor::isPolymorphic)))
        .collect(toImmutableList());
  }

  // The goal of this algorithm is to find: (1) all the mangled names that this type needs to
  // respond to, and (2) which method, i.e. which implementation, should respond to the mangle name.
  //
  // The first part is easier. We can collect all the mangled names for the super type hierarchy and
  // add the ones introduced by this type.
  //
  // Once we have a list of all the mangled names for the type, we can iterate over them and see if
  // the implementation responding to it is the correct one; if it is not the correct one or there
  // is none (e.g. default method) we create a bridge method.
  //
  // The complex part is how to find the correct implementation method. To answer that we need to
  // find out how mangled names are related to each other with respect to Java override semantics.
  //
  // The key to this relation is to describe each set of related mangled names by a unique
  // "override key", and to find out which implementation method is the one that will handle the
  // mangled names in the set corresponding to each "override key".
  //
  // The method getPolymorphicMethods() does most of the hard work including the recursive discovery
  // of all the mangled names.
  //
  // The method getMangledNameToOverrideKeyMap() computes the "override key" corresponding to each
  // mangled name as a map.
  //
  // The method getOverrideKeyToTargetMap() computes the implementation method for each set also as
  // a map by the "override key".

  /**
   * Computes all the methods exposed by this type, including supertype methods and synthetic
   * bridges. In the results there is one method per mangled name handled by this type.
   */
  @Memoized
  public Collection<MethodDescriptor> getPolymorphicMethods() {
    DeclaredTypeDescriptor declaration = toUnparameterizedTypeDescriptor();
    if (!declaration.equals(this)) {
      return specializeMethods(
          declaration.getPolymorphicMethods(), getTransitiveParameterization());
    }

    // The bridges need to be computed at the type declaration in order to create them as
    // declarations. That is why the computation is performed at the unparameterized type descriptor
    // (as it is equivalent to the type declaration).

    Map<String, MethodDescriptor> methodsByMangledName = new LinkedHashMap<>();

    // 1. Add super type methods.
    getSuperTypesStream()
        .flatMap(s -> s.getPolymorphicMethods().stream())
        .forEach(m -> methodsByMangledName.put(m.getMangledName(), m));

    // 2. Add the new methods that are declared in the class, replacing the overridden methods.
    getDeclaredMethodDescriptors().stream()
        .filter(MethodDescriptor::isPolymorphic)
        .forEach(m -> methodsByMangledName.put(m.getMangledName(), m));

    if (isInterface()) {
      // Even though it is possible to do some bridge synthesis in interfaces through default
      // methods, it introduces complexity and is less optimal since the default method will
      // still need a bridge stub in the implementing class.
      // Hence all bridge stubs are synthesized in classes.
      return methodsByMangledName.values();
    }

    // At this point we have ALL the mangled names that NEED to be handled by this class and
    // what is the method that is handling those before the bridge computation.
    // In what follows we will have to determine whether the method handling the mangled name is
    // at the right target or not, and if not a bridge will be created.
    Map<String, String> overrideKeysByMangledName = getMangledNameToOverrideKeyMap();
    Map<String, MethodDescriptor> targetMethodsByOverrideKey =
        new LinkedHashMap<>(getOverrideKeyToTargetMap());

    // Bridge method creation proceeds in two stages: the first stage determines the bridges that
    // are specialized overrides of the targets (see MethodDescriptor.isSpecializingBridge); since
    // these might need to be targets of generalizing bridges (created in stage 2).
    //
    // Specialized overrides happen when an implemented interface exposes a superclass method with
    // specialized parameter types. E.g.
    //
    //  class AbstractList<T> { void add(T t) {...} }
    //  interface StringList {  void add(String s); }
    //  class MyStringList extends AbstractList<String> implements StringList {}
    //
    // In this example StringList.add specializes the parameter types from AbstractList.add. That
    // means that MyStringList would need to handle m_add__String by using m_add__Object from the
    // super class. Hence there will be a bridge. We call these bridges specializing bridges.
    // Default methods are also handled in this first stage. Both specializing bridges and default
    // method bridges might become targets of the other type of bridges.

    // Stage 1: Create specializing bridges.
    for (String mangledName : overrideKeysByMangledName.keySet()) {

      MethodDescriptor currentTarget = methodsByMangledName.get(mangledName);
      MethodDescriptor targetImplementation =
          targetMethodsByOverrideKey.get(overrideKeysByMangledName.get(mangledName));

      if (isCorrectTarget(currentTarget, targetImplementation)) {
        continue;
      }

      if (needsSpecializingBridge(targetImplementation) || targetImplementation.isDefaultMethod()) {
        MethodOrigin methodOrigin =
            targetImplementation.isDefaultMethod()
                ? MethodOrigin.DEFAULT_METHOD_BRIDGE
                : MethodOrigin.SPECIALIZING_BRIDGE;

        // Create a specializing bridge.
        MethodDescriptor newBridge =
            createBridgeMethodDescriptor(methodOrigin, currentTarget, targetImplementation);
        methodsByMangledName.put(newBridge.getMangledName(), newBridge);

        // Now that the specializing bridge is in the class, it acts exactly as if a user wrote
        // the override by hand and this method becomes the target for the other bridges.
        targetMethodsByOverrideKey.put(newBridge.getOverrideKey(), newBridge);
      }
    }

    // Stage 2: Create all standard bridges.
    for (String mangledName : overrideKeysByMangledName.keySet()) {
      MethodDescriptor currentTarget = methodsByMangledName.get(mangledName);
      MethodDescriptor targetImplementation =
          targetMethodsByOverrideKey.get(overrideKeysByMangledName.get(mangledName));

      if (isCorrectTarget(currentTarget, targetImplementation)) {
        // No need to bridge; the mangled name already dispatches to the correct target.
        continue;
      }

      // Add the bridge and note that the target might be an abstract method in a superclass
      // or an interface. This guarantees that the bridges will be in place as high up in
      // the hierarchy allowing JavaScript subclassing.
      MethodDescriptor newBridge =
          createBridgeMethodDescriptor(
              MethodOrigin.GENERALIZING_BRIDGE, currentTarget, targetImplementation);
      checkState(newBridge.isGeneralizingdBridge());
      methodsByMangledName.put(newBridge.getMangledName(), newBridge);
    }

    return methodsByMangledName.values();
  }

  private static ImmutableList<MethodDescriptor> specializeMethods(
      Collection<MethodDescriptor> methods, Map<TypeVariable, TypeDescriptor> parameterization) {
    return methods.stream()
        .map(m -> m.getDeclarationDescriptor().specializeTypeVariables(parameterization))
        .collect(toImmutableList());
  }

  /** Whether mangled name already has the actual method that handles that name. */
  private static boolean isCorrectTarget(MethodDescriptor method, MethodDescriptor newTarget) {
    if (method.isGeneralizingdBridge()) {
      // Generalizing bridges always dispatch to the right target by construction, but they might
      // do so indirectly.
      return true;
    }

    if (method.isBridgeTarget(newTarget)) {
      // There is a bridge in place that implements the forwarding to the needed target.
      return true;
    }

    if (newTarget.isDefaultMethod()) {
      // This is a default method in an interface. Because this is the target for a mangled name in
      // a class it needs a default method bridge; even if the mangled name is correct.
      return false;
    }

    if (newTarget.getMangledName().equals(method.getMangledName())) {
      // Since current target is an implementation in a class and the new target is not a default
      // method (which is the only case in which a target already implemented in a class which same
      // mangled name as the new target might be overridden) it is already correct.
      return true;
    }

    return false;
  }

  /** Computes the set of all the mangled names that should target the same implementation. */
  // TODO(b/70853239): This computation should be done in the traversal in getPolymorphicMethods(),
  // but due to inaccuracies in specializeTypeVariables it cannot be move there yet. Move
  // once specializeTypeVariables is fixed.
  @Memoized
  Map<String, String> getMangledNameToOverrideKeyMap() {
    Map<String, String> overrideKeysByMangledName = new LinkedHashMap<>();

    getSuperTypesStream()
        .forEach(t -> overrideKeysByMangledName.putAll(t.getMangledNameToOverrideKeyMap()));

    for (MethodDescriptor declaredMethod : getDeclaredMethodDescriptors()) {
      if (!declaredMethod.isPolymorphic()) {
        continue;
      }
      // TODO(b/150876433): Select the target override key in a principled manner when there is a
      // collision due to JsMethod. If it wasn't for JsMethods the mapping would be 1-1, because
      // the override key uses the same elements as the mangled name (i.e. name, parameter types
      // and the package if it is package private).
      // However, two different JsMethods with different override key might have the same name. In
      // those cases, we choose the last one in the super type hierarchy. It is safe because all
      // the override keys corresponding to a JsMethod will have the same target otherwise it
      // would not have passed restriction checking.
      overrideKeysByMangledName.put(
          declaredMethod.getMangledName(), declaredMethod.getOverrideKey());
    }

    return overrideKeysByMangledName;
  }

  /**
   * Determines the actual implementation target that will handle all the mangled names associated
   * with an override key.
   */
  // TODO(b/70853239): This computation should be done in the traversal in getPolymorphicMethods(),
  // but due to inaccuracies in specializeTypeVariables it cannot be move there yet. Move
  // once specializeTypeVariables is fixed.
  @Memoized
  Map<String, MethodDescriptor> getOverrideKeyToTargetMap() {
    Map<String, MethodDescriptor> targetByOverrideKey = new LinkedHashMap<>();

    // 1. Initially the implementations for each override key will be the same as in the superclass.
    if (getSuperTypeDescriptor() != null) {
      targetByOverrideKey.putAll(getSuperTypeDescriptor().getOverrideKeyToTargetMap());
    }

    // 2. Now replace the ones that are overridden in this class and add the new override keys and
    // their corresponding targets introduced by this class. This subclass might override supertype
    // methods and hence these overriding methods need to become the target for the corresponding
    // override key.
    getDeclaredMethodDescriptors().stream()
        .filter(MethodDescriptor::isPolymorphic)
        .forEach(
            m -> {
              targetByOverrideKey.put(m.getOverrideKey(), m);
              if (!m.getVisibility().isPackagePrivate()) {
                // The non package private methods are ALSO targets for the package private
                // signature.
                String packagePrivateOverridingKey = m.getPackagePrivateOverrideKey();
                targetByOverrideKey.put(packagePrivateOverridingKey, m);
              }
            });

    // 3. Now add the override keys and the corresponding targets introduced by interfaces. These
    // might be a new abstract method, a new default method or a default method that overrode an
    // existing target. Methods in an interface that are already implements in the class are not
    // targets but an interface might introduce a new methods (default or abstract) or might
    // override a default method that is not implemented in the class.
    for (DeclaredTypeDescriptor superInterface : getInterfaceTypeDescriptors()) {
      for (MethodDescriptor methodDescriptor :
          superInterface.getOverrideKeyToTargetMap().values()) {
        String overrideKey = methodDescriptor.getOverrideKey();
        MethodDescriptor overriddenMethod = targetByOverrideKey.get(overrideKey);
        // Looking at the superinterfaces to see if we find new targets for new override chains
        // introduced by this interface, or default methods that will need to replace an overridden
        // (default) method.
        if (overriddenMethod == null
            || isOverridingDefaultMethod(methodDescriptor, overriddenMethod)) {
          targetByOverrideKey.put(overrideKey, methodDescriptor);
        }
      }
    }

    return targetByOverrideKey;
  }

  /**
   * Returns true if {@code candidateMethod} is a default method that overrides {@code method}.
   *
   * <p>Note that in the case that a new method overrides the current method, the current method
   * might be a default method or an abstract interface method.
   */
  private static boolean isOverridingDefaultMethod(
      MethodDescriptor candidateMethod, MethodDescriptor method) {
    if (!candidateMethod.isDefaultMethod()) {
      return false;
    }

    if (!method.getEnclosingTypeDescriptor().isInterface()) {
      return false;
    }

    // Keep the method at the bottom of the hierarchy since if that one is a default method the one
    // further down has to be the target.
    TypeDeclaration candidateDeclaration =
        candidateMethod.getEnclosingTypeDescriptor().getTypeDeclaration();
    TypeDeclaration currentDeclaration = method.getEnclosingTypeDescriptor().getTypeDeclaration();
    return candidateDeclaration.getMaxInterfaceDepth() >= currentDeclaration.getMaxInterfaceDepth();
  }

  /** Returns true if the method needs a specializing bridge. */
  private static boolean needsSpecializingBridge(MethodDescriptor method) {
    // The method is a superclass method that is specialized by the current type and that
    // specialization introduced an new overridden method from some interface.
    return !method.getEnclosingTypeDescriptor().isInterface()
        && !method.getOverrideKey().equals(method.getDeclarationDescriptor().getOverrideKey());
  }

  private MethodDescriptor createBridgeMethodDescriptor(
      MethodOrigin origin,
      MethodDescriptor bridgeMethodDescriptor,
      MethodDescriptor targetMethodDescriptor) {

    return MethodDescriptor.Builder.from(
            adjustReturn(origin, targetMethodDescriptor, bridgeMethodDescriptor))
        .setJsInfo(bridgeMethodDescriptor.getJsInfo())
        .setEnclosingTypeDescriptor(this)
        .setDeclarationDescriptor(null)
        .makeBridge(origin, bridgeMethodDescriptor, targetMethodDescriptor)
        .setFinal(bridgeMethodDescriptor.isGeneralizingdBridge())
        .build();
  }

  /**
   * Adjusts the return due to Kotlin semantics for non-nullable basic types.
   *
   * <p>Note that bridges are created to satisfy a method signature, and methods implementing the
   * same signature need to agree in typing (e.g. an override needs to be a subtype of the methods
   * it overrides)
   *
   * <p>Since bridge method creation computes bridge signatures by specializing a parameterized
   * method there is some ambiguity in Kotlin whether a return type that is "Int" should be
   * considered "!java.lang.Integer" or "int". To resolve this ambiguity we need to see what is the
   * contract implemented by the bridge origin, which is the one that provides the mangled name.
   */
  // TODO(b/234498715): Extend to parameters when primitive bridges are fully implemented. Consider
  // whether it is better to just forward getParameters/getReturnType to the bridge descriptor,
  // like we forward getManglingDescriptor, or whether always override parameters/return on
  // construction of bridges.
  private MethodDescriptor adjustReturn(
      MethodOrigin origin, MethodDescriptor targetMethodDescriptor, MethodDescriptor bridgeOrigin) {
    if (origin != MethodOrigin.GENERALIZING_BRIDGE) {
      // Only generalizing bridges use the origin as their mangling descriptor.
      return targetMethodDescriptor;
    }
    if (bridgeOrigin.getReturnTypeDescriptor().isPrimitive()
        != targetMethodDescriptor.getReturnTypeDescriptor().isPrimitive()) {
      return targetMethodDescriptor.transform(
          builder -> builder.setReturnTypeDescriptor(bridgeOrigin.getReturnTypeDescriptor()));
    }
    return targetMethodDescriptor;
  }

  /** Returns the default (parameterless) constructor for the type. */
  @Memoized
  public MethodDescriptor getDefaultConstructorMethodDescriptor() {
    return getDeclaredMethodDescriptors().stream()
        .filter(MethodDescriptor::isConstructor)
        .filter(methodDescriptor -> methodDescriptor.getParameterTypeDescriptors().isEmpty())
        .findFirst()
        .get();
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

    if (isJsType()
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

  /**
   * A mapping that fully describes the final specialized type argument value for every super type
   * or interface of the current type.
   *
   * <p>For example given:
   *
   * <pre>{@code
   * class A<A1, A2> {}
   * class B<B1> extends A<String, B1>
   * class C<C1> extends B<C1>
   * }</pre>
   *
   * <p>If the current type is C then the resulting mappings are:
   *
   * <pre>{@code
   * - A1 -> String
   * - A2 -> C1
   * - B1 -> C1
   * }</pre>
   */
  @Memoized
  public Map<TypeVariable, TypeDescriptor> getTransitiveParameterization() {
    Map<TypeVariable, TypeDescriptor> specializedTypeArgumentByTypeParameters =
        new LinkedHashMap<>(getLocalParameterization());

    getSuperTypesStream()
        .forEach(
            t -> specializedTypeArgumentByTypeParameters.putAll(t.getTransitiveParameterization()));

    return specializedTypeArgumentByTypeParameters;
  }

  Stream<DeclaredTypeDescriptor> getSuperTypesStream() {
    return getSuperTypeDescriptor() == null
        ? getInterfaceTypeDescriptors().stream()
        : Stream.concat(
            getInterfaceTypeDescriptors().stream(), Stream.of(getSuperTypeDescriptor()));
  }

  private Map<TypeVariable, TypeDescriptor> getLocalParameterization() {
    ImmutableList<TypeVariable> typeVariables = getTypeDeclaration().getTypeParameterDescriptors();
    ImmutableList<TypeDescriptor> typeArguments = getTypeArgumentDescriptors();

    Map<TypeVariable, TypeDescriptor> typeArgumentsByTypeVariable = new LinkedHashMap<>();

    boolean isRaw = typeArguments.isEmpty();
    if (isRaw) {
      typeArguments =
          typeVariables.stream().map(TypeVariable::toRawTypeDescriptor).collect(toImmutableList());
    }
    Streams.forEachPair(
        typeVariables.stream(), typeArguments.stream(), typeArgumentsByTypeVariable::put);
    return typeArgumentsByTypeVariable;
  }

  @Override
  TypeDescriptor replaceInternalTypeDescriptors(TypeReplacer fn, Set<TypeDescriptor> seen) {
    ImmutableList<TypeDescriptor> typeArguments = getTypeArgumentDescriptors();
    ImmutableList<TypeDescriptor> newtypeArguments =
        replaceTypeDescriptors(typeArguments, fn, seen);
    if (!typeArguments.equals(newtypeArguments)) {
      return Builder.from(this).setTypeArgumentDescriptors(newtypeArguments).build();
    }

    // We should also re-write TypeVariable for the TypeDescriptor however the type model  currently
    // doesn't allow that since they are not part of the key. This leaves edge cases where we may
    // leave a reference however JavaScript stack will detect that.
    // Note that this limitation is acceptable since in practice user shouldn't refer to AutoValue
    // generated classes (this is where this functionality is currently only used) other than a few
    // trival scenarios. What we have here is already an overkill in practice for well formed code.
    return this;
  }

  @Override
  public DeclaredTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> parameterization) {
    if (AstUtils.isIdentityFunction(parameterization)) {
      return this;
    }
    if (getTypeArgumentDescriptors().isEmpty()) {
      return this;
    }

    return Builder.from(this)
        .setTypeArgumentDescriptors(
            getTypeArgumentDescriptors().stream()
                .map(t -> t.specializeTypeVariables(parameterization))
                .collect(toImmutableList()))
        .setSuperTypeDescriptorFactory(
            () ->
                getSuperTypeDescriptor() != null
                    ? getSuperTypeDescriptor().specializeTypeVariables(parameterization)
                    : null)
        .setInterfaceTypeDescriptorsFactory(
            () ->
                getInterfaceTypeDescriptors().stream()
                    .map(t -> t.specializeTypeVariables(parameterization))
                    .collect(toImmutableList()))
        .setSingleAbstractMethodDescriptorFactory(
            () ->
                getSingleAbstractMethodDescriptor() != null
                    ? getSingleAbstractMethodDescriptor().specializeTypeVariables(parameterization)
                    : null)
        .setDeclaredFieldDescriptorsFactory(
            () ->
                getDeclaredFieldDescriptors().stream()
                    .map(f -> f.specializeTypeVariables(parameterization))
                    .collect(toImmutableList()))
        .setDeclaredMethodDescriptorsFactory(
            () ->
                getDeclaredMethodDescriptors().stream()
                    .map(m -> m.specializeTypeVariables(parameterization))
                    .collect(toImmutableList()))
        .setEnclosingTypeDescriptor(
            getEnclosingTypeDescriptor() != null
                ? getEnclosingTypeDescriptor().specializeTypeVariables(parameterization)
                : null)
        .build();
  }

  @Override
  public String getReadableDescription() {
    return getTypeDeclaration().getReadableDescription()
        + (hasTypeArguments()
            ? getTypeArgumentDescriptors().stream()
                .map(TypeDescriptor::getReadableDescription)
                .collect(joining(", ", "<", ">"))
            : "");
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_DeclaredTypeDescriptor.Builder()
        // Default values.
        .setNullable(true)
        .setTypeArgumentDescriptors(ImmutableList.of())
        .setDeclaredMethodDescriptorsFactory(() -> ImmutableList.of())
        .setDeclaredFieldDescriptorsFactory(() -> ImmutableList.of())
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.of())
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
        DescriptorFactory<ImmutableList<MethodDescriptor>> declaredMethodDescriptorsFactory);

    public Builder setDeclaredMethodDescriptorsFactory(
        Supplier<ImmutableList<MethodDescriptor>> declaredMethodDescriptorsFactory) {
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

    abstract TypeDeclaration getTypeDeclaration();

    abstract DeclaredTypeDescriptor autoBuild();

    @SuppressWarnings("ReferenceEquality")
    public DeclaredTypeDescriptor build() {
      if (getTypeDeclaration().isJsEnum()) {
        // JsEnums don't extend Enum but Object. Fix it up on construction.
        setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject);
      }

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
