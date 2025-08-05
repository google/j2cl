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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.ThreadLocalInterner;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import com.google.j2cl.transpiler.ast.FieldDescriptor.FieldOrigin;
import com.google.j2cl.transpiler.ast.TypeDeclaration.SourceLanguage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** A reference to a method. */
@Visitable
@AutoValue
public abstract class MethodDescriptor extends MemberDescriptor {
  /** A method parameter descriptor */
  @AutoValue
  public abstract static class ParameterDescriptor implements HasAnnotations {

    public abstract TypeDescriptor getTypeDescriptor();

    public abstract boolean isVarargs();

    @Override
    public abstract ImmutableList<Annotation> getAnnotations();

    public abstract boolean isOptional();

    public abstract boolean isJsOptional();

    @Memoized
    public ParameterDescriptor toRawParameterDescriptor() {
      return toBuilder().setTypeDescriptor(getTypeDescriptor().toRawTypeDescriptor()).build();
    }

    public abstract Builder toBuilder();

    public static Builder newBuilder() {
      return new AutoValue_MethodDescriptor_ParameterDescriptor.Builder()
          .setVarargs(false)
          .setAnnotations(ImmutableList.of())
          .setOptional(false)
          .setJsOptional(false);
    }

    private static final ThreadLocalInterner<ParameterDescriptor> interner =
        new ThreadLocalInterner<>();

    /** A Builder for ParameterDescriptor. */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setTypeDescriptor(TypeDescriptor typeDescriptor);

      public abstract Builder setVarargs(boolean isVarargs);

      abstract boolean isVarargs();

      public abstract Builder setAnnotations(List<Annotation> annotations);

      public abstract Builder setOptional(boolean isOptional);

      abstract boolean isOptional();

      public abstract Builder setJsOptional(boolean isJsOptional);

      abstract boolean isJsOptional();

      abstract ParameterDescriptor autoBuild();

