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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.j2cl.common.ThreadLocalInterner;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
public abstract class TypeDeclaration
    implements HasJsNameInfo, HasReadableDescription, HasUnusableByJsSuppression {

  /** Kind of type declaration. */
  public enum Kind {
    CLASS,
    ENUM,
    INTERFACE
  }

  /** Source language a type was written in. */
  public enum SourceLanguage {
    JAVA,
    KOTLIN
  }

  private static final String OVERLAY_IMPLEMENTATION_CLASS_SUFFIX = "Overlay";

  /**
   * References to some descriptors need to be deferred in some cases since it will cause infinite
   * loops.
   */
  public interface DescriptorFactory<T> {
    T get(TypeDeclaration typeDeclaration);
  }

  @Override
  public final boolean equals(Object o) {
    if (o instanceof TypeDeclaration) {
      return getUniqueId().equals(((TypeDeclaration) o).getUniqueId());
    }
    return false;
  }

  @Memoized
  public boolean declaresDefaultMethods() {
    return isInterface()
        && getDeclaredMethodDescriptors().stream().anyMatch(MethodDescriptor::isDefaultMethod);
  }

  /** Returns the unqualified simple source name like "Inner". */
  @Memoized
  public String getSimpleSourceName() {
    return AstUtils.getSimpleSourceName(getClassComponents());
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
    return AstUtils.buildQualifiedName(getPackageName(), getSimpleBinaryName());
  }

  /**
   * Returns the mangled name of a type.
   *
   * <p>The mangled name of a type is a string that uniquely identifies the type and will become
   * part of the JavaScript method name to be able to differentiate method overloads.
   */
  @Memoized
  public String getMangledName() {
    return getQualifiedSourceName().replace('.', '_');
  }

  /** Returns the globally unique qualified name by which this type should be defined/imported. */
  public String getModuleName() {
    return getQualifiedJsName();
  }

  /** Returns the type descriptor for the module that needs to be required for this type */
  @Memoized
  public TypeDeclaration getEnclosingModule() {
    String moduleRelativeJsName = getModuleRelativeJsName();
    if (!isNative() || !moduleRelativeJsName.contains(".")) {
      return this;
    }

    if (getEnclosingTypeDeclaration() != null && !hasCustomizedJsNamespace()) {
      // Make sure that if the enclosing module is a non native type, getEnclosing module returns
      // the normal Java TypeDeclaration instead of synthesizing a native one. This is important
      // because it guarantees that the type will be goog.required using the "$impl" module not the
      // header module which might cause dependency cycles.
      return getEnclosingTypeDeclaration().getEnclosingModule();
    }

    // Synthesize a module root.
    String enclosingJsName = Iterables.get(Splitter.on('.').split(moduleRelativeJsName), 0);
    String enclosingJsNamespace = getJsNamespace();
    return TypeDescriptors.createNativeTypeDescriptor(enclosingJsNamespace, enclosingJsName)
        .getTypeDeclaration();
  }

  /**
   * Returns the qualifier for the type from the root of the module, {@code ""} if the type is the
   * module root.
   */
  @Memoized
  public String getInnerTypeQualifier() {
    String moduleRelativeJsName = getModuleRelativeJsName();
    int dotIndex = moduleRelativeJsName.indexOf('.');
    if (dotIndex == -1) {
      return "";
    }

    return moduleRelativeJsName.substring(dotIndex + 1);
  }

  private boolean hasCustomizedJsNamespace() {
    return getCustomizedJsNamespace() != null;
  }

  public String getImplModuleName() {
    return isNative() ? getModuleName() : getModuleName() + "$impl";
  }

  /** Returns the fully package qualified name like "com.google.common". */
  @Nullable
  public abstract String getPackageName();

  public boolean isInSamePackage(TypeDeclaration other) {
    return getPackageName().equals(other.getPackageName());
  }

  /**
   * Returns a list of Strings representing the current type's simple name and enclosing type simple
   * names. For example for "com.google.foo.Outer" the class components are ["Outer"] and for
   * "com.google.foo.Outer.Inner" the class components are ["Outer", "Inner"].
   */
  public abstract ImmutableList<String> getClassComponents();

  /** Returns new synthesized inner class components. */
  public ImmutableList<String> synthesizeInnerClassComponents(Object... parts) {
    return ImmutableList.<String>builder()
        .addAll(getClassComponents())
        .add("$" + Joiner.on("$").skipNulls().join(parts))
        .build();
  }

  @Nullable
  public abstract TypeDeclaration getEnclosingTypeDeclaration();

  /** Returns the topmost enclosing type declaration for this type. */
  public TypeDeclaration getTopEnclosingDeclaration() {
    TypeDeclaration enclosingTypeDeclaration = getEnclosingTypeDeclaration();
    return enclosingTypeDeclaration == null
        ? this
        : enclosingTypeDeclaration.getTopEnclosingDeclaration();
  }

  /** Returns the enclosing method descriptor if the class is a local or an anonymous class. */
  @Nullable
  @Memoized
  public MethodDescriptor getEnclosingMethodDescriptor() {
    return getEnclosingMethodDescriptorFactory().get();
  }

  abstract Supplier<MethodDescriptor> getEnclosingMethodDescriptorFactory();

  public abstract ImmutableList<TypeVariable> getTypeParameterDescriptors();

  public abstract Visibility getVisibility();

  public abstract Kind getKind();

  public abstract SourceLanguage getSourceLanguage();

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

  public boolean isAbstract() {
    return getHasAbstractModifier();
  }

  public abstract boolean isFinal();

  /** Returns whether the described type is a functional interface (JLS 9.8). */
  public abstract boolean isFunctionalInterface();

  /** Returns whether the described type has the @FunctionalInterface annotation. */
  public abstract boolean isAnnotatedWithFunctionalInterface();

  /** Returns whether the described type has the @AutoValue annotation. */
  public abstract boolean isAnnotatedWithAutoValue();

  /** Returns whether the described type has the @AutoValue.Builder annotation. */
  public abstract boolean isAnnotatedWithAutoValueBuilder();

  @Memoized
  public boolean isJsFunctionImplementation() {
    return isClass()
        && getInterfaceTypeDescriptors().stream().anyMatch(TypeDescriptor::isJsFunctionInterface);
  }

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

  @Nullable
  public abstract JsEnumInfo getJsEnumInfo();

  @Nullable
  public abstract String getWasmInfo();

  public boolean isKtNative() {
    return getKtTypeInfo() != null;
  }

  @Nullable
  abstract KtTypeInfo getKtTypeInfo();

  @Nullable
  abstract KtObjcInfo getKtObjcInfo();

  public abstract boolean isDeprecated();

  public boolean isJsEnum() {
    return getJsEnumInfo() != null;
  }

  /** Returns true if the class captures its enclosing instance */
  public abstract boolean isCapturingEnclosingInstance();

  @Memoized
  public boolean isExtern() {
    return isNative() && hasExternNamespace();
  }

  public boolean isStarOrUnknown() {
    return getSimpleJsName().equals("*") || getSimpleJsName().equals("?");
  }

  private boolean hasExternNamespace() {
    // A native type descriptor is an extern if its namespace is the global namespace or if
    // it inherited the namespace from its (enclosing) extern type.
    return JsUtils.isGlobal(getJsNamespace())
        || (!hasCustomizedJsNamespace()
            && getEnclosingTypeDeclaration() != null
            && getEnclosingTypeDeclaration().isExtern());
  }

  public boolean hasTypeParameters() {
    return !getTypeParameterDescriptors().isEmpty();
  }

  @Memoized
  public boolean implementsInterfaces() {
    return getAllSuperTypesIncludingSelf().stream().anyMatch(TypeDeclaration::isInterface);
  }

  @Memoized
  public boolean extendsNativeClass() {
    TypeDeclaration superType = getSuperTypeDeclaration();
    if (superType == null) {
      return false;
    }

    return superType.isNative() || superType.extendsNativeClass();
  }

  /** Returns the depths of this type in the class hierarchy tree. */
  @Memoized
  public int getClassHierarchyDepth() {
    if (getSuperTypeDeclaration() == null) {
      return 1;
    }
    return getSuperTypeDeclaration().getClassHierarchyDepth() + 1;
  }

  public boolean hasJsConstructor() {
    return !getJsConstructorMethodDescriptors().isEmpty();
  }

  public boolean isJsConstructorSubtype() {
    TypeDeclaration superType = getSuperTypeDeclaration();
    return superType != null && superType.hasJsConstructor();
  }

  /** Whether cast to this type are checked or not. */
  public boolean isNoopCast() {
    if (isNative() && isJsEnum() && !getJsEnumInfo().hasCustomValue()) {
      // Nothing is known about the underlying type of Native JsEnum that don't provide a custom
      // value
      return true;
    }
    return isNative() && isInterface();
  }

  /**
   * Returns the JavaScript name for this class. This is same as simple source name unless modified
   * by JsType.
   */
  @Override
  @Nullable
  public abstract String getSimpleJsName();

  /**
   * Returns the qualifier for the type from the root of the module including the module root.
   *
   * <p>For example in the following code:
   *
   * <pre>{@code
   * class Top {
   *   @JsType(isNative = true, namespace = "foo", name = "Top.Inner")
   *   class TopInner {
   *     @JsType(isNative = true)
   *     class InnerInner {}
   *   }
   * }
   *
   * }</pre>
   *
   * <p>The module relative JS names are in order is Top, Top.Inner, Top.Inner.InnerInner.
   */
  @Memoized
  String getModuleRelativeJsName() {
    if (!isNative() || hasCustomizedJsNamespace() || getEnclosingTypeDeclaration() == null) {
      return getSimpleJsName();
    }

    String enclosingModuleRelativeName = getEnclosingTypeDeclaration().getModuleRelativeJsName();
    // enclosingModuleRelativeName can only be empty if the type has TypeDescriptors.globalNamespace
    // as an enclosing type. This could only potentially happen in synthetic type descriptors.
    return AstUtils.buildQualifiedName(enclosingModuleRelativeName, getSimpleJsName());
  }

  @Override
  @Nullable
  @Memoized
  public String getJsNamespace() {
    if (hasCustomizedJsNamespace()) {
      return getCustomizedJsNamespace();
    }

    if (getEnclosingTypeDeclaration() == null) {
      return getPackageName();
    }

    if (isNative()) {
      return getEnclosingTypeDeclaration().getJsNamespace();
    }

    if (getEnclosingTypeDeclaration().isNative()) {
      // When there is a type nested within a native type, it's important not to generate a name
      // like "Array.1" (like would happen if the outer native type was claiming to be native
      // Array and the nested type was anonymous) since this is almost guaranteed to collide
      // with other people also creating nested classes within a native type that claims to be
      // native Array.
      return getEnclosingTypeDeclaration().getQualifiedSourceName();
    }
    // Use the parent qualified name.
    return getEnclosingTypeDeclaration().getQualifiedJsName();
  }

  @Override
  @Memoized
  public String getQualifiedJsName() {
    if (JsUtils.isGlobal(getJsNamespace())) {
      return getModuleRelativeJsName();
    }
    return AstUtils.buildQualifiedName(getJsNamespace(), getModuleRelativeJsName());
  }

  @Nullable
  abstract String getCustomizedJsNamespace();

  @Nullable
  public abstract String getObjectiveCNamePrefix();

  public abstract boolean isNullMarked();

  @Memoized
  public TypeDeclaration getMetadataTypeDeclaration() {
    if (isNative() || isJsEnum()) {
      return getOverlayImplementationTypeDeclaration();
    }

    if (isJsFunctionInterface()) {
      return BootstrapType.JAVA_SCRIPT_FUNCTION.getDeclaration();
    }

    return this;
  }

  @Nullable
  public abstract TypeDeclaration getOverlaidTypeDeclaration();

  @Memoized
  public TypeDeclaration getOverlayImplementationTypeDeclaration() {
    return newBuilder()
        .setEnclosingTypeDeclaration(this)
        .setOverlaidTypeDeclaration(this)
        .setClassComponents(synthesizeInnerClassComponents(OVERLAY_IMPLEMENTATION_CLASS_SUFFIX))
        .setVisibility(Visibility.PUBLIC)
        .setKind(getKind())
        .build();
  }

  @Memoized
  public boolean hasOverlayImplementationType() {
    // TODO(b/116825224): this should just be
    //           isJsEnum() || isNative() || isJsFunctionInteface() && declaresJsOverlayMembers.
    // but there are some synthetic type descriptors created by
    // TypeDescriptors.createNativeGlobalTypeDescriptor that do are marked native and confuse the
    // rewriting of overlay references.
    return isJsEnum()
        || (isJsType() && isNative())
        || (isJsFunctionInterface() && declaresJsOverlayMembers());
  }

  private boolean declaresJsOverlayMembers() {
    return getDeclaredMethodDescriptors().stream().anyMatch(MethodDescriptor::isJsOverlay)
        || getDeclaredFieldDescriptors().stream().anyMatch(FieldDescriptor::isJsOverlay);
  }

  /**
   * Returns a list of the type descriptors of interfaces that are explicitly implemented directly
   * on this type.
   */
  @Memoized
  public ImmutableList<DeclaredTypeDescriptor> getInterfaceTypeDescriptors() {
    return getInterfaceTypeDescriptorsFactory().get(this);
  }

  /** Returns the height of the largest inheritance chain of any interface implemented here. */
  @Memoized
  public int getMaxInterfaceDepth() {
    return 1
        + getInterfaceTypeDescriptors().stream()
            .mapToInt(i -> i.getTypeDeclaration().getMaxInterfaceDepth())
            .max()
            .orElse(0);
  }

  /**
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  @Memoized
  public DeclaredTypeDescriptor toRawTypeDescriptor() {
    return DeclaredTypeDescriptor.newBuilder()
        .setTypeDeclaration(this)
        .setEnclosingTypeDescriptor(
            applyOrNull(getEnclosingTypeDeclaration(), t -> t.toRawTypeDescriptor()))
        .setSuperTypeDescriptorFactory(
            () -> applyOrNull(getSuperTypeDescriptor(), t -> t.toRawTypeDescriptor()))
        .setInterfaceTypeDescriptorsFactory(
            () ->
                getInterfaceTypeDescriptors().stream()
                    .map(DeclaredTypeDescriptor::toRawTypeDescriptor)
                    .collect(toImmutableList()))
        .setTypeArgumentDescriptors(ImmutableList.of())
        .setDeclaredFieldDescriptorsFactory(
            () ->
                getDeclaredFieldDescriptors().stream()
                    .map(FieldDescriptor::toRawMemberDescriptor)
                    .collect(toImmutableList()))
        .setDeclaredMethodDescriptorsFactory(
            () ->
                getDeclaredMethodDescriptors().stream()
                    .map(MethodDescriptor::toRawMemberDescriptor)
                    .collect(toImmutableList()))
        .setSingleAbstractMethodDescriptorFactory(
            () ->
                applyOrNull(
                    toUnparameterizedTypeDescriptor().getSingleAbstractMethodDescriptor(),
                    t -> t.toRawMemberDescriptor()))
        .build();
  }

  /**
   * Returns the fully package qualified source name like "com.google.common.Outer.Inner". Used in
   * places where original name is useful (like aliasing, identifying the corressponding java type,
   * Debug/Error output, etc.
   */
  @Memoized
  public String getQualifiedSourceName() {
    return AstUtils.buildQualifiedName(
        Streams.concat(Stream.of(getPackageName()), getClassComponents().stream()));
  }

  @Memoized
  public String getKtSimpleName() {
    KtTypeInfo ktTypeInfo = getKtTypeInfo();
    String qualifiedName = ktTypeInfo != null ? ktTypeInfo.getQualifiedName() : null;
    return qualifiedName != null
        ? qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1)
        : getSimpleSourceName();
  }

  @Memoized
  public String getKtQualifiedName() {
    KtTypeInfo ktTypeInfo = getKtTypeInfo();
    return ktTypeInfo == null
        ? isLocal() ? getKtSimpleName() : getQualifiedSourceName()
        : ktTypeInfo.getQualifiedName();
  }

  @Nullable
  @Memoized
  public String getKtBridgeSimpleName() {
    String ktBridgeQualifiedName = getKtBridgeQualifiedName();
    if (ktBridgeQualifiedName == null) {
      return null;
    }
    return ktBridgeQualifiedName.substring(ktBridgeQualifiedName.lastIndexOf('.') + 1);
  }

  @Nullable
  @Memoized
  public String getKtBridgeQualifiedName() {
    KtTypeInfo ktTypeInfo = getKtTypeInfo();
    return ktTypeInfo != null ? ktTypeInfo.getBridgeQualifiedName() : null;
  }

  @Nullable
  @Memoized
  public String getKtCompanionQualifiedName() {
    KtTypeInfo ktTypeInfo = getKtTypeInfo();
    return ktTypeInfo == null ? null : ktTypeInfo.getCompanionQualifiedName();
  }

  @Nullable
  @Memoized
  public String getObjectiveCName() {
    KtObjcInfo ktObjcInfo = getKtObjcInfo();
    return ktObjcInfo != null ? ktObjcInfo.getObjectiveCName() : null;
  }

  @Memoized
  @Nullable
  public DeclaredTypeDescriptor getSuperTypeDescriptor() {
    return getSuperTypeDescriptorFactory().get(this);
  }

  @Nullable
  public TypeDeclaration getSuperTypeDeclaration() {
    return getSuperTypeDescriptor() == null ? null : getSuperTypeDescriptor().getTypeDeclaration();
  }

  public final boolean hasRecursiveTypeBounds() {
    return getTypeParameterDescriptors().stream().anyMatch(TypeVariable::hasRecursiveDefinition);
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
  public DeclaredTypeDescriptor toUnparameterizedTypeDescriptor() {
    return getUnparameterizedTypeDescriptorFactory().get(this);
  }

  /** A unique string for a give type. Used for interning. */
  @Memoized
  public String getUniqueId() {
    String uniqueKey = getQualifiedBinaryName();
    return uniqueKey + TypeDeclaration.createTypeParametersUniqueId(getTypeParameterDescriptors());
  }

  private static String createTypeParametersUniqueId(List<TypeVariable> typeParameterDescriptors) {
    if (typeParameterDescriptors == null || typeParameterDescriptors.isEmpty()) {
      return "";
    }
    return typeParameterDescriptors.stream()
        .map(TypeVariable::getUniqueId)
        .collect(joining(", ", "<", ">"));
  }

  @Override
  @Memoized
  public int hashCode() {
    return getUniqueId().hashCode();
  }

  /** Returns {@code true} if {@code this} is subtype of {@code that}. */
  public boolean isSubtypeOf(TypeDeclaration that) {
    // TODO(b/70951075): distinguish between Java isSubtypeOf and our target interpretation of
    // isSubtypeOf for optimization purposes in the context of jsinterop. Note that this method is
    // used assuming it provides Java semantics.
    return TypeDescriptors.isJavaLangObject(that.toUnparameterizedTypeDescriptor())
        || getAllSuperTypesIncludingSelf().contains(that);
  }

  @Memoized
  public Set<TypeDeclaration> getAllSuperTypesIncludingSelf() {
    Set<TypeDeclaration> allSupertypesIncludingSelf = new LinkedHashSet<>();
    allSupertypesIncludingSelf.add(this);
    toUnparameterizedTypeDescriptor()
        .getSuperTypesStream()
        .forEach(
            t ->
                allSupertypesIncludingSelf.addAll(
                    t.getTypeDeclaration().getAllSuperTypesIncludingSelf()));
    return allSupertypesIncludingSelf;
  }

  /**
   * The list of methods declared in the type. Note: this does not include methods synthetic methods
   * (like bridge methods) nor supertype methods that are not overridden in the type.
   */
  @Memoized
  public ImmutableList<MethodDescriptor> getDeclaredMethodDescriptors() {
    return getDeclaredMethodDescriptorsFactory().get(this);
  }

  /** Returns the JsConstructor for this class if any. */
  @Memoized
  @Nullable
  public List<MethodDescriptor> getJsConstructorMethodDescriptors() {
    return getDeclaredMethodDescriptors().stream()
        .filter(MethodDescriptor::isJsConstructor)
        .collect(toImmutableList());
  }
  /**
   * The list of fields declared in the type. Note: this does not include methods synthetic fields
   * (like captures) nor supertype fields.
   */
  @Memoized
  public ImmutableList<FieldDescriptor> getDeclaredFieldDescriptors() {
    return getDeclaredFieldDescriptorsFactory().get(this);
  }

  @Override
  public final String toString() {
    return getUniqueId();
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    // TODO(rluble): Actually provide a real readable description.
    if (isAnonymous()) {
      if (getInterfaceTypeDescriptors().isEmpty()) {
        return "<anonymous> extends " + getSuperTypeDescriptor().getReadableDescription();
      } else {
        return "<anonymous> implements "
            + getInterfaceTypeDescriptors().get(0).getReadableDescription();
      }
    } else if (isLocal()) {
      return getSimpleSourceName().replaceFirst("\\$\\d+", "");
    }
    return getSimpleSourceName();
  }

  /* PRIVATE AUTO_VALUE PROPERTIES */

  abstract boolean getHasAbstractModifier();

  @Nullable
  abstract DescriptorFactory<ImmutableList<DeclaredTypeDescriptor>>
      getInterfaceTypeDescriptorsFactory();

  abstract DescriptorFactory<DeclaredTypeDescriptor> getUnparameterizedTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<DeclaredTypeDescriptor> getSuperTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<MethodDescriptor>> getDeclaredMethodDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<FieldDescriptor>> getDeclaredFieldDescriptorsFactory();

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    DescriptorFactory<DeclaredTypeDescriptor> unparameterizedFactory =
        // TODO(b/117105240): Remove once the type declaration is properly decoupled from the
        // type descriptor. For now we need to set all the fields that might be parameterized to
        // provide a reasonable default for the unparameterized type descriptor factory.
        self ->
            DeclaredTypeDescriptor.newBuilder()
                .setTypeDeclaration(self)
                .setEnclosingTypeDescriptor(
                    applyOrNull(
                        self.getEnclosingTypeDeclaration(),
                        t -> t.toUnparameterizedTypeDescriptor()))
                .setSuperTypeDescriptorFactory(self::getSuperTypeDescriptor)
                .setInterfaceTypeDescriptorsFactory(self::getInterfaceTypeDescriptors)
                .setTypeArgumentDescriptors(self.getTypeParameterDescriptors())
                .build();

    return new AutoValue_TypeDeclaration.Builder()
        // Default values.
        .setVisibility(Visibility.PUBLIC)
        .setSourceLanguage(SourceLanguage.JAVA)
        .setHasAbstractModifier(false)
        .setAnonymous(false)
        .setNative(false)
        .setCapturingEnclosingInstance(false)
        .setFinal(false)
        .setFunctionalInterface(false)
        .setAnnotatedWithFunctionalInterface(false)
        .setAnnotatedWithAutoValue(false)
        .setAnnotatedWithAutoValueBuilder(false)
        .setJsFunctionInterface(false)
        .setJsType(false)
        .setLocal(false)
        .setUnusableByJsSuppressed(false)
        .setDeprecated(false)
        .setNullMarked(false)
        .setTypeParameterDescriptors(ImmutableList.of())
        .setDeclaredMethodDescriptorsFactory(() -> ImmutableList.of())
        .setDeclaredFieldDescriptorsFactory(() -> ImmutableList.of())
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.of())
        .setEnclosingMethodDescriptorFactory(() -> null)
        .setUnparameterizedTypeDescriptorFactory(unparameterizedFactory)
        .setSuperTypeDescriptorFactory(() -> null);
  }

  private static <T, U> U applyOrNull(T descriptor, Function<T, U> function) {
    return descriptor == null ? null : function.apply(descriptor);
  }

  // TODO(b/181615162): This is a temporary hack to be able to reuse bridging logic in Closure
  // and Wasm.
  private static final ThreadLocal<Boolean> ignoreJsEnumAnnotations =
      ThreadLocal.withInitial(() -> false);

  public static void setIgnoreJsEnumAnnotations() {
    ignoreJsEnumAnnotations.set(true);
  }

  /** Builder for a TypeDeclaration. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAnonymous(boolean isAnonymous);

    public abstract Builder setClassComponents(List<String> classComponents);

    public abstract Builder setEnclosingTypeDeclaration(TypeDeclaration enclosingTypeDeclaration);

    public abstract Builder setEnclosingMethodDescriptorFactory(
        Supplier<MethodDescriptor> enclosingMethodDescriptorFactory);

    public abstract Builder setOverlaidTypeDeclaration(TypeDeclaration typeDeclaration);

    public abstract Builder setHasAbstractModifier(boolean hasAbstractModifier);

    public abstract Builder setKind(Kind kind);

    public abstract Builder setSourceLanguage(SourceLanguage sourceLanguage);

    public abstract Builder setCapturingEnclosingInstance(boolean capturingEnclosingInstance);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setFunctionalInterface(boolean isFunctionalInterface);

    public abstract Builder setAnnotatedWithFunctionalInterface(boolean isAnnotated);

    public abstract Builder setAnnotatedWithAutoValue(boolean annotatedWithAutoValue);

    public abstract Builder setAnnotatedWithAutoValueBuilder(boolean annotatedWithAutoValueBuilder);

    public abstract Builder setJsFunctionInterface(boolean isJsFunctionInterface);

    public abstract Builder setJsType(boolean isJsType);

    public abstract Builder setJsEnumInfo(JsEnumInfo jsEnumInfo);

    public abstract Builder setWasmInfo(String wasmInfo);

    public abstract Builder setUnusableByJsSuppressed(boolean isUnusableByJsSuppressed);

    public abstract Builder setDeprecated(boolean isDeprecated);

    public abstract Builder setLocal(boolean local);

    public abstract Builder setNative(boolean isNative);

    public abstract Builder setKtTypeInfo(KtTypeInfo ktTypeInfo);

    public abstract Builder setKtObjcInfo(KtObjcInfo ktObjcInfo);

    public abstract Builder setTypeParameterDescriptors(
        Iterable<TypeVariable> typeParameterDescriptors);

    public abstract Builder setVisibility(Visibility visibility);

    public abstract Builder setPackageName(String packageName);

    public abstract Builder setSimpleJsName(String simpleJsName);

    public abstract Builder setCustomizedJsNamespace(String jsNamespace);

    public abstract Builder setObjectiveCNamePrefix(String objectiveCNamePreifx);

    public abstract Builder setNullMarked(boolean isNullMarked);

    public abstract Builder setInterfaceTypeDescriptorsFactory(
        DescriptorFactory<ImmutableList<DeclaredTypeDescriptor>> interfaceTypeDescriptorsFactory);

    public Builder setInterfaceTypeDescriptorsFactory(
        Supplier<ImmutableList<DeclaredTypeDescriptor>> interfaceTypeDescriptorsFactory) {
      return setInterfaceTypeDescriptorsFactory(
          typeDescriptor -> interfaceTypeDescriptorsFactory.get());
    }

    public abstract Builder setSuperTypeDescriptorFactory(
        DescriptorFactory<DeclaredTypeDescriptor> superTypeDescriptorFactory);

    public Builder setSuperTypeDescriptorFactory(
        Supplier<DeclaredTypeDescriptor> superTypeDescriptorFactory) {
      return setSuperTypeDescriptorFactory(typeDescriptor -> superTypeDescriptorFactory.get());
    }

    public abstract Builder setUnparameterizedTypeDescriptorFactory(
        DescriptorFactory<DeclaredTypeDescriptor> unparameterizedTypeDescriptorFactory);

    public Builder setUnparameterizedTypeDescriptorFactory(
        Supplier<DeclaredTypeDescriptor> unparameterizedTypeDescriptorFactory) {
      return setUnparameterizedTypeDescriptorFactory(
          typeDescriptor -> unparameterizedTypeDescriptorFactory.get());
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

    // Builder accessors to aid construction.
    abstract Optional<String> getPackageName();

    abstract ImmutableList<String> getClassComponents();

    abstract Optional<String> getSimpleJsName();

    abstract Optional<TypeDeclaration> getEnclosingTypeDeclaration();

    abstract Optional<JsEnumInfo> getJsEnumInfo();

    abstract Kind getKind();

    private static final ThreadLocalInterner<TypeDeclaration> interner =
        new ThreadLocalInterner<>();

    abstract TypeDeclaration autoBuild();

    public TypeDeclaration build() {
      if (getJsEnumInfo().isPresent()) {
        if (ignoreJsEnumAnnotations.get()) {
          setJsEnumInfo(null);
        } else {
          // The actual supertype for JsEnums is Object. JsEnum don't really extend Enum
          // and modeling that fact in the type model allows passes that query assignability (e.g.
          // to implement casts, instance ofs and JsEnum boxing etc.) to get the right answer.
          if (getKind() == Kind.ENUM) {
            // Users can write code that marks an interface as JsEnum that will be rejected
            // by JsInteropRestrictionsChecker, but we need to preserve our invariants that
            // interfaces don't have supertype.
            setSuperTypeDescriptorFactory(() -> TypeDescriptors.get().javaLangObject);
          }
        }
      }

      if (!getPackageName().isPresent() && getEnclosingTypeDeclaration().isPresent()) {
        setPackageName(getEnclosingTypeDeclaration().get().getPackageName());
      }

      if (!getSimpleJsName().isPresent()) {
        setSimpleJsName(AstUtils.getSimpleSourceName(getClassComponents()));
      }

      TypeDeclaration typeDeclaration = autoBuild();

      // If this is an inner class, make sure the package is consistent.
      checkState(
          typeDeclaration.getEnclosingTypeDeclaration() == null
              || typeDeclaration
                  .getEnclosingTypeDeclaration()
                  .getPackageName()
                  .equals(typeDeclaration.getPackageName()));

      // Has to be an interface to be a functional interface.
      checkState(typeDeclaration.isInterface() || !typeDeclaration.isFunctionalInterface());

      checkState(
          typeDeclaration.getTypeParameterDescriptors().stream()
              .noneMatch(TypeVariable::isWildcardOrCapture));

      checkState(
          typeDeclaration.getTypeParameterDescriptors().stream()
              .noneMatch(TypeVariable::isNullable));

      return interner.intern(typeDeclaration);
    }

    public static Builder from(TypeDeclaration typeDeclaration) {
      return typeDeclaration.toBuilder();
    }
  }
}
