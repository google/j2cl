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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.j2cl.ast.FieldDescriptor.FieldOrigin;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/** A reference to a method. */
@AutoValue
public abstract class MethodDescriptor extends MemberDescriptor {
  /** A method parameter descriptor */
  @AutoValue
  public abstract static class ParameterDescriptor {
    public abstract TypeDescriptor getTypeDescriptor();

    public abstract boolean isVarargs();

    public abstract boolean isJsOptional();

    public abstract boolean isDoNotAutobox();

    @Memoized
    public ParameterDescriptor toRawParameterDescriptor() {
      return toBuilder().setTypeDescriptor(getTypeDescriptor().toRawTypeDescriptor()).build();
    }

    public abstract Builder toBuilder();

    public static Builder newBuilder() {
      return new AutoValue_MethodDescriptor_ParameterDescriptor.Builder()
          .setVarargs(false)
          .setJsOptional(false)
          .setDoNotAutobox(false);
    }

    private static final ThreadLocalInterner<ParameterDescriptor> interner =
        new ThreadLocalInterner<>();

    /** A Builder for ParameterDescriptor. */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setTypeDescriptor(TypeDescriptor typeDescriptor);

      public abstract Builder setVarargs(boolean isVarargs);

      public abstract Builder setJsOptional(boolean isJsOptional);

      public abstract Builder setDoNotAutobox(boolean isDoNotAutobox);

      abstract ParameterDescriptor autoBuild();

      public ParameterDescriptor build() {
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
    INSTANCE_OF_SUPPORT_METHOD,
    GENERALIZING_BRIDGE, // Bridges a more general signature to a more specific one.
    SPECIALIZING_BRIDGE, // Bridges a more specific signature to a more general one.
    DEFAULT_METHOD_BRIDGE, // Bridges to a default method interface.
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
      switch (this) {
          // User written methods and bridges need to be mangled the same way.
        case SOURCE:
        case GENERALIZING_BRIDGE:
        case SPECIALIZING_BRIDGE:
        case DEFAULT_METHOD_BRIDGE:
          return "m_";
          // Getters and setters need to be mangled as fields.
        case SYNTHETIC_PROPERTY_SETTER:
        case SYNTHETIC_PROPERTY_GETTER:
          return FieldOrigin.SOURCE.getPrefix();
          // Don't prefix the rest, they all start with "$"
        default:
          return "";
      }
    }

    @Override
    public boolean isInstanceOfSupportMember() {
      return this == INSTANCE_OF_SUPPORT_METHOD;
    }
  }

  public static final String INIT_METHOD_NAME = "$init";
  public static final String CTOR_METHOD_PREFIX = "$ctor";
  public static final String CLINIT_METHOD_NAME = "$clinit";
  public static final String EQUALS_METHOD_NAME = "equals";
  public static final String VALUE_OF_METHOD_NAME = "valueOf"; // Boxed type valueOf() method.
  public static final String VALUE_METHOD_SUFFIX = "Value"; // Boxed type **Value() method.
  public static final String IS_INSTANCE_METHOD_NAME = "$isInstance";
  public static final String MARK_IMPLEMENTOR_METHOD_NAME = "$markImplementor";
  public static final String CREATE_METHOD_NAME = "$create";
  public static final String LOAD_MODULES_METHOD_NAME = "$loadModules";
  public static final String COPY_METHOD_NAME = "$copy";

  public static String buildMethodSignature(
      String name, TypeDescriptor... parameterTypeDescriptors) {
    return buildMethodSignature(name, Arrays.asList(parameterTypeDescriptors));
  }

  private static String buildMethodSignature(
      String name, List<TypeDescriptor> parameterTypeDescriptors) {
    return name
        + parameterTypeDescriptors.stream()
            .map(MethodDescriptor::getSignatureStringForParameter)
            .collect(joining(",", "(", ")"));
  }

