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
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.j2cl.common.ThreadLocalInterner;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import com.google.j2cl.transpiler.ast.TypeDescriptors.BootstrapType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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
@Visitable
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

  /** The origin of the class. */
  public enum Origin {
    SOURCE,
    LAMBDA_ABSTRACT_ADAPTOR,
    LAMBDA_IMPLEMENTOR
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

  /**
   * Returns the unqualified simple source name as written in the source, {@code null} if the class
   * does not have a name, i.e. it is an anonymous class or a synthetic class.
   */
  @Nullable
  public abstract String getOriginalSimpleSourceName();

  /**
   * Returns the unqualified name like "Inner".
   *
   * <p>Note: this is the simple name of the class and might or might not be the original simple
   * source name, e.g. for local classes it returns a synthetic name that make them unique as inner
   * classes of their enclosing types; it also returns names for classes that don't have source
   * names, e.g. anonymous and synthetic classes.
   */
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

  public boolean hasCustomizedJsNamespace() {
    return getCustomizedJsNamespace() != null;
  }

  public String getImplModuleName() {
    return isNative() ? getModuleName() : getModuleName() + "$impl";
  }

  public abstract PackageDeclaration getPackage();

  /** Returns the fully package qualified name like "com.google.common". */
  public String getPackageName() {
    return getPackage().getName();
  }

  public boolean isInSamePackage(TypeDeclaration other) {
    return getPackage() == other.getPackage();
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

  public abstract Origin getOrigin();

  abstract Supplier<MethodDescriptor> getEnclosingMethodDescriptorFactory();

  public abstract ImmutableList<TypeVariable> getTypeParameterDescriptors();

  public abstract Visibility getVisibility();

  public abstract Kind getKind();

  public abstract boolean isAnnotation();

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

  // TODO(b/322906767): Remove when the bug is fixed.
  private static final boolean PRESERVE_EQUALS_FOR_JSTYPE_INTERFACE =
      "true"
          .equals(
              System.getProperty(
                  "com.google.j2cl.transpiler.backend.kotlin.preserveEqualsForJsTypeInterface"));

  public boolean isKtFunctionalInterface() {
    if (!isFunctionalInterface()) {
      return false;
    }

    if (getAllSuperTypesIncludingSelf().stream()
        .filter(TypeDeclaration::isInterface)
        // TODO(b/317299672): Remove JsType special casing since should preserve all of them for
        // migration purposes.
        .filter(t -> PRESERVE_EQUALS_FOR_JSTYPE_INTERFACE && t.isJsType())
        .flatMap(t -> t.getDeclaredMethodDescriptors().stream())
        .anyMatch(MethodDescriptor::isOrOverridesJavaLangObjectMethod)) {
      // If the interface has an explicit {@code java.lang.Object} method, it is not considered to
      // be functional in Kotlin.
      return false;
    }

    MethodDescriptor methodDescriptor = getSingleAbstractMethodDescriptor();
    return !methodDescriptor.isKtProperty()
        && methodDescriptor.getTypeParameterTypeDescriptors().isEmpty();
  }

  /** Returns whether the described type is a functional interface (JLS 9.8). */
  public abstract boolean isFunctionalInterface();

  /** Returns whether the described type has the @FunctionalInterface annotation. */
  public abstract boolean isAnnotatedWithFunctionalInterface();

  /** Returns whether the described type has the @AutoValue annotation. */
  public abstract boolean isAnnotatedWithAutoValue();

  /** Returns whether the described type has the @AutoValue.Builder annotation. */
  public abstract boolean isAnnotatedWithAutoValueBuilder();

  /**
   * Returns whether the described type is a test class, i.e. has the JUnit @RunWith annotation
   * or @RunParameterized annotation.
   */
  public abstract boolean isTestClass();

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

  public boolean implementsInterfaces() {
    return !getAllSuperInterfaces().isEmpty();
  }

  @Memoized
  public boolean extendsNativeClass() {
    TypeDeclaration superType = getSuperTypeDeclaration();
    if (superType == null) {
      return false;
    }

    return superType.isNative() || superType.extendsNativeClass();
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
      return getPackage().getJsNamespace();
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
    if (isNative() || (isJsEnum() && AstUtils.isJsEnumBoxingSupported())) {
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
    //           isJsEnum() || isNative() || isJsFunctionInterface() && declaresJsOverlayMembers.
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

  /**
   * Returns the depth of this type in the type hierarchy tree, including classes and interfaces.
   */
  @Memoized
  public int getTypeHierarchyDepth() {
    return 1
        + Stream.concat(Stream.of(getSuperTypeDescriptor()), getInterfaceTypeDescriptors().stream())
            .filter(Predicates.notNull())
            .mapToInt(i -> i.getTypeDeclaration().getTypeHierarchyDepth())
            .max()
            .orElse(0);
  }

  /**
   * Returns the fully package qualified source name like "com.google.common.Outer.Inner". Used in
   * places where original name is useful (like aliasing, identifying the corresponding java type,
   * Debug/Error output, etc.
   */
  @Memoized
  public String getQualifiedSourceName() {
    return AstUtils.buildQualifiedName(
        Streams.concat(Stream.of(getPackageName()), getClassComponents().stream()));
  }

  @Nullable
  @Memoized
  public String getKtNativeQualifiedName() {
    KtTypeInfo ktTypeInfo = getKtTypeInfo();
    return ktTypeInfo != null ? ktTypeInfo.getQualifiedName() : null;
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
   * Returns the erasure type (see definition of erasure type at
   * http://help.eclipse.org/luna/index.jsp) with an empty type arguments list.
   */
  @Memoized
  public DeclaredTypeDescriptor toRawTypeDescriptor() {
    return toDescriptor(ImmutableList.of());
  }

  /**
   * Returns the usage site type descriptor corresponding to this declaration with the trivial
   * parameterization.
   */
  @Memoized
  public DeclaredTypeDescriptor toDescriptor() {
    return toDescriptor(getTypeParameterDescriptors());
  }

  /** Returns the usage site type descriptor with parameterization. */
  public DeclaredTypeDescriptor toDescriptor(
      Iterable<? extends TypeDescriptor> typeArgumentDescriptors) {
    return DeclaredTypeDescriptor.newBuilder()
        .setTypeDeclaration(this)
        .setTypeArgumentDescriptors(typeArgumentDescriptors)
        .setNullable(true)
        .build();
  }

  /** A unique string for a given type. Used for interning. */
  @Memoized
  public String getUniqueId() {
    return getQualifiedBinaryName();
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
    return TypeDescriptors.isJavaLangObject(that.toDescriptor())
        || getAllSuperTypesIncludingSelf().contains(that);
  }

  @Memoized
  public Set<TypeDeclaration> getAllSuperTypesIncludingSelf() {
    Set<TypeDeclaration> allSupertypesIncludingSelf = new LinkedHashSet<>();
    allSupertypesIncludingSelf.add(this);
    toDescriptor()
        .getSuperTypesStream()
        .forEach(
            t ->
                allSupertypesIncludingSelf.addAll(
                    t.getTypeDeclaration().getAllSuperTypesIncludingSelf()));
    return allSupertypesIncludingSelf;
  }

  @Memoized
  public Set<TypeDeclaration> getAllSuperInterfaces() {
    return getAllSuperTypesIncludingSelf().stream()
        .filter(Predicate.not(Predicate.isEqual(this)))
        .filter(TypeDeclaration::isInterface)
        .collect(toImmutableSet());
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

  @Memoized
  @Nullable
  public MethodDescriptor getSingleAbstractMethodDescriptor() {
    return getSingleAbstractMethodDescriptorFactory().get(this);
  }

  /**
   * The list of fields declared in the type. Note: this does not include methods synthetic fields
   * (like captures) nor supertype fields.
   */
  @Memoized
  public ImmutableList<FieldDescriptor> getDeclaredFieldDescriptors() {
    return getDeclaredFieldDescriptorsFactory().get(this);
  }

  @Memoized
  ImmutableMap<String, Literal> getOrdinalValueByEnumFieldName() {
    ImmutableMap.Builder<String, Literal> immutableMapBuilder = ImmutableMap.builder();
    int ordinal = 0;
    for (FieldDescriptor fieldDescriptor : getDeclaredFieldDescriptors()) {
      if (!fieldDescriptor.isEnumConstant()) {
        continue;
      }
      immutableMapBuilder.put(fieldDescriptor.getName(), NumberLiteral.fromInt(ordinal++));
    }
    return immutableMapBuilder.buildOrThrow();
  }

  /**
   * The list of fields declared in the type. Note: this does not include methods synthetic fields
   * (like captures) nor supertype fields.
   */
  @Memoized
  public ImmutableList<TypeDeclaration> getMemberTypeDeclarations() {
    return getMemberTypeDeclarationsFactory().get();
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

  @Nullable
  abstract DescriptorFactory<DeclaredTypeDescriptor> getSuperTypeDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<MethodDescriptor>> getDeclaredMethodDescriptorsFactory();

  @Nullable
  abstract DescriptorFactory<MethodDescriptor> getSingleAbstractMethodDescriptorFactory();

  @Nullable
  abstract DescriptorFactory<ImmutableList<FieldDescriptor>> getDeclaredFieldDescriptorsFactory();

  @Nullable
  abstract Supplier<ImmutableList<TypeDeclaration>> getMemberTypeDeclarationsFactory();

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_TypeDeclaration.Builder()
        // Default values.
        .setVisibility(Visibility.PUBLIC)
        .setSourceLanguage(SourceLanguage.JAVA)
        .setOrigin(Origin.SOURCE)
        .setHasAbstractModifier(false)
        .setAnonymous(false)
        .setNative(false)
        .setAnnotation(false)
        .setCapturingEnclosingInstance(false)
        .setFinal(false)
        .setFunctionalInterface(false)
        .setAnnotatedWithFunctionalInterface(false)
        .setAnnotatedWithAutoValue(false)
        .setAnnotatedWithAutoValueBuilder(false)
        .setTestClass(false)
        .setJsFunctionInterface(false)
        .setJsType(false)
        .setLocal(false)
        .setUnusableByJsSuppressed(false)
        .setDeprecated(false)
        .setNullMarked(false)
        .setTypeParameterDescriptors(ImmutableList.of())
        .setDeclaredMethodDescriptorsFactory(() -> ImmutableList.of())
        .setSingleAbstractMethodDescriptorFactory(() -> null)
        .setDeclaredFieldDescriptorsFactory(() -> ImmutableList.of())
        .setMemberTypeDeclarationsFactory(() -> ImmutableList.of())
        .setInterfaceTypeDescriptorsFactory(() -> ImmutableList.of())
        .setEnclosingMethodDescriptorFactory(() -> null)
        .setSuperTypeDescriptorFactory(() -> null);
  }

  // TODO(b/340930928): This is a temporary hack since JsFunction is not supported in Wasm.
  private static final ThreadLocal<Boolean> ignoreJsFunctionAnnotations =
      ThreadLocal.withInitial(() -> false);

  public static void setIgnoreJsFunctionAnnotations() {
    ignoreJsFunctionAnnotations.set(true);
  }

  // TODO(b/181615162): This is a temporary hack which allows Wasm to treat JsEnums differently from
  // Closure.
  // In Wasm:
  // - TODO(b/288145698): Native JsEnums are ignored (the annotation is removed on creation of
  // TypeDeclaration)
  // - The supertype of JsEnums is not modified (it is still Enum, not changed to Object).
  private static final ThreadLocal<Boolean> implementWasmJsEnumSemantics =
      ThreadLocal.withInitial(() -> false);

  public static void setImplementWasmJsEnumSemantics() {
    implementWasmJsEnumSemantics.set(true);
  }

  TypeDeclaration acceptInternal(Processor processor) {
    return Visitor_TypeDeclaration.visit(processor, this);
  }

  /** Builder for a TypeDeclaration. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAnonymous(boolean isAnonymous);

    public abstract Builder setClassComponents(String... classComponents);

    public abstract Builder setClassComponents(List<String> classComponents);

    public abstract Builder setEnclosingTypeDeclaration(TypeDeclaration enclosingTypeDeclaration);

    public abstract Builder setEnclosingMethodDescriptorFactory(
        Supplier<MethodDescriptor> enclosingMethodDescriptorFactory);

    public abstract Builder setOverlaidTypeDeclaration(TypeDeclaration typeDeclaration);

    public abstract Builder setHasAbstractModifier(boolean hasAbstractModifier);

    public abstract Builder setKind(Kind kind);

    public abstract Builder setAnnotation(boolean isAnnotation);

    public abstract Builder setSourceLanguage(SourceLanguage sourceLanguage);

    public abstract Builder setCapturingEnclosingInstance(boolean capturingEnclosingInstance);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setFunctionalInterface(boolean isFunctionalInterface);

    public abstract Builder setAnnotatedWithFunctionalInterface(boolean isAnnotated);

    public abstract Builder setOrigin(Origin origin);

    public abstract Builder setAnnotatedWithAutoValue(boolean annotatedWithAutoValue);

    public abstract Builder setAnnotatedWithAutoValueBuilder(boolean annotatedWithAutoValueBuilder);

    public abstract Builder setTestClass(boolean isTestClass);

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

    public abstract Builder setOriginalSimpleSourceName(String originalSimpleSourceName);

    public abstract Builder setPackage(PackageDeclaration packageDeclaration);

    private String qualifiedSourceName;

    public Builder setQualifiedSourceName(String qualifiedSourceName) {
      this.qualifiedSourceName = qualifiedSourceName;
      return this;
    }

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

    public abstract Builder setDeclaredMethodDescriptorsFactory(
        DescriptorFactory<ImmutableList<MethodDescriptor>> declaredMethodDescriptorsFactory);

    public Builder setDeclaredMethodDescriptorsFactory(
        Supplier<ImmutableList<MethodDescriptor>> declaredMethodDescriptorsFactory) {
      return setDeclaredMethodDescriptorsFactory(
          typeDescriptor -> declaredMethodDescriptorsFactory.get());
    }

    public abstract Builder setSingleAbstractMethodDescriptorFactory(
        DescriptorFactory<MethodDescriptor> singleDeclaredAbstractMethodDescriptorFactory);

    public Builder setSingleAbstractMethodDescriptorFactory(
        Supplier<MethodDescriptor> singleDeclaredAbstractMethodDescriptorFactory) {
      return setSingleAbstractMethodDescriptorFactory(
          typeDescriptor -> singleDeclaredAbstractMethodDescriptorFactory.get());
    }

    public abstract Builder setDeclaredFieldDescriptorsFactory(
        DescriptorFactory<ImmutableList<FieldDescriptor>> declaredFieldDescriptorsFactory);

    public Builder setDeclaredFieldDescriptorsFactory(
        Supplier<ImmutableList<FieldDescriptor>> declaredFieldDescriptorsFactory) {
      return setDeclaredFieldDescriptorsFactory(
          typeDescriptor -> declaredFieldDescriptorsFactory.get());
    }

    public abstract Builder setMemberTypeDeclarationsFactory(
        Supplier<ImmutableList<TypeDeclaration>> memberTypeDeclarationsFactory);

    // Builder accessors to aid construction.
    abstract Optional<ImmutableList<String>> getClassComponents();

    abstract Optional<String> getSimpleJsName();

    abstract Optional<PackageDeclaration> getPackage();

    abstract Optional<TypeDeclaration> getEnclosingTypeDeclaration();

    abstract Optional<JsEnumInfo> getJsEnumInfo();

    abstract boolean isJsFunctionInterface();

    abstract boolean isNative();

    abstract Kind getKind();

    abstract boolean isAnnotation();

    private static final ThreadLocalInterner<TypeDeclaration> interner =
        new ThreadLocalInterner<>();

    abstract TypeDeclaration autoBuild();

    public TypeDeclaration build() {
      if (isJsFunctionInterface() && ignoreJsFunctionAnnotations.get()) {
        setJsFunctionInterface(false);
      }

      // TODO(b/181615162): Find a better way to expose different flavors of type models by backend.
      if (getKind() == Kind.ENUM && isNative() && implementWasmJsEnumSemantics.get()) {
        setJsEnumInfo(null);
        setNative(false);
      }

      if (qualifiedSourceName != null) {
        // Setting qualifiedSourceName is only allowed for top-level types and shouldn't be mixed
        // with other construction styles (like providing packages, class components, etc.).
        checkState(getEnclosingTypeDeclaration().isEmpty());
        checkState(getPackage().isEmpty());
        checkState(getClassComponents().isEmpty());

        int lastDot = qualifiedSourceName.lastIndexOf('.');
        setPackage(
            PackageDeclaration.newBuilder()
                .setName(lastDot == -1 ? "" : qualifiedSourceName.substring(0, lastDot))
                .build());
        setClassComponents(qualifiedSourceName.substring(lastDot + 1));
      }

      if (!getPackage().isPresent()) {
        // If no package is set, enclosing type is mandatory where we can get the package from.
        setPackage(getEnclosingTypeDeclaration().get().getPackage());
      }

      if (!getSimpleJsName().isPresent()) {
        setSimpleJsName(AstUtils.getSimpleSourceName(getClassComponents().get()));
      }

      checkState(!isAnnotation() || getKind() == Kind.INTERFACE);

      TypeDeclaration typeDeclaration = autoBuild();

      // Has to be an interface to be a functional interface.
      checkState(typeDeclaration.isInterface() || !typeDeclaration.isFunctionalInterface());

      checkState(
          typeDeclaration.getTypeParameterDescriptors().stream()
              .noneMatch(TypeVariable::isWildcardOrCapture));

      checkState(
          typeDeclaration.getTypeParameterDescriptors().stream()
              .map(TypeVariable::getNullabilityAnnotation)
              .allMatch(Predicate.isEqual(NullabilityAnnotation.NONE)));

      return interner.intern(typeDeclaration);
    }

    public static Builder from(TypeDeclaration typeDeclaration) {
      return typeDeclaration.toBuilder();
    }
  }
}