      public ParameterDescriptor build() {
        checkState(!isOptional() || !isVarargs(), "Parameters cannot be both varargs and optional");
        // TODO(b/236400205): we need to think through the implications of something being both
        //  optional and explicitly JsOptional. For now we'll prevent frontends from getting us into
        //  that state.
        checkState(
            !isOptional() || !isJsOptional(), "Parameters cannot be both optional and JsOptional");
        return interner.intern(autoBuild());
      }
    }
  }

  /** Whether the method originated in source code or was synthesized by a pass */
  public enum MethodOrigin implements MemberDescriptor.Origin {
    SOURCE,
    SYNTHETIC_FACTORY_FOR_CONSTRUCTOR("<synthetic: ctor_create>"),
    SYNTHETIC_NOOP_JAVASCRIPT_CONSTRUCTOR("<synthetic: ctor_js>", Visibility.PROTECTED),
    SYNTHETIC_CTOR_FOR_CONSTRUCTOR("<init>"),
    SYNTHETIC_CLASS_INITIALIZER("<clinit>"),
    SYNTHETIC_INSTANCE_INITIALIZER("<init>", Visibility.PRIVATE),
    SYNTHETIC_PROPERTY_SETTER("<synthetic: setter>"),
    SYNTHETIC_PROPERTY_GETTER("<synthetic: getter>"),
    SYNTHETIC_ADAPT_LAMBDA("<synthetic: adapt_lambda>"),
    SYNTHETIC_LAMBDA_ADAPTOR_CONSTRUCTOR("<synthetic: lambda_adaptor_ctor>"),
    SYNTHETIC_CLASS_LITERAL_GETTER,
    SYNTHETIC_STRING_LITERAL_GETTER,
    SYNTHETIC_SYSTEM_PROPERTY_GETTER_OPTIONAL,
    SYNTHETIC_SYSTEM_PROPERTY_GETTER_REQUIRED,
    SYNTHETIC_INSTANCE_OF_SUPPORT_METHOD,
    GENERALIZING_BRIDGE, // Bridges a more general signature to a more specific one.
    SPECIALIZING_BRIDGE, // Bridges a more specific signature to a more general one.
    DEFAULT_METHOD_BRIDGE, // Bridges to a default method interface.
    ABSTRACT_STUB, // Abstract override stub of an abstract super type method.
    ;

    private final String stackTraceFrameName;
    private final Visibility overriddenJsVisibility;

    MethodOrigin() {
      this(null);
    }

    MethodOrigin(String stackTraceFrameName) {
      this(stackTraceFrameName, Visibility.PUBLIC);
    }

    MethodOrigin(String stackTraceFrameName, Visibility overriddenJsVisibility) {
      this.stackTraceFrameName = stackTraceFrameName;
      this.overriddenJsVisibility = overriddenJsVisibility;
    }

    public String getName() {
      return stackTraceFrameName;
    }

    @Override
    public String getPrefix() {
      return switch (this) {
        // User written methods and bridges need to be mangled the same way.
        case SOURCE,
            GENERALIZING_BRIDGE,
            SPECIALIZING_BRIDGE,
            DEFAULT_METHOD_BRIDGE,
            ABSTRACT_STUB ->
            "m_";

        // Getters and setters need to be mangled as fields.
        case SYNTHETIC_SYSTEM_PROPERTY_GETTER_REQUIRED, SYNTHETIC_SYSTEM_PROPERTY_GETTER_OPTIONAL ->
            // Synthetic property getters use the name of the property as the name of the method
            // hence they don't start with "$" and the prefix needs to be added here.
            "$";

        case SYNTHETIC_PROPERTY_SETTER, SYNTHETIC_PROPERTY_GETTER -> FieldOrigin.SOURCE.getPrefix();

        // Don't prefix the rest, they all start with "$"
        default -> "";
      };
    }

    @Override
    public boolean isSyntheticInstanceOfSupportMember() {
      return this == SYNTHETIC_INSTANCE_OF_SUPPORT_METHOD;
    }

    public boolean isOnceMethod() {
      return switch (this) {
        case SYNTHETIC_CLASS_INITIALIZER,
            SYNTHETIC_CLASS_LITERAL_GETTER,
            SYNTHETIC_STRING_LITERAL_GETTER,
            SYNTHETIC_SYSTEM_PROPERTY_GETTER_OPTIONAL,
            SYNTHETIC_SYSTEM_PROPERTY_GETTER_REQUIRED ->
            true;
        default -> false;
      };
    }

    public boolean isSystemGetPropertyGetter() {
      return switch (this) {
        case SYNTHETIC_SYSTEM_PROPERTY_GETTER_OPTIONAL, SYNTHETIC_SYSTEM_PROPERTY_GETTER_REQUIRED ->
            true;
        default -> false;
      };
    }

    public boolean isRequiredSystemGetPropertyGetter() {
      return this == SYNTHETIC_SYSTEM_PROPERTY_GETTER_REQUIRED;
    }
  }

  // TODO(b/317164851): Remove hack that makes jsinfo ignored for non-native types in Wasm.
  private static final ThreadLocal<Boolean> ignoreNonNativeJsInfo =
      ThreadLocal.withInitial(() -> false);

  public static void setIgnoreNonNativeJsInfo() {
    ignoreNonNativeJsInfo.set(true);
  }

  public static final String CONSTRUCTOR_METHOD_NAME = "<init>";
  public static final String CTOR_METHOD_PREFIX = "$ctor";
  public static final String CREATE_METHOD_NAME = "$create";
  public static final String VALUE_OF_METHOD_NAME = "valueOf"; // Boxed type valueOf() method.

  static final String INIT_METHOD_NAME = "$init";
  static final String CLINIT_METHOD_NAME = "$clinit";
  static final String IS_INSTANCE_METHOD_NAME = "$isInstance";
  static final String MARK_IMPLEMENTOR_METHOD_NAME = "$markImplementor";
  static final String LOAD_MODULES_METHOD_NAME = "$loadModules";
  static final String COPY_METHOD_NAME = "$copy";

  public static String buildMethodSignature(
      String name, TypeDescriptor... parameterTypeDescriptors) {
    return buildMethodSignature(name, Arrays.asList(parameterTypeDescriptors), false);
  }

  private static String buildMethodSignature(
      String name, List<TypeDescriptor> parameterTypeDescriptors, boolean isSuspendFunction) {
    String signature =
        name
            + parameterTypeDescriptors.stream()
                .map(MethodDescriptor::getSignatureStringForParameter)
                .collect(joining(",", "(", ")"));
    if (isSuspendFunction) {
      // Suspend functions are considered to have separate namespace so a suffix is added to
      // differentiate them from regular functions
      signature += "#suspend";
    }
    return signature;
  }

  private static String getSignatureStringForParameter(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor descriptor) {
      return descriptor.getQualifiedBinaryName();
    }
    if (typeDescriptor instanceof PrimitiveTypeDescriptor descriptor) {
      return descriptor.getSimpleSourceName();
    }
    if (typeDescriptor instanceof ArrayTypeDescriptor descriptor) {
      return getSignatureStringForParameter(descriptor.getComponentTypeDescriptor()) + "[]";
    }
    return getSignatureStringForParameter(typeDescriptor.toRawTypeDescriptor());
  }

  public abstract boolean isAbstract();

  public abstract boolean isSynchronized();

  @Override
  public abstract boolean isConstructor();

  public boolean isVarargs() {
    return getParameterDescriptors().stream().anyMatch(ParameterDescriptor::isVarargs);
  }

  @Override
  public abstract boolean isDefaultMethod();

  @Override
  public boolean isLocalFunction() {
    return getEnclosingMethodDescriptor() != null;
  }

  /** Return true if the underlying method represent a Kotlin suspend function. */
  public abstract boolean isSuspendFunction();

  public boolean isBridge() {
    return getBridgeOrigin() != null;
  }

  /**
   * A generalizing bridge is a bridge from a supertype method that is overridden in a subtype and
   * the parameters of the implementation have been specialized.
   *
   * <p>An example of this situation is:
   *
   * <pre>{@code
   * class AbstractList<T> {
   *   void add(T t) {}
   * }
   *
   * class StringList extends AbstractList<String> {
   *  // add(String) here specializes AbstractList.add(Object), needing a bridge from add(Object) to
   *  // add(String). Such a bridge is a generalizing bridge.
   *   void add(String s) {}
   * }
   * }</pre>
   */
  public boolean isGeneralizingBridge() {
    return getOrigin() == MethodOrigin.GENERALIZING_BRIDGE;
  }

  /**
   * A specializing bridge is a bridge from a supertype method that is overridden at a subtype and
   * the parameters of the implementation are more general than those of the method that overrides
   * it.
   *
   * <p>An example of this situation is:
   *
   * <pre>{@code
   * class AbstractList<T> {
   *   void add(T t) {}
   * }
   *
   * interface StringList {
   *   void add(String s) {}
   * }
   *
   * class StringListImpl extends AbstractStringList<String> implements StringList {
   *  // Since StringListImpl does not override AbstractStringList.add(Object) with an
   *  // implementation, there is an implicit override of StringList.add(String) by
   *  // AbstractStringList.add(Object) at this type.
   *  // Since the actual implementation, i.e. AbstractStringList.add(Object) has a more general
   *  // than required by the override in StringList, a specializing bridge from add(String) to
   *  // add(Object) will be created in MyStringImpl.
   * }
   * }</pre>
   */
  public boolean isSpecializingBridge() {
    return getOrigin() == MethodOrigin.SPECIALIZING_BRIDGE;
  }

  /** Returns true if the method descriptor is an abstract stub. */
  public boolean isAbstractStub() {
    return getOrigin() == MethodOrigin.ABSTRACT_STUB;
  }

  public boolean isDefaultMethodBridge() {
    return getOrigin() == MethodOrigin.DEFAULT_METHOD_BRIDGE;
  }

  /** Returns {@code true} if the method is annotated with {@code HasNoSideEffect}. */
  @Memoized
  public boolean isSideEffectFree() {
    return hasAnnotation("javaemul.internal.annotations.HasNoSideEffects");
  }

  /** Returns true if the bridge was build with {@code candidateTarget} as its target. */
  boolean isBridgeTarget(MethodDescriptor candidateTarget) {
    MethodDescriptor method = getBridgeTarget();
    return method != null
        && method.getDeclarationDescriptor().equals(candidateTarget.getDeclarationDescriptor());
  }

  /** Returns the enclosing method descriptor if the descriptor represents a local function. */
  @Nullable
  public abstract MethodDescriptor getEnclosingMethodDescriptor();

  public abstract ImmutableList<ParameterDescriptor> getParameterDescriptors();

  public abstract TypeDescriptor getReturnTypeDescriptor();

  public abstract ImmutableList<TypeDescriptor> getExceptionTypeDescriptors();

  /** Type parameters declared in the method. */
  public abstract ImmutableList<TypeVariable> getTypeParameterTypeDescriptors();

  public abstract ImmutableList<TypeDescriptor> getTypeArgumentTypeDescriptors();

  @Memoized
  public ImmutableList<TypeDescriptor> getParameterTypeDescriptors() {
    return getParameterDescriptors().stream()
        .map(ParameterDescriptor::getTypeDescriptor)
        .collect(toImmutableList());
  }

  @Override
  public abstract MethodOrigin getOrigin();

  /**
   * The source method that causes a bridge to be created.
   *
   * <p>This could be a default method declaration on an interface, or a method in a supertype that
   * will be specialized.
   */
  @Nullable
  public abstract MethodDescriptor getBridgeOrigin();

  /**
   * The actual source method that will be overriding this bridge.
   *
   * <p>This is the method that bridge will be forwarding to.
   */
  @Nullable
  public abstract MethodDescriptor getBridgeTarget();

  /**
   * Returns the descriptor of the method declaration. A method descriptor might describe a
   * specialized version of a method, e.g.
   *
   * <p>
   *
   * <pre>{@code
   *   class A<T> {
   *     void m(T t);  // Method declaration described as m(T).
   *   }
   *
   *   // Method call with a method descriptor for m(String) that has the method descriptor
   *   // for m(T) as its declaration.
   *   new A<String>().m("Hi");
   * <p>
   * }</pre>
   */
  @Override
  public MethodDescriptor getDeclarationDescriptor() {
    return getDeclarationDescriptorOrNullIfSelf() == null
        ? this
        : getDeclarationDescriptorOrNullIfSelf();
  }

  @Nullable
  // A method declaration can be itself but AutoValue does not allow for a property to be a
  // reference to the value object being created, so we use a backing nullable property where null
  // encodes a self reference for AutoValue purposes and provide the accessor above to hide
  // the details.
  abstract MethodDescriptor getDeclarationDescriptorOrNullIfSelf();

  @Memoized
  public MethodDescriptor toRawMemberDescriptor() {
    return toBuilder()
        .setEnclosingTypeDescriptor(getEnclosingTypeDescriptor().toRawTypeDescriptor())
        .setTypeParameterTypeDescriptors(ImmutableList.of())
        .setTypeArgumentTypeDescriptors(ImmutableList.of())
        .setReturnTypeDescriptor(getReturnTypeDescriptor().toRawTypeDescriptor())
        .setParameterDescriptors(
            getParameterDescriptors().stream()
                .map(ParameterDescriptor::toRawParameterDescriptor)
                .collect(toImmutableList()))
        .setDeclarationDescriptor(getDeclarationDescriptor())
        .build();
  }

  /** Removes the type parameters declared in the method. */
  @Memoized
  public MethodDescriptor withoutTypeParameters() {

    Set<TypeVariable> typeParameters = new HashSet<>(getTypeParameterTypeDescriptors());
    return Builder.from(
            specializeTypeVariables(
                p ->
                    typeParameters.contains(p)
                        ? TypeVariable.createWildcardWithUpperBound(p.getUpperBoundTypeDescriptor())
                        : p))
        .setDeclarationDescriptor(
            isDeclaration() ? null : getDeclarationDescriptor().withoutTypeParameters())
        .setTypeParameterTypeDescriptors(ImmutableList.of())
        .setTypeArgumentTypeDescriptors(ImmutableList.of())
        .build();
  }

  @Override
  public boolean isJsProperty() {
    return isJsPropertySetter() || isJsPropertyGetter();
  }

  /**
   * Returns {@code true} if the method has a JsFunction calling contract in JavaScript.
   *
   * <p>Note that the Java overriding method in the JsFunction implementation class may or may not
   * be a JsFunction. It is only a JsFunction if it wouldn't need a bridge.
   */
  @Override
  @Memoized
  public boolean isJsFunction() {
    if (!isDeclaration()) {
      return getDeclarationDescriptor().isJsFunction();
    }

    DeclaredTypeDescriptor enclosingType = getEnclosingTypeDescriptor();
    if (enclosingType.isJsFunctionInterface()) {
      MethodDescriptor singleAbstractMethod = enclosingType.getSingleAbstractMethodDescriptor();
      return singleAbstractMethod != null
          && this == singleAbstractMethod.getDeclarationDescriptor();
    }

    return enclosingType.isJsFunctionImplementation()
        && getJsOverriddenMethodDescriptors().stream().anyMatch(MethodDescriptor::isJsFunction);
  }

  @Memoized
  public boolean isOrOverridesJsFunction() {
    return isJsFunction()
        || getJavaOverriddenMethodDescriptors().stream().anyMatch(MethodDescriptor::isJsFunction);
  }

  /**
   * Returns true if it is a vararg method that can be referenced by JavaScript side. A
   * non-JsOverlay JsMethod, and a JsFunction can be referenced by JavaScript side.
   *
   * <p>TODO(rluble): In our AST model, isJsMethod() and isJsOverlay() is NOT mutually-exclusive. We
   * may want to re-examine it after we import JsInteropRestrictionChecker and do refactoring on the
   * AST.
   */
  public boolean isJsMethodVarargs() {
    return isVarargs() && ((isJsMethod() && !isJsOverlay()) || isJsFunction() || isJsConstructor());
  }

  @Override
  public boolean isInstanceMember() {
    return !isStatic() && !isConstructor() && !isLocalFunction();
  }

  /** Whether the method does dynamic dispatch and can be overridden. */
  public boolean isPolymorphic() {
    return isInstanceMember() && !getVisibility().isPrivate();
  }

  @Override
  public boolean isMethod() {
    return true;
  }

  @Nullable
  abstract KtObjcInfo getKtObjcInfo();

  /** Compute the KtInfo of the function by traversing its overriding chain. */
  @Override
  @Memoized
  public KtInfo getKtInfo() {
    if (getManglingDescriptor() != this) {
      return getManglingDescriptor().getKtInfo();
    }

    KtInfo ktInfo = getDeclarationDescriptor().getOriginalKtInfo();

    for (MethodDescriptor overriddenMethodDescriptor : getJavaOverriddenMethodDescriptors()) {
      KtInfo overriddenKtInfo =
          overriddenMethodDescriptor.getDeclarationDescriptor().getOriginalKtInfo();
      ktInfo =
          KtInfo.newBuilder()
              .setProperty(ktInfo.isProperty() || overriddenKtInfo.isProperty())
              .setName(ktInfo.getName() == null ? overriddenKtInfo.getName() : ktInfo.getName())
              .build();
    }

    return ktInfo;
  }

  /** Compute the JsInfo of the function by traversing its overriding chain. */
  @Override
  @Memoized
  public JsInfo getJsInfo() {
    if (getManglingDescriptor() != this) {
      return getManglingDescriptor().getJsInfo();
    }

    checkState(isDeclaration());
    JsInfo originalJsInfo = getOriginalJsInfo();

    // Make the implicit constructor of an anonymous class extending a JsType with JsConstructor
    // automatically a JsConstructor.
    if (getEnclosingTypeDescriptor().getTypeDeclaration().isAnonymous()
        && isConstructor()
        && getEnclosingTypeDescriptor().getSuperTypeDescriptor().hasJsConstructor()) {
      return JsInfo.Builder.from(originalJsInfo).setJsMemberType(JsMemberType.CONSTRUCTOR).build();
    }

    if (originalJsInfo.isJsOverlay()
        || originalJsInfo.getJsName() != null
        || originalJsInfo.getJsNamespace() != null) {
      // Do not examine overridden methods if the method is marked as JsOverlay or it has a JsMember
      // annotation that customizes the name.
      return originalJsInfo;
    }

    boolean hasExplicitJsMemberAnnotation = originalJsInfo.getHasJsMemberAnnotation();
    JsInfo defaultJsInfo = originalJsInfo;

    for (MethodDescriptor overriddenMethodDescriptor : getJavaOverriddenMethodDescriptors()) {
      if (!isNative() && !canInheritsJsInfoFrom(overriddenMethodDescriptor)) {
        // Only propagate the JsInfo from methods with the same signature.
        continue;
      }

      JsInfo inheritedJsInfo = overriddenMethodDescriptor.getOriginalJsInfo();

      if (inheritedJsInfo.getJsMemberType() == JsMemberType.NONE) {
        continue;
      }

      if (hasExplicitJsMemberAnnotation
          && originalJsInfo.getJsMemberType() != inheritedJsInfo.getJsMemberType()) {
        // If they are inconsistent preserve the explicit annotation on the member so that the
        // restriction checker can report the error.
        continue;
      }

      if (inheritedJsInfo.getJsName() != null) {
        // Found an overridden method of the same JsMember type one that customizes the name, done.
        // If there are any conflicts with other overrides they will be reported by
        // JsInteropRestrictionsChecker.
        return JsInfo.Builder.from(inheritedJsInfo).setJsAsync(originalJsInfo.isJsAsync()).build();
      }

      if (defaultJsInfo == originalJsInfo && !hasExplicitJsMemberAnnotation) {
        // The original method does not have a JsMember annotation and traversing the list of
        // overridden methods we found the first that has an explicit JsMember annotation.
        // Keep it as the one to be used if none is found that customizes the name.
        // This allows to "inherit" the JsMember type from the override.
        defaultJsInfo = inheritedJsInfo;
      }
    }

    if (!isNative()
        && getJavaOverriddenMethodDescriptors().stream()
            .filter(MethodDescriptor::isJsMember)
            .anyMatch(Predicates.not(this::canInheritsJsInfoFrom))) {
      // This is a Java override of a method that does not have the same signature and its
      // JsMethod annotation does not specify a name; in this case the method will not be considered
      // a JsMethod but instead it will be the target of a JsMethod bridge.
      return JsInfo.Builder.from(JsInfo.NONE).setJsAsync(originalJsInfo.isJsAsync()).build();
    }

    // Don't inherit @JsAsync annotation from overridden methods.
    return JsInfo.Builder.from(defaultJsInfo)
        .setJsAsync(originalJsInfo.isJsAsync())
        .setHasJsMemberAnnotation(hasExplicitJsMemberAnnotation)
        .build();
  }

  /** Returns true if the method inherits the JsInfo from {@code methodDescriptor}. */
  public boolean canInheritsJsInfoFrom(MethodDescriptor methodDescriptor) {
    return getJsMemberCompatibilitySignature()
        .equals(methodDescriptor.getJsMemberCompatibilitySignature());
  }

  /**
   * Returns a signature used to decided whether two JsMethods can or not share the same property
   * name.
   *
   * <p>The Java signature, returned by getSignature(), that is used to compute overrides only
   * considers parameters but not return types. Since specializing the return types does not affect
   * the contract. In Kotlin, on the other hand, you can specialize the return to a primitive type
   * which can not be used if the contract assumed a reference type as a return. So two methods that
   * only differ in the return type, with one returning a primitive and the other a reference type,
   * cannot both use the same name.
   */
  @Memoized
  String getJsMemberCompatibilitySignature() {
    if (getManglingDescriptor() != this) {
      return getManglingDescriptor().getJsMemberCompatibilitySignature();
    }
    String returnType =
        getReturnTypeDescriptor().isPrimitive()
                || AstUtils.isNonNativeJsEnum(getReturnTypeDescriptor())
            ? getSignatureStringForParameter(getReturnTypeDescriptor())
            : "Reference";
    return getSignature() + ":" + returnType;
  }

  public boolean isPropertyGetter() {
    return isJsPropertyGetter() || getOrigin() == MethodOrigin.SYNTHETIC_PROPERTY_GETTER;
  }

  public boolean isPropertySetter() {
    return isJsPropertySetter() || getOrigin() == MethodOrigin.SYNTHETIC_PROPERTY_SETTER;
  }

  public abstract boolean isEnumSyntheticMethod();

  @Override
  public boolean isCustomIsInstanceMethod() {
    return getOrigin() == MethodOrigin.SOURCE
        && getParameterDescriptors().size() == 1
        && getName().equals(IS_INSTANCE_METHOD_NAME);
  }

  @Nullable
  public String getObjectiveCName() {
    KtObjcInfo ktObjcInfo = getKtObjcInfo();
    return ktObjcInfo != null ? ktObjcInfo.getObjectiveCName() : null;
  }

  /** Returns true if this descriptor and {@code other} refer to the same method declaration. */
  // TODO(b/372055363): This should be equivalent to
  //  `getDeclarationDescriptor().equals(other.getDeclarationDescriptor())`
  public boolean isSameMethod(MethodDescriptor other) {
    return getEnclosingTypeDescriptor().isSameBaseType(other.getEnclosingTypeDescriptor())
        && getDeclarationDescriptor()
            .getSignature()
            .equals(other.getDeclarationDescriptor().getSignature());
  }

  @Override
  @Memoized
  public boolean isOrOverridesJavaLangObjectMethod() {
    if (!isPolymorphic()) {
      return false;
    }

    if (isJavaLangObject(getEnclosingTypeDescriptor())) {
      return true;
    }

    return TypeDescriptors.get().javaLangObject.getDeclaredMethodDescriptors().stream()
        .anyMatch(this::isOverride);
  }

  /** Returns {@code true} if this method overrides a JsMethod. */
  @Memoized
  public boolean isOrOverridesJsMethod() {
    return isJsMethod()
        || getJavaOverriddenMethodDescriptors().stream().anyMatch(MethodDescriptor::isJsMethod);
  }

  @Override
  @Memoized
  public String getBinaryName() {
    return getOrigin().getName() == null ? getName() : getOrigin().getName();
  }

  @Memoized
  @Override
  public String getMangledName() {
    if (getManglingDescriptor() != this) {
      return getManglingDescriptor().getMangledName();
    }

    // Do not use JsInfo when producing mangled names for wasm.
    if (useClosureManglingPatterns()) {
      if (isJsConstructor() || getOrigin() == MethodOrigin.SYNTHETIC_NOOP_JAVASCRIPT_CONSTRUCTOR) {
        return "constructor";
      }

      if (isPropertyGetter()) {
        return "get " + computePropertyMangledName();
      }

      if (isPropertySetter()) {
        return "set " + computePropertyMangledName();
      }

      if (isJsMethod()) {
        return getSimpleJsName();
      }

      if (getOrigin().isSyntheticInstanceOfSupportMember() || isCustomIsInstanceMethod()) {
        // Class support methods, like $isInstance and $markImplementor, should not be mangled.
        return getName();
      }
    }

    // All special cases have been handled. Go ahead and construct the mangled name for a plain
    // Java method.
    String suffix = "";
    if (isInstanceMember()) {
      // Only use suffixes for instance methods. Static methods are always called through the
      // right constructor, no need to add a suffix to avoid collisions.
      suffix =
          switch (getVisibility()) {
            case PRIVATE ->
                // To ensure that private methods never override each other.
                "_$p_" + getEnclosingTypeDescriptor().getMangledName();
            case PACKAGE_PRIVATE ->
                // To ensure that package private methods only override one another when
                // they are in the same package.
                "_$pp_"
                    + getEnclosingTypeDescriptor()
                        .getTypeDeclaration()
                        .getPackageName()
                        .replace('.', '_');
            default -> "";
          };
    }

    if (isSuspendFunction()) {
      // Suspend functions are considered to have separate namespace so a suffix is added to
      // differentiate them from regular functions. Since they get an implicit Continuation
      // parameter which becomes part of our signature, the suffix avoids conflicts with a regular
      // functions that explicitly declare a Continuation typed parameter.
      suffix += "_$s_";
    }

    Stream<TypeDescriptor> signatureDescriptors = getParameterTypeDescriptors().stream();
    if (!isConstructor() && getOrigin() != MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR) {
      // Constructors and constructor related factories always return the enclosing class type and
      // there is no need to make the mangled name longer unnecessarily.
      // TODO(b/241302441): Consider mangling all the methods the same way without exceptions.
      signatureDescriptors =
          Stream.concat(signatureDescriptors, Stream.of(getReturnTypeDescriptor()));
    }

    String signature =
        signatureDescriptors
            .map(TypeDescriptor::toRawTypeDescriptor)
            .map(TypeDescriptor::getMangledName)
            .collect(joining("__"));

    return buildMangledName(signature + suffix);
  }

  /**
   * Returns the type descriptor that is needed to determine the actual types for the method
   * parameters used for determining the method mangled name and the types that can actually flow to
   * the method.
   */
  @Memoized
  MethodDescriptor getManglingDescriptor() {
    if (!isDeclaration()) {
      return getDeclarationDescriptor().getManglingDescriptor();
    }
    if (isGeneralizingBridge() || isAbstractStub()) {
      // Generalizing bridges are methods that fill the gap between the overridden parent method and
      // a specialized override. Abstract stubs override a parent method that has a given mangled
      // name. In both cases, the parameter/return types for these methods do not determine the
      // actual method name, since the mangling needs to match the method that they are actually
      // overriding.
      return getBridgeOrigin().getManglingDescriptor();
    }
    return this;
  }

  @Override
  String getManglingPrefix() {
    String name = getName();
    checkState(!getOrigin().getPrefix().isEmpty() || name.startsWith("$"));

    // Do not add a prefix to user written methods whose name start with '$'. Some of those internal
    // members like $create, $isInstance, etc could also be provided by the user to customize the
    // behaviour.
    boolean isInternal = name.startsWith("$") && getOrigin() == MethodOrigin.SOURCE;
    return isInternal ? "" : getOrigin().getPrefix();
  }

  /**
   * Returns the type of the objects that can arrive at runtime as parameters.
   *
   * <p>For bridge methods those will be the parameter types of the method they are bridging from.
   * The bridge method is responsible for casting these parameters to the appropriate type before
   * forwarding to the actual implementation.
   */
  public List<TypeDescriptor> getDispatchParameterTypeDescriptors() {
    return getManglingDescriptor().getParameterTypeDescriptors();
  }

  public TypeDescriptor getDispatchReturnTypeDescriptor() {
    return getManglingDescriptor().getReturnTypeDescriptor();
  }

  /**
   * Returns whether this method descriptor overrides the provided method descriptor from the
   * Java/Kotlin source perspective.
   *
   * <p>This includes both real and accidental overrides.
   */
  public boolean isOverride(MethodDescriptor that) {
    // A method can not override itself.
    if (this == that) {
      return false;
    }
    // Static methods do not participate in override chains.
    if (this.isStatic() || that.isStatic()) {
      return false;
    }
    Visibility thisVisibility = this.getVisibility();
    Visibility thatVisibility = that.getVisibility();
    // Private methods can not override nor can they be overridden.
    if (thisVisibility.isPrivate() || thatVisibility.isPrivate()) {
      return false;
    }
    // To override a package private method one must reside in the same package.
    if (thatVisibility.isPackagePrivate()
        && !getEnclosingTypeDescriptor().isInSamePackage(that.getEnclosingTypeDescriptor())) {
      return false;
    }

    return isSameSignature(that);
  }

  /** Returns {@code true} is {@code this} has the same signature as {@code that}. */
  public boolean isSameSignature(MethodDescriptor that) {
    return getSignature().equals(that.getSignature());
  }

  public Visibility getJsVisibility() {
    // TODO(b/29632512): stop overriding visibility and reflect the java visibilities.
    return getOrigin().overriddenJsVisibility;
  }

  /** Returns a signature suitable for override checking from the Java source perspective. */
  @Memoized
  public String getSignature() {
    return getSignature(getEnclosingTypeDescriptor().getTypeDeclaration().getSourceLanguage());
  }

  public String getSignature(SourceLanguage sourceLanguage) {
    boolean isKotlinType = sourceLanguage == SourceLanguage.KOTLIN;
    ImmutableList<TypeDescriptor> parameterTypeDescriptors =
        isKotlinType
            // In Kotlin types, non-nullable Int has the signature of primitive int (same
            // for all non-nullable "primitive" types).
            ? convertNonNullableBoxedTypesToPrimitives(getParameterTypeDescriptors())
            : getParameterTypeDescriptors();
    return buildMethodSignature(
        getNameApplyingKotlinRenames(), parameterTypeDescriptors, isSuspendFunction());
  }

  /** Returns the method name but accounts for kotlin's rename of {@code T List.remove(int)}. */
  // TODO(b/372484266): Do this in a more principled manner.
  private String getNameApplyingKotlinRenames() {
    if (getName().equals("remove")
        && TypeDescriptors.isPrimitiveBoolean(getReturnTypeDescriptor())
        && getParameterDescriptors().size() == 1
        && TypeDescriptors.isPrimitiveInt(
            Iterables.getOnlyElement(getParameterTypeDescriptors()))) {
      // Kotlin renames the method `T List<T>.remove(int)` to `removeAt` so that it does not
      // clash with `boolean List<T>.remove(T)` when specializing to `List<Int>`. Internally we need
      // to preserve the original names to interoperate with Java code, so the new name only applies
      // when reasoning about overrides. For the Java frontend it is safe to always use an arbitrary
      // and unique override key for `boolean remove(int)`, since methods that only involve
      // primitives in their parameter and return types can never specialize them. In Kotlin this
      // change resolves the source renaming by making the signatures of `int remove(int)` and
      // `boolean remove(int)` different.
      // Note that in Java the remove API is `boolean List<Int>.remove(Object)` but kotlin creates a
      // special override `boolean List<Int>.remove(Int)` which is the one we need to have a
      // special case for here.
      return "remove#specialized";
    }
    return getName();
  }

  private ImmutableList<TypeDescriptor> convertNonNullableBoxedTypesToPrimitives(
      ImmutableList<TypeDescriptor> parameterTypeDescriptors) {
    return parameterTypeDescriptors.stream()
        .map(
            typeDescriptor ->
                TypeDescriptors.isBoxedType(typeDescriptor.toRawTypeDescriptor())
                        && !typeDescriptor.isNullable()
                    ? typeDescriptor.toRawTypeDescriptor().toUnboxedType()
                    : typeDescriptor)
        .collect(toImmutableList());
  }

  /**
   * The override key defines a grouping of method descriptors at a type that will be handled by the
   * same actual implementation.
   *
   * <p>The difference between an override signature and an override key is that the override key
   * will be different for package private methods in different packages even if they have the same
   * signature.
   */
  String getOverrideKey(SourceLanguage sourceLanguage) {
    checkState(isPolymorphic());
    return getVisibility().isPackagePrivate()
        ? getPackagePrivateOverrideKey(sourceLanguage)
        : getSignature(sourceLanguage);
  }

  // This is a somewhat hacky way to differentiate package private override keys. A more principled
  // approach is to introduce a value type with two fields, so that we can use it as a key in
  // the maps used for the bridge method computations.
  private static final String PACKAGE_PRIVATE_MARKER = "{pp}-";

  String getPackagePrivateOverrideKey(SourceLanguage sourceLanguage) {
    return getSignature(sourceLanguage)
        + PACKAGE_PRIVATE_MARKER
        + getEnclosingTypeDescriptor().getTypeDeclaration().getPackageName();
  }

  public MethodDescriptor transform(Consumer<? super Builder> transformer) {
    Builder builder = toBuilder();
    if (!isDeclaration()) {
      builder.setDeclarationDescriptor(getDeclarationDescriptor().transform(transformer));
    }
    transformer.accept(builder);
    return builder.build();
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_MethodDescriptor.Builder()
        // Default values.
        .setVisibility(Visibility.PUBLIC)
        .setOriginalJsInfo(JsInfo.NONE)
        .setOriginalKtInfo(KtInfo.NONE)
        .setAnnotations(ImmutableList.of())
        .setAbstract(false)
        .setSynchronized(false)
        .setConstructor(false)
        .setDefaultMethod(false)
        .setNative(false)
        .setStatic(false)
        .setFinal(false)
        .setSynthetic(false)
        .setSuspendFunction(false)
        .setEnumSyntheticMethod(false)
        .setOrigin(MethodOrigin.SOURCE)
        .setParameterDescriptors(ImmutableList.of())
        .setReturnTypeDescriptor(PrimitiveTypes.VOID)
        .setExceptionTypeDescriptors(ImmutableList.of())
        .setTypeParameterTypeDescriptors(ImmutableList.of())
        .setTypeArgumentTypeDescriptors(ImmutableList.of());
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    String parameterString =
        getParameterDescriptors().stream()
            .map(MethodDescriptor::getParameterReadableDescription)
            .collect(joining(", "));

    TypeDeclaration enclosingTypeDeclaration = getEnclosingTypeDescriptor().getTypeDeclaration();
    if (isConstructor()) {
      return String.format(
          "%s(%s)", enclosingTypeDeclaration.getReadableDescription(), parameterString);
    }
    return String.format(
        "%s %s.%s(%s)",
        getReturnTypeDescriptor().getReadableDescription(),
        enclosingTypeDeclaration.getReadableDescription(),
        getName(),
        parameterString);
  }

  private static String getParameterReadableDescription(ParameterDescriptor parameterDescriptor) {
    TypeDescriptor parameterTypeDescriptor =
        parameterDescriptor.getTypeDescriptor().toRawTypeDescriptor();
    if (parameterDescriptor.isVarargs()) {
      ArrayTypeDescriptor parameterArrayTypeDescriptor =
          (ArrayTypeDescriptor) parameterTypeDescriptor;
      return String.format(
          "%s...",
          parameterArrayTypeDescriptor.getComponentTypeDescriptor().getReadableDescription());
    } else if (parameterDescriptor.isJsOptional()) {
      return String.format("@JsOptional %s", parameterTypeDescriptor.getReadableDescription());
    }
    return parameterTypeDescriptor.getReadableDescription();
  }

  /**
   * Returns a set of the method descriptors that are overridden by {@code methodDescriptor} from
   * the Java semantics perspective.
   */
  @Memoized
  public ImmutableSet<MethodDescriptor> getJavaOverriddenMethodDescriptors() {
    if (!isPolymorphic()) {
      return ImmutableSet.of();
    }

    return getEnclosingTypeDescriptor().getAllSuperTypesIncludingSelf().stream()
        .filter(t -> t != getEnclosingTypeDescriptor())
        .flatMap(t -> t.getDeclaredMethodDescriptors().stream())
        .filter(MethodDescriptor::isPolymorphic)
        .filter(this::isOverride)
        .collect(toImmutableSet());
  }

  /**
   * Returns whether this method descriptor overrides other method descriptor from the Java semantic
   * perspective.
   */
  public boolean isJavaOverride() {
    return !getJavaOverriddenMethodDescriptors().isEmpty();
  }

  /**
   * Returns a set of the method descriptors that are overridden by {@code methodDescriptor} from
   * the JavaScript perspective.
   */
  @Memoized
  public ImmutableSet<MethodDescriptor> getJsOverriddenMethodDescriptors() {
    if (!isPolymorphic()) {
      return ImmutableSet.of();
    }

    var overriddenMethodsBuilder = ImmutableSet.<MethodDescriptor>builder();

    getEnclosingTypeDescriptor()
        .getSuperTypesStream()
        // Remove java.lang.Object as a supertype if the type is an interface since in JavaScript
        // interface methods never override class methods.
        .filter(t -> !(getEnclosingTypeDescriptor().isInterface() && isJavaLangObject(t)))
        .flatMap(t -> t.getPolymorphicMethods().stream())
        .filter(m -> m.getMangledName().equals(getMangledName()))
        .forEach(m -> overriddenMethodsBuilder.add(m).addAll(m.getJsOverriddenMethodDescriptors()));

    return overriddenMethodsBuilder.build();
  }

  public boolean isJsOverride() {
    return getJsOverriddenMethodDescriptors().stream().anyMatch(MethodDescriptor::isJsOverrideable);
  }

  /**
   * Whether it is valid to emit a JsDoc @override annotations for methods that override methods in
   * this type.
   */
  private boolean isJsOverrideable() {
    return !isJsOverlay()
        && !getEnclosingTypeDescriptor().isStarOrUnknown()
        && !getEnclosingTypeDescriptor().isJsFunctionInterface();
  }

  @Override
  public MethodDescriptor specializeTypeVariables(
      Map<TypeVariable, TypeDescriptor> applySpecializedTypeArgumentByTypeParameters) {
    return specializeTypeVariables(
        TypeDescriptors.mappingFunctionFromMap(applySpecializedTypeArgumentByTypeParameters));
  }

  @Override
  public MethodDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacingTypeDescriptorByTypeVariable) {
    if (AstUtils.isIdentityFunction(replacingTypeDescriptorByTypeVariable)) {
      return this;
    }
    // First specialize the type parameters/type arguments since that might introduce new captures
    // that need to be propagated. e.g.
    // class A<T> {
    //   <S extends List<T>, U extends S> void m(X<U>) {...}
    // }
    //
    // class B extends A<String> {
    //    // <S1 extends List<String>, U1 extends S1> void m(X<U1>)  should be the bridge here.
    // }
    //

    // Specializing a declaration means specializing the type parameters; whereas specializing a
    // usage site, means specializing the type variables that might appear in the current
    // type arguments.
    ImmutableList<? extends TypeDescriptor> typeArgumentTypeDescriptors =
        isDeclaration() ? getTypeParameterTypeDescriptors() : getTypeArgumentTypeDescriptors();

    // Compute the new specialization mapping by creating replacement type variables whenever
    // bounds need to be specialized.
    var updatedReplacingTypeDescriptorByTypeVariable =
        propagateChangesInBounds(
            typeArgumentTypeDescriptors, replacingTypeDescriptorByTypeVariable);

    // Specialize the type arguments using the updated mapping.
    ImmutableList<TypeDescriptor> specializedTypeArgumentDescriptors =
        typeArgumentTypeDescriptors.stream()
            .map(
                typeDescriptor ->
                    typeDescriptor.specializeTypeVariables(
                        updatedReplacingTypeDescriptorByTypeVariable))
            .collect(toImmutableList());

    // Specialize the return type using the updated mapping.
    TypeDescriptor returnTypeDescriptor = getReturnTypeDescriptor();
    TypeDescriptor specializedReturnTypeDescriptor =
        returnTypeDescriptor.specializeTypeVariables(updatedReplacingTypeDescriptorByTypeVariable);

    // Specialize the parameters type using the updated mapping.
    ImmutableList<TypeDescriptor> parameterTypeDescriptors = getParameterTypeDescriptors();
    ImmutableList<TypeDescriptor> specializedParameterTypeDescriptors =
        parameterTypeDescriptors.stream()
            .map(
                typeDescriptor ->
                    typeDescriptor.specializeTypeVariables(
                        updatedReplacingTypeDescriptorByTypeVariable))
            .collect(toImmutableList());

    DeclaredTypeDescriptor enclosingTypeDescriptor = getEnclosingTypeDescriptor();
    DeclaredTypeDescriptor specializedEnclosingTypeDescriptor =
        isStatic()
            ? enclosingTypeDescriptor
            : enclosingTypeDescriptor.specializeTypeVariables(
                updatedReplacingTypeDescriptorByTypeVariable);

    if (specializedEnclosingTypeDescriptor.equals(getEnclosingTypeDescriptor())
        && specializedTypeArgumentDescriptors.equals(typeArgumentTypeDescriptors)
        && specializedReturnTypeDescriptor.equals(returnTypeDescriptor)
        && specializedParameterTypeDescriptors.equals(parameterTypeDescriptors)) {
      // Nothing has changed, return the original descriptor.
      return this;
    }

    return MethodDescriptor.Builder.from(this)
        .setDeclarationDescriptor(getDeclarationDescriptor())
        .setTypeArgumentTypeDescriptors(specializedTypeArgumentDescriptors)
        .setReturnTypeDescriptor(specializedReturnTypeDescriptor)
        .updateParameterTypeDescriptors(specializedParameterTypeDescriptors)
        .setEnclosingTypeDescriptor(specializedEnclosingTypeDescriptor)
        .build();
  }

  private Function<TypeVariable, ? extends TypeDescriptor> propagateChangesInBounds(
      ImmutableList<? extends TypeDescriptor> argumentTypeDescriptors,
      Function<TypeVariable, ? extends TypeDescriptor> replacingTypeDescriptorByTypeVariable) {
    if (argumentTypeDescriptors.isEmpty()) {
      return replacingTypeDescriptorByTypeVariable;
    }

    TypeDescriptor nextArgument = Iterables.getFirst(argumentTypeDescriptors, null);
    return propagateChangesInBounds(
        argumentTypeDescriptors.subList(1, argumentTypeDescriptors.size()),
        maybeIntroduceNewTypeVariable(nextArgument, replacingTypeDescriptorByTypeVariable));
  }

  private static Function<TypeVariable, ? extends TypeDescriptor> maybeIntroduceNewTypeVariable(
      TypeDescriptor typeDescriptor,
      Function<TypeVariable, ? extends TypeDescriptor> replacingTypeDescriptorByTypeVariable) {
    if (!typeDescriptor.isTypeVariable()) {
      // Not a type variable, nothing to do.
      return replacingTypeDescriptorByTypeVariable;
    }

    var typeVariable = ((TypeVariable) typeDescriptor).toDeclaration();
    if (typeVariable.isWildcardOrCapture()) {
      // Wildcards bounds are specialized directly by `specializeTypeVariables` since the
      // declaration is implicit in the usage.
      // TODO(b/246332093): Captures in the current state are in most places handled like wildcards
      // until we improve the modeling; the scenarios where we rely heavily in substitution (e.g.
      // bridge construction) do not involve captures.
      return replacingTypeDescriptorByTypeVariable;
    }

    if (!typeVariable
        .specializeTypeVariables(replacingTypeDescriptorByTypeVariable)
        .equals(typeVariable)) {
      // If the type variable will be replaced, there is no need to change their bounds, e.g.
      //
      //   <S, T extends X<S>>
      //
      // with a mapping
      //
      //     S -> A
      //     T -> B
      //
      // will result in
      //
      //    <A, B>
      //
      return replacingTypeDescriptorByTypeVariable;
    }

    // Check if the bound is affected.
    var boundTypeDescriptor = typeVariable.getUpperBoundTypeDescriptor();
    var specializedBoundTypeDescriptor =
        boundTypeDescriptor.specializeTypeVariables(replacingTypeDescriptorByTypeVariable);
    if (boundTypeDescriptor.equals(specializedBoundTypeDescriptor)) {
      return replacingTypeDescriptorByTypeVariable;
    }

    // The bound was specialized, introduce a new type variable and add it to the replacement
    // function.
    TypeVariable replacementTypeVariable =
        TypeVariable.newBuilder()
            .setName(typeVariable.getName())
            .setUniqueKey(
                typeVariable.getUniqueKey()
                    + "#SpecializedBound#"
                    + specializedBoundTypeDescriptor.getUniqueId())
            .setUpperBoundTypeDescriptorFactory(
                self ->
                    // The bound might also refer to the variable that is being replaced, e.g. in
                    //
                    //   T extends X<T, U>
                    //
                    // if there is a replacement U -> V, the declaration needs to be
                    //
                    //  T1 extends X<T1, V>.
                    specializedBoundTypeDescriptor.specializeTypeVariables(
                        ImmutableMap.of(typeVariable, self)))
            .build();

    return tv ->
        tv.equals(typeVariable)
            ? replacementTypeVariable
            : replacingTypeDescriptorByTypeVariable.apply(tv);
  }

  /**
   * Returns the mapping between all the type variables in the enclosing context and the type
   * arguments.
   *
   * <p>Note: It does not include the mapping for wildcards; hence this parameterization is not
   * enough to recreate a method descriptor from its declaration.
   */
  @Memoized
  public Map<TypeVariable, TypeDescriptor> getParameterization() {
    Map<TypeVariable, TypeDescriptor> parameterization = new LinkedHashMap<>();
    Streams.forEachPair(
        getDeclarationDescriptor().getTypeParameterTypeDescriptors().stream(),
        getTypeArgumentTypeDescriptors().stream(),
        parameterization::put);
    if (!isStatic()) {
      parameterization.putAll(getEnclosingTypeDescriptor().getParameterization());
    }
    return parameterization;
  }

  @Override
  public final String toString() {
    StringBuilder sb = new StringBuilder();
    switch (getJsInfo().getJsMemberType()) {
      case METHOD:
        sb.append("@JsMethod ");
        break;
      case PROPERTY:
        sb.append("@JsProperty ");
        break;
      case CONSTRUCTOR:
        sb.append("@JsConstructor ");
        break;
      default:
        break;
    }
    if (isSuspendFunction()) {
      sb.append("suspend ");
    }
    if (isStatic()) {
      sb.append("static ");
    }
    if (isDefaultMethod()) {
      sb.append("default ");
    }

    sb.append(getReturnTypeDescriptor().getReadableDescription())
        .append(" ")
        .append(getEnclosingTypeDescriptor().getSimpleSourceName())
        .append('.')
        .append(getName())
        .append(
            getParameterTypeDescriptors().stream()
                .map(TypeDescriptor::getReadableDescription)
                .collect(joining(",", "(", ")")));
    if (getVisibility().isPackagePrivate()) {
      sb.append(" pp");
    }
    switch (getOrigin()) {
      case SPECIALIZING_BRIDGE:
        sb.append(" s-bridge");
        break;
      case GENERALIZING_BRIDGE:
        sb.append(" g-bridge");
        break;
      case DEFAULT_METHOD_BRIDGE:
        sb.append(" d-bridge");
        break;
      case ABSTRACT_STUB:
        sb.append(" stub");
        break;
      case SOURCE:
        break;
      default:
        sb.append(" synthetic");
    }
    return sb.toString();
  }

  @Override
  MemberDescriptor acceptInternal(Processor processor) {
    return Visitor_MethodDescriptor.visit(processor, this);
  }

  /** A Builder for MethodDescriptors. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setDefaultMethod(boolean isDefault);

    public abstract Builder setNative(boolean isNative);

    public abstract Builder setStatic(boolean isStatic);

    public abstract Builder setConstructor(boolean isConstructor);

    public abstract Builder setAbstract(boolean isAbstract);

    public abstract Builder setSynchronized(boolean isSynchronized);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setSynthetic(boolean isSynthetic);

    public abstract Builder setSuspendFunction(boolean suspendFunction);

    public Builder makeAbstractStub(MethodDescriptor methodDescriptor) {
      return setBridgeOrigin(methodDescriptor)
          .setOrigin(MethodOrigin.ABSTRACT_STUB)
          .setBridgeTarget(null)
          .setSynthetic(true)
          .makeDeclaration()
          // Clear properties that might have been carried over when creating this
          // descriptor from an existing one.
          .setDefaultMethod(false)
          .setAbstract(true)
          .setNative(false);
    }

    public Builder makeBridge(
        MethodOrigin methodOrigin,
        MethodDescriptor originDescriptor,
        MethodDescriptor targetDescriptor) {
      return setBridgeOrigin(originDescriptor)
          .setOrigin(methodOrigin)
          .setBridgeTarget(targetDescriptor)
          .makeDeclaration()
          .setSynthetic(true)
          // Clear properties that might have been carried over when creating this
          // descriptor from an existing one.
          .setDefaultMethod(false)
          .setAbstract(false)
          .setNative(false);
    }

    public Builder makeDeclaration() {
      // Replace type variables as some might haven been replaced due to changes in bounds.
      var typeParameters = new ArrayList<>(getTypeParameterTypeDescriptors());

      int i = 0;
      // Argument list might be shorter than parameter list due to raw types.
      for (var argument : getTypeArgumentTypeDescriptors()) {
        if (isFreeTypeVariable(argument)) {
          typeParameters.set(i, (TypeVariable) argument);
        }
        i++;
      }

      return setDeclarationDescriptor(null)
          .setTypeParameterTypeDescriptors(typeParameters)
          .setTypeArgumentTypeDescriptors(ImmutableList.of());
    }

    private static boolean isFreeTypeVariable(TypeDescriptor typeDescriptor) {
      return typeDescriptor.isTypeVariable() && !typeDescriptor.isWildcardOrCapture();
    }

    /** Internal use only. Use {@link #makeBridge}. */
    abstract Builder setBridgeOrigin(MethodDescriptor bridgeOrigin);

    /** Internal use only. Use {@link #makeBridge}. */
    abstract Builder setBridgeTarget(MethodDescriptor bridgeOrigin);

    public abstract Builder setEnumSyntheticMethod(boolean isEnumSyntheticMethod);

    public abstract Builder setEnclosingTypeDescriptor(
        DeclaredTypeDescriptor enclosingTypeDescriptor);

    public abstract Builder setEnclosingMethodDescriptor(
        @Nullable MethodDescriptor enclosingMethodDescriptor);

    public abstract DeclaredTypeDescriptor getEnclosingTypeDescriptor();

    public abstract Builder setName(String name);

    public abstract Builder setReturnTypeDescriptor(TypeDescriptor returnTypeDescriptor);

    public abstract TypeDescriptor getReturnTypeDescriptor();

    public abstract Builder setVisibility(Visibility visibility);

    public abstract Builder setOriginalJsInfo(JsInfo jsInfo);

    public abstract Builder setOriginalKtInfo(KtInfo ktInfo);

    public abstract Builder setKtObjcInfo(KtObjcInfo ktObjcInfo);

    public abstract Builder setAnnotations(List<Annotation> annotations);

    public abstract Builder setOrigin(MethodOrigin methodOrigin);

    public abstract Builder setTypeParameterTypeDescriptors(
        Iterable<TypeVariable> typeParameterTypeDescriptors);

    @CanIgnoreReturnValue
    public Builder addTypeParameterTypeDescriptors(
        int index, TypeVariable... typeParameterTypeDescriptors) {
      return addTypeParameterTypeDescriptors(index, Arrays.asList(typeParameterTypeDescriptors));
    }

    @CanIgnoreReturnValue
    public Builder addTypeParameterTypeDescriptors(
        int index, List<TypeVariable> typeParameterTypeDescriptors) {
      List<TypeVariable> newTypeParameterTypeDescriptors =
          new ArrayList<>(getTypeParameterTypeDescriptors());
      newTypeParameterTypeDescriptors.addAll(index, typeParameterTypeDescriptors);
      return setTypeParameterTypeDescriptors(newTypeParameterTypeDescriptors);
    }

    public abstract ImmutableList<TypeVariable> getTypeParameterTypeDescriptors();

    @CanIgnoreReturnValue
    public Builder setParameterTypeDescriptors(TypeDescriptor... parameterTypeDescriptors) {
      return setParameterTypeDescriptors(Arrays.asList(parameterTypeDescriptors));
    }

    @CanIgnoreReturnValue
    public Builder setParameterTypeDescriptors(List<TypeDescriptor> parameterTypeDescriptors) {
      return setParameterDescriptors(toParameterDescriptors(parameterTypeDescriptors));
    }

    @CanIgnoreReturnValue
    public Builder updateParameterTypeDescriptors(List<TypeDescriptor> parameterTypeDescriptors) {
      return setParameterDescriptors(
          Streams.zip(
                  getParameterDescriptors().stream(),
                  parameterTypeDescriptors.stream(),
                  (pd, td) -> pd.toBuilder().setTypeDescriptor(td).build())
              .collect(toImmutableList()));
    }

    @CanIgnoreReturnValue
    public Builder addParameterTypeDescriptors(
        int index, TypeDescriptor... parameterTypeDescriptors) {
      return addParameterTypeDescriptors(index, Arrays.asList(parameterTypeDescriptors));
    }

    @CanIgnoreReturnValue
    public Builder addParameterTypeDescriptors(
        int index, Collection<TypeDescriptor> parameterTypeDescriptors) {
      List<ParameterDescriptor> newParameterDescriptors =
          new ArrayList<>(getParameterDescriptors());
      newParameterDescriptors.addAll(index, toParameterDescriptors(parameterTypeDescriptors));
      return setParameterDescriptors(newParameterDescriptors);
    }

    @CanIgnoreReturnValue
    public Builder removeParameterTypeDescriptors() {
      return setParameterDescriptors();
    }

    @CanIgnoreReturnValue
    public abstract Builder setExceptionTypeDescriptors(
        ImmutableList<TypeDescriptor> exceptionTypeDescriptors);

    public abstract ImmutableList<TypeDescriptor> getTypeArgumentTypeDescriptors();

    public abstract Builder setTypeArgumentTypeDescriptors(List<TypeDescriptor> typeArguments);

    abstract ImmutableList<ParameterDescriptor> getParameterDescriptors();

    public ImmutableList<TypeDescriptor> getParameterTypeDescriptors() {
      return getParameterDescriptors().stream()
          .map(ParameterDescriptor::getTypeDescriptor)
          .collect(toImmutableList());
    }

    static ImmutableList<ParameterDescriptor> toParameterDescriptors(
        Collection<TypeDescriptor> parameterTypeDescriptors) {
      return parameterTypeDescriptors.stream()
          .map(
              typeDescriptor ->
                  ParameterDescriptor.newBuilder().setTypeDescriptor(typeDescriptor).build())
          .collect(toImmutableList());
    }

    @CanIgnoreReturnValue
    public Builder setParameterDescriptors(ParameterDescriptor... parameterDescriptors) {
      return setParameterDescriptors(Arrays.asList(parameterDescriptors));
    }

    public abstract Builder setParameterDescriptors(
        Iterable<ParameterDescriptor> parameterDescriptors);

    public abstract Builder setParameterDescriptors(
        ImmutableList<ParameterDescriptor> parameterDescriptors);

    @CanIgnoreReturnValue
    public Builder removeParameterOptionality() {
      return setParameterDescriptors(
          getParameterDescriptors().stream()
              .map(
                  parameterDescriptor ->
                      parameterDescriptor.toBuilder().setJsOptional(false).build())
              .collect(toImmutableList()));
    }

    @CanIgnoreReturnValue
    public Builder setDeclarationDescriptor(MethodDescriptor declarationMethodDescriptor) {
      return setDeclarationDescriptorOrNullIfSelf(declarationMethodDescriptor);
    }

    // Accessors to support validation, default construction and custom setters.
    abstract Builder setDeclarationDescriptorOrNullIfSelf(
        MethodDescriptor declarationMethodDescriptor);

    abstract MethodDescriptor getDeclarationDescriptorOrNullIfSelf();

    abstract boolean isConstructor();

    abstract boolean isNative();

    abstract MethodDescriptor autoBuild();

    public MethodDescriptor build() {
      if (isConstructor()) {
        setReturnTypeDescriptor(getEnclosingTypeDescriptor().toNonNullable());
        setName(CONSTRUCTOR_METHOD_NAME);
      }

      boolean isNative = isNative() || getEnclosingTypeDescriptor().isNative();
      if (!isNative && ignoreNonNativeJsInfo.get()) {
        setOriginalJsInfo(JsInfo.NONE);
      }

      if (getDeclarationDescriptorOrNullIfSelf() == null) {
        // Use a canonical version of the enclosing type descriptor in method declarations.
        setEnclosingTypeDescriptor(getEnclosingTypeDescriptor().getDeclarationDescriptor());
      }

      MethodDescriptor methodDescriptor = autoBuild();

      if (methodDescriptor.isDeclaration()) {
        checkState(methodDescriptor.getTypeArgumentTypeDescriptors().isEmpty());
      }

      MethodDescriptor internedMethodDescriptor = interner.intern(methodDescriptor);
      if (internedMethodDescriptor == methodDescriptor) {
        // This method descriptor is seen for the first time, make sure that it has been constructed
        // properly.

        // Bridge methods cannot be abstract nor native,
        checkState(
            !methodDescriptor.isGeneralizingBridge()
                || (!methodDescriptor.isAbstract() || !methodDescriptor.isNative()));
        // Bridge methods have to be marked synthetic,
        checkState(!methodDescriptor.isGeneralizingBridge() || methodDescriptor.isSynthetic());

        // Static methods cannot be abstract
        checkState(!methodDescriptor.isStatic() || !methodDescriptor.isAbstract());

        // Default methods can not be static.
        checkState(!methodDescriptor.isDefaultMethod() || !methodDescriptor.isStatic());

        // Default methods can not be constructors.
        checkState(!methodDescriptor.isDefaultMethod() || !methodDescriptor.isConstructor());

        // Default methods can not be bridges.
        checkState(!methodDescriptor.isDefaultMethod() || !methodDescriptor.isAbstract());

        // Default methods can not be abstract.
        checkState(!methodDescriptor.isDefaultMethod() || !methodDescriptor.isGeneralizingBridge());

        // JsAsync local function are not supported yet.
        checkState(!methodDescriptor.isLocalFunction() || !methodDescriptor.isJsAsync());

        // Default methods can only be in interfaces.
        checkState(
            !methodDescriptor.isDefaultMethod()
                || methodDescriptor.getEnclosingTypeDescriptor().isInterface());

        // At most 1 varargs parameter.
        checkState(
            methodDescriptor.getParameterDescriptors().stream()
                    .filter(ParameterDescriptor::isVarargs)
                    .count()
                <= 1);

        // varargs parameter is the last one.
        checkState(
            !methodDescriptor.isVarargs()
                || Iterables.getLast(methodDescriptor.getParameterDescriptors()).isVarargs());

        // Optional parameters must be trailing.
        checkState(
            methodDescriptor.getParameterDescriptors().stream()
                .dropWhile(not(ParameterDescriptor::isOptional))
                .allMatch(ParameterDescriptor::isOptional));

        checkState(
            methodDescriptor.getTypeParameterTypeDescriptors().stream()
                .map(TypeVariable::getNullabilityAnnotation)
                .allMatch(Predicate.isEqual(NullabilityAnnotation.NONE)));

        // Check that the properties of the declaration descriptor are consistent with those
        // of the method descriptor itself.
        checkDeclarationDescriptor(methodDescriptor);
      }
      return internedMethodDescriptor;
    }

    private static void checkDeclarationDescriptor(MethodDescriptor methodDescriptor) {
      if (methodDescriptor.isDeclaration()) {
        return;
      }

      MethodDescriptor declaration = methodDescriptor.getDeclarationDescriptor();
      checkState(declaration.isDeclaration());

      checkState(
          declaration
              .getEnclosingTypeDescriptor()
              .isSameBaseType(methodDescriptor.getEnclosingTypeDescriptor()));

      checkState(methodDescriptor.getName().equals(declaration.getName()));

      checkState(methodDescriptor.isConstructor() == declaration.isConstructor());
      // TODO(b/159983149): Uncomment when fixed.
      // checkState(methodDescriptor.isAbstract() == declaration.isAbstract());
      // checkState(methodDescriptor.isNative() == declaration.isNative());
      checkState(methodDescriptor.isDefaultMethod() == declaration.isDefaultMethod());
      checkState(methodDescriptor.isFinal() == declaration.isFinal());
      checkState(methodDescriptor.isLocalFunction() == declaration.isLocalFunction());
      checkState(methodDescriptor.isStatic() == declaration.isStatic());
      checkState(
          declaration.getParameterTypeDescriptors().size()
              == methodDescriptor.getParameterTypeDescriptors().size());
      checkState(
          declaration.getReturnTypeDescriptor().isPrimitive()
              == methodDescriptor.getReturnTypeDescriptor().isPrimitive());

      checkState(
          !methodDescriptor.isGeneralizingBridge()
              || methodDescriptor.isJsMethod() == methodDescriptor.getBridgeOrigin().isJsMethod());
    }

    public static Builder from(MethodDescriptor methodDescriptor) {
      return methodDescriptor.toBuilder();
    }

    private static final ThreadLocalInterner<MethodDescriptor> interner =
        new ThreadLocalInterner<>();
  }
}