  private static String getSignatureStringForParameter(TypeDescriptor typeDescriptor) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      return ((DeclaredTypeDescriptor) typeDescriptor).getQualifiedBinaryName();
    }
    if (typeDescriptor instanceof PrimitiveTypeDescriptor) {
      return ((PrimitiveTypeDescriptor) typeDescriptor).getSimpleSourceName();
    }
    if (typeDescriptor instanceof ArrayTypeDescriptor) {
      return getSignatureStringForParameter(
              ((ArrayTypeDescriptor) typeDescriptor).getComponentTypeDescriptor())
          + "[]";
    }
    return getSignatureStringForParameter(typeDescriptor.toRawTypeDescriptor());
  }

  public abstract boolean isAbstract();

  @Override
  public abstract boolean isConstructor();

  public boolean isVarargs() {
    return getParameterDescriptors().stream().anyMatch(ParameterDescriptor::isVarargs);
  }

  @Override
  public abstract boolean isDefaultMethod();

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
  public boolean isGeneralizingdBridge() {
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

  public boolean isDefaultMethodBridge() {
    return getOrigin() == MethodOrigin.DEFAULT_METHOD_BRIDGE;
  }

  /** Returns true if the bridge was build with {@code candidateTarget} as its target. */
  boolean isBridgeTarget(MethodDescriptor candidateTarget) {
    MethodDescriptor method = getBridgeTarget();
    return method != null
        && method.getDeclarationDescriptor().equals(candidateTarget.getDeclarationDescriptor());
  }

  public abstract ImmutableList<ParameterDescriptor> getParameterDescriptors();

  public abstract TypeDescriptor getReturnTypeDescriptor();

  /** Type parameters declared in the method. */
  public abstract ImmutableList<TypeVariable> getTypeParameterTypeDescriptors();

  public boolean isParameterOptional(int i) {
    return getParameterDescriptors().get(i).isJsOptional();
  }

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

  public boolean isInit() {
    return getOrigin() == MethodOrigin.SYNTHETIC_INSTANCE_INITIALIZER;
  }

  /**
   * Returns the descriptor of the method declaration. A method descriptor might describe a
   * specialized version of a method, e.g.
   *
   * <p>
   *
   * <pre>
   *   class A<T> {
   *     void m(T t);  // Method declaration described as m(T).
   *   }
   *
   *   // Method call with a method descriptor for m(String) that has the method descriptor
   *   // for m(T) as its declaration.
   *   new A<String>().m("Hi");
   * <p>
   * </pre>
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

  @Override
  @Memoized
  public MethodDescriptor toRawMemberDescriptor() {
    return toBuilder()
        .setEnclosingTypeDescriptor(getEnclosingTypeDescriptor().toRawTypeDescriptor())
        .setTypeParameterTypeDescriptors(ImmutableList.of())
        .setReturnTypeDescriptor(getReturnTypeDescriptor().toRawTypeDescriptor())
        .setParameterDescriptors(
            getParameterDescriptors().stream()
                .map(ParameterDescriptor::toRawParameterDescriptor)
                .collect(toImmutableList()))
        .build();
  }

  /** Removes the type parameters declared in the method. */
  // TODO(b/13096948): This is only needed to remove the type parameters from JsFunction
  // methods. JsCompiler does not have a way to represent type parameterized function types; if
  // typedefs were allowed templates, J2CL could represent JsFunctions using typedefs allowing
  // the methods be parameterized.
  @Memoized
  public MethodDescriptor withoutTypeParameters() {

    Set<TypeVariable> typeParameters = new HashSet<>(getTypeParameterTypeDescriptors());
    return Builder.from(
            specializeTypeVariables(
                p ->
                    typeParameters.contains(p)
                        ? TypeVariable.createWildcardWithBound(p.getBoundTypeDescriptor())
                        : p))
        .setDeclarationDescriptor(
            isDeclaration() ? null : getDeclarationDescriptor().withoutTypeParameters())
        .setTypeParameterTypeDescriptors(ImmutableList.of())
        .build();
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
    return !isStatic() && !isConstructor();
  }

  /** Whether the method does dynamic dispatch and can be overridden. */
  boolean isPolymorphic() {
    return isInstanceMember() && !getVisibility().isPrivate();
  }

  @Override
  public boolean isMethod() {
    return true;
  }

  public boolean isPropertyGetter() {
    return isJsPropertyGetter() || getOrigin() == MethodOrigin.SYNTHETIC_PROPERTY_GETTER;
  }

  public boolean isPropertySetter() {
    return isJsPropertySetter() || getOrigin() == MethodOrigin.SYNTHETIC_PROPERTY_SETTER;
  }

  public abstract boolean isEnumSyntheticMethod();

  @Override
  @Memoized
  public boolean isOrOverridesJavaLangObjectMethod() {
    if (!isPolymorphic()) {
      return false;
    }

    if (TypeDescriptors.isJavaLangObject(getEnclosingTypeDescriptor())) {
      return true;
    }

    return getJavaOverriddenMethodDescriptors().stream()
        .map(MethodDescriptor::getEnclosingTypeDescriptor)
        .anyMatch(TypeDescriptors::isJavaLangObject);
  }

  @Override
  @Memoized
  public String getBinaryName() {
    return getOrigin().getName() == null ? getName() : getOrigin().getName();
  }

  @Memoized
  @Override
  public String getMangledName() {
    if (!isDeclaration()) {
      return getDeclarationDescriptor().getMangledName();
    }
    if (isGeneralizingdBridge()) {
      // Bridges are methods that fill the gap between the overridden parent method and
      // a specialized override, For that reason their mangled name has to be the same as the
      // method they override but the other properties (e.g. parameter types, etc) are derived
      // from the specialized method.
      return getBridgeOrigin().getMangledName();
    }

    if (isConstructor()) {
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

    if (getOrigin().isInstanceOfSupportMember()) {
      // Class support methods, like $isInstance and $markImplementor, should not be mangled.
      return getName();
    }

    // All special cases have been handled. Go ahead and construct the mangled name for a plain
    // Java method.
    String suffix = "";
    if (!isStatic()) {
      // Only use suffixes for instance methods. Static methods are always called through the
      // right constructor, no need to add a suffix to avoid collisions.
      switch (getVisibility()) {
        case PRIVATE:
          // To ensure that private methods never override each other.
          suffix = "_$p_" + getEnclosingTypeDescriptor().getMangledName();
          break;
        case PACKAGE_PRIVATE:
          // To ensure that package private methods only override one another when
          // they are in the same package.
          suffix =
              "_$pp_"
                  + getEnclosingTypeDescriptor()
                      .getTypeDeclaration()
                      .getPackageName()
                      .replace('.', '_');
          break;
        default:
          break;
      }
    }
    String parameterSignature = String.join("__", getMangledParameterTypes());
    return buildMangledName(parameterSignature + suffix);
  }

  /** Returns the list of mangled name of parameters' types. */
  private List<String> getMangledParameterTypes() {
    return Lists.transform(
        getParameterTypeDescriptors(),
        parameterTypeDescriptor -> parameterTypeDescriptor.toRawTypeDescriptor().getMangledName());
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
   * Returns whether this method descriptor overrides the provided method descriptor from the Java
   * source perspective.
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
    // An overriding method can not reduce visibility.
    if (thisVisibility.level < thatVisibility.level) {
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
  public String getSignature() {
    return buildMethodSignature(getName(), getParameterTypeDescriptors());
  }

  /**
   * The override key defines a grouping of method descriptors at a type that will be handled by the
   * same actual implementation.
   *
   * <p>The difference between an override signature and an override key is that the override key
   * will be different for package private methods in different packages even if they have the same
   * signature.
   */
  String getOverrideKey() {
    checkState(isPolymorphic());
    return getVisibility().isPackagePrivate() ? getPackagePrivateOverrideKey() : getSignature();
  }

  // This is a somewhat hacky way to differentiate package private override keys. A more principled
  // approach is to introduce a value type with two fields, so that we can use it as a key in
  // the maps used for the bridge method computations.
  private static final String PACKAGE_PRIVATE_MARKER = "{pp}-";

  String getPackagePrivateOverrideKey() {
    return getSignature()
        + PACKAGE_PRIVATE_MARKER
        + getEnclosingTypeDescriptor().getTypeDeclaration().getPackageName();
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_MethodDescriptor.Builder()
        // Default values.
        .setVisibility(Visibility.PUBLIC)
        .setJsInfo(JsInfo.NONE)
        .setAbstract(false)
        .setConstructor(false)
        .setDefaultMethod(false)
        .setNative(false)
        .setStatic(false)
        .setFinal(false)
        .setSynthetic(false)
        .setEnumSyntheticMethod(false)
        .setJsFunction(false)
        .setUnusableByJsSuppressed(false)
        .setDeprecated(false)
        .setOrigin(MethodOrigin.SOURCE)
        .setParameterDescriptors(Collections.emptyList())
        .setTypeParameterTypeDescriptors(Collections.emptyList())
        .setReturnTypeDescriptor(PrimitiveTypes.VOID);
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
   * the Java semantics persepective.
   */
  @Memoized
  public Set<MethodDescriptor> getJavaOverriddenMethodDescriptors() {
    return getOverriddenMethodDescriptors(
        this::isOverride, MethodDescriptor::getJavaOverriddenMethodDescriptors);
  }

  /**
   * Returns a set of the method descriptors that are overridden by {@code methodDescriptor} from
   * the JavaScript perspective.
   */
  @Memoized
  public Set<MethodDescriptor> getJsOverriddenMethodDescriptors() {
    return getOverriddenMethodDescriptors(
        m -> m.getMangledName().equals(getMangledName()),
        MethodDescriptor::getJsOverriddenMethodDescriptors);
  }

  /**
   * Generic recursive computation of overridden.
   *
   * <p>Note that the getOverriddenMethodsFn is passed as a parameter to take advantage of the
   * memoization and limit the computation cost.
   */
  private Set<MethodDescriptor> getOverriddenMethodDescriptors(
      Predicate<MethodDescriptor> isOverrideFn,
      Function<MethodDescriptor, Set<MethodDescriptor>> getOverriddenMethodsFn) {
    Set<MethodDescriptor> overriddenMethods = new LinkedHashSet<>();

    getEnclosingTypeDescriptor()
        .getSuperTypesStream()
        .flatMap(t -> t.getPolymorphicMethods().stream())
        .filter(isOverrideFn)
        .forEach(
            m -> {
              overriddenMethods.add(m);
              overriddenMethods.addAll(getOverriddenMethodsFn.apply(m));
            });

    return overriddenMethods;
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

    // Original type variables.
    TypeDescriptor returnTypeDescriptor = getReturnTypeDescriptor();
    ImmutableList<TypeDescriptor> parameterTypeDescriptors = getParameterTypeDescriptors();

    // Specialized type variables (possibly recursively).
    TypeDescriptor specializedReturnTypeDescriptor =
        returnTypeDescriptor.specializeTypeVariables(replacingTypeDescriptorByTypeVariable);
    ImmutableList<TypeDescriptor> specializedParameterTypeDescriptors =
        parameterTypeDescriptors.stream()
            .map(
                typeDescriptor ->
                    typeDescriptor.specializeTypeVariables(replacingTypeDescriptorByTypeVariable))
            .collect(toImmutableList());

    return MethodDescriptor.Builder.from(this)
        .setDeclarationDescriptor(getDeclarationDescriptor())
        .setReturnTypeDescriptor(specializedReturnTypeDescriptor)
        .updateParameterTypeDescriptors(specializedParameterTypeDescriptors)
        .setEnclosingTypeDescriptor(
            getEnclosingTypeDescriptor()
                .specializeTypeVariables(replacingTypeDescriptorByTypeVariable))
        .build();
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
      case SOURCE:
        break;
      default:
        sb.append(" synthetic");
    }
    return sb.toString();
  }

  /** A Builder for MethodDescriptors. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setDefaultMethod(boolean isDefault);

    public abstract Builder setNative(boolean isNative);

    public abstract Builder setStatic(boolean isStatic);

    public abstract Builder setConstructor(boolean isConstructor);

    public abstract Builder setAbstract(boolean isAbstract);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setSynthetic(boolean isSynthetic);

    public Builder makeBridge(
        MethodOrigin methodOrigin,
        MethodDescriptor originDescriptor,
        MethodDescriptor targetDescriptor) {
      return setBridgeOrigin(originDescriptor)
          .setOrigin(methodOrigin)
          .setBridgeTarget(targetDescriptor)
          .setSynthetic(true)
          // Clear properties that might have been carried over when creating this
          // descriptor from an existing one.
          .setDeclarationDescriptor(null)
          .setDefaultMethod(false)
          .setAbstract(false)
          .setNative(false);
    }

    /** Internal use only. Use {@link #makeBridge}. */
    abstract Builder setBridgeOrigin(MethodDescriptor bridgeOrigin);

    /** Internal use only. Use {@link #makeBridge}. */
    abstract Builder setBridgeTarget(MethodDescriptor bridgeOrigin);

    public abstract Builder setEnumSyntheticMethod(boolean isEnumSyntheticMethod);

    public abstract Builder setJsFunction(boolean isJsFunction);

    public abstract Builder setUnusableByJsSuppressed(boolean isUnusableByJsSuppressed);

    public abstract Builder setDeprecated(boolean isDeprecated);

    public abstract Builder setEnclosingTypeDescriptor(
        DeclaredTypeDescriptor enclosingTypeDescriptor);

    public abstract Builder setName(String name);

    public abstract Builder setReturnTypeDescriptor(TypeDescriptor returnTypeDescriptor);

    public abstract Builder setVisibility(Visibility visibility);

    public abstract Builder setJsInfo(JsInfo jsInfo);

    public abstract Builder setOrigin(MethodOrigin methodOrigin);

    public abstract Builder setTypeParameterTypeDescriptors(
        Iterable<TypeVariable> typeParameterTypeDescriptors);

    public Builder setParameterTypeDescriptors(TypeDescriptor... parameterTypeDescriptors) {
      return setParameterTypeDescriptors(Arrays.asList(parameterTypeDescriptors));
    }

    public Builder setParameterTypeDescriptors(List<TypeDescriptor> parameterTypeDescriptors) {
      return setParameterDescriptors(toParameterDescriptors(parameterTypeDescriptors));
    }

    private Builder updateParameterTypeDescriptors(List<TypeDescriptor> parameterTypeDescriptors) {
      return setParameterDescriptors(
          Streams.zip(
                  getParameterDescriptors().stream(),
                  parameterTypeDescriptors.stream(),
                  (pd, td) -> pd.toBuilder().setTypeDescriptor(td).build())
              .collect(toImmutableList()));
    }

    public Builder addParameterTypeDescriptors(
        int index, TypeDescriptor... parameterTypeDescriptors) {
      return addParameterTypeDescriptors(index, Arrays.asList(parameterTypeDescriptors));
    }

    public Builder addParameterTypeDescriptors(
        int index, Collection<TypeDescriptor> parameterTypeDescriptors) {
      List<ParameterDescriptor> newParameterDescriptors =
          new ArrayList<>(getParameterDescriptors());
      newParameterDescriptors.addAll(index, toParameterDescriptors(parameterTypeDescriptors));
      if (getDeclarationDescriptorOrNullIfSelf() != null) {
        setDeclarationDescriptorOrNullIfSelf(
            MethodDescriptor.Builder.from(getDeclarationDescriptorOrNullIfSelf())
                .addParameterTypeDescriptors(index, parameterTypeDescriptors)
                .build());
      }
      return setParameterDescriptors(newParameterDescriptors);
    }

    abstract MethodDescriptor getDeclarationDescriptorOrNullIfSelf();

    abstract ImmutableList<ParameterDescriptor> getParameterDescriptors();

    static ImmutableList<ParameterDescriptor> toParameterDescriptors(
        Collection<TypeDescriptor> parameterTypeDescriptors) {
      return parameterTypeDescriptors.stream()
          .map(
              typeDescriptor ->
                  ParameterDescriptor.newBuilder().setTypeDescriptor(typeDescriptor).build())
          .collect(toImmutableList());
    }

    public Builder setParameterDescriptors(ParameterDescriptor... parameterDescriptors) {
      return setParameterDescriptors(Arrays.asList(parameterDescriptors));
    }

    public abstract Builder setParameterDescriptors(
        Iterable<ParameterDescriptor> parameterDescriptors);

    public abstract Builder setParameterDescriptors(
        ImmutableList<ParameterDescriptor> parameterDescriptors);

    public Builder removeParameterOptionality() {
      return setParameterDescriptors(
          getParameterDescriptors().stream()
              .map(
                  parameterDescriptor ->
                      parameterDescriptor.toBuilder().setJsOptional(false).build())
              .collect(toImmutableList()));
    }

    public Builder setDeclarationDescriptor(MethodDescriptor declarationMethodDescriptor) {
      return setDeclarationDescriptorOrNullIfSelf(declarationMethodDescriptor);
    }

    // Accessors to support validation, default construction and custom setters.
    abstract Builder setDeclarationDescriptorOrNullIfSelf(
        MethodDescriptor declarationMethodDescriptor);

    abstract boolean isConstructor();

    abstract Optional<String> getName();

    abstract MethodDescriptor autoBuild();

    private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    public MethodDescriptor build() {
      if (isConstructor()) {
        // Constructors have a constant name <init>.
        checkState(!getName().isPresent() || getName().get().equals(CONSTRUCTOR_METHOD_NAME));

        setName(CONSTRUCTOR_METHOD_NAME);
      }

      checkState(getName().isPresent());

      MethodDescriptor methodDescriptor = autoBuild();

      MethodDescriptor internedMethodDescriptor = interner.intern(methodDescriptor);
      if (internedMethodDescriptor == methodDescriptor) {
        // This method descriptor is seen for the first time, make sure that it has been constructed
        // properly.

        // Bridge methods cannot be abstract nor native,
        checkState(
            !methodDescriptor.isGeneralizingdBridge()
                || (!methodDescriptor.isAbstract() || !methodDescriptor.isNative()));
        // Bridge methods have to be marked synthetic,
        checkState(!methodDescriptor.isGeneralizingdBridge() || methodDescriptor.isSynthetic());

        // Static methods cannot be abstract
        checkState(!methodDescriptor.isStatic() || !methodDescriptor.isAbstract());

        // Only constructors can be JsConstructor
        checkState(!methodDescriptor.isJsConstructor() || methodDescriptor.isConstructor());

        // Constructors can not be JsMethods.
        checkState(!methodDescriptor.isJsMethod() || !methodDescriptor.isConstructor());

        // Default methods can not be static.
        checkState(!methodDescriptor.isDefaultMethod() || !methodDescriptor.isStatic());

        // Default methods can not be constructors.
        checkState(!methodDescriptor.isDefaultMethod() || !methodDescriptor.isConstructor());

        // Default methods can not be bridges.
        checkState(!methodDescriptor.isDefaultMethod() || !methodDescriptor.isAbstract());

        // Default methods can not be abstract.
        checkState(
            !methodDescriptor.isDefaultMethod() || !methodDescriptor.isGeneralizingdBridge());

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
      checkState(methodDescriptor.isStatic() == declaration.isStatic());
      checkState(
          declaration.getParameterTypeDescriptors().size()
              == methodDescriptor.getParameterTypeDescriptors().size());
    }

    public static Builder from(MethodDescriptor methodDescriptor) {
      Builder builder = methodDescriptor.toBuilder();
      if (builder.isConstructor()) {
        // clear the name.
        builder.setName(null);
      }
      return builder;
    }

    private static final ThreadLocalInterner<MethodDescriptor> interner =
        new ThreadLocalInterner<>();
  }
}
