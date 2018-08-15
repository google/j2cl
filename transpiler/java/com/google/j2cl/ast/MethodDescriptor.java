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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

/** A reference to a method. */
@AutoValue
@Visitable
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
    SYNTHETIC_ADAPT_LAMBDA("<synthetic: adapt_lambda>");

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
      return "";
    }
  }

  public static final String INIT_METHOD_PREFIX = "$init";
  public static final String CTOR_METHOD_PREFIX = "$ctor";
  public static final String CLINIT_METHOD_NAME = "$clinit";
  public static final String VALUE_OF_METHOD_NAME = "valueOf"; // Boxed type valueOf() method.
  public static final String VALUE_METHOD_SUFFIX = "Value"; // Boxed type **Value() method.
  public static final String IS_INSTANCE_METHOD_NAME = "$isInstance";
  public static final String IS_ASSIGNABLE_FROM_METHOD_NAME = "$isAssignableFrom";
  public static final String CREATE_METHOD_NAME = "$create";
  public static final String LOAD_MODULES_METHOD_NAME = "$loadModules";

  public static String getSignature(String name, TypeDescriptor... parameterTypeDescriptors) {
    return getSignature(name, Arrays.asList(parameterTypeDescriptors));
  }

  private static String getSignature(String name, List<TypeDescriptor> parameterTypeDescriptors) {
    return name
        + parameterTypeDescriptors
            .stream()
            .map(TypeDescriptor::toRawTypeDescriptor)
            .map(TypeDescriptor::getQualifiedBinaryName)
            .collect(joining(",", "(", ")"));
  }

  public abstract boolean isAbstract();

  @Override
  public abstract boolean isConstructor();

  public boolean isVarargs() {
    return getParameterDescriptors().stream().anyMatch(ParameterDescriptor::isVarargs);
  }

  @Override
  public abstract boolean isDefaultMethod();

  public abstract boolean isBridge();

  public abstract ImmutableList<ParameterDescriptor> getParameterDescriptors();

  public abstract TypeDescriptor getReturnTypeDescriptor();

  /** Type parameters declared in the method. */
  public abstract ImmutableList<TypeVariable> getTypeParameterTypeDescriptors();

  public boolean isParameterOptional(int i) {
    return getParameterDescriptors().get(i).isJsOptional();
  }

  @Memoized
  public ImmutableList<TypeDescriptor> getParameterTypeDescriptors() {
    return getParameterDescriptors()
        .stream()
        .map(ParameterDescriptor::getTypeDescriptor)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public abstract MethodOrigin getOrigin();

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
    return getDeclarationMethodDescriptorOrNullIfSelf() == null
        ? this
        : getDeclarationMethodDescriptorOrNullIfSelf();
  }

  @Nullable
  // A method declaration can be itself but AutoValue does not allow for a property to be a
  // reference to the value object being created, so we use a backing nullable property where null
  // encodes a self reference for AutoValue purposes and provide the accessor above to hide
  // the details.
  abstract MethodDescriptor getDeclarationMethodDescriptorOrNullIfSelf();

  @Override
  @Memoized
  public MethodDescriptor toRawMemberDescriptor() {
    return toBuilder()
        .setEnclosingTypeDescriptor(getEnclosingTypeDescriptor().toRawTypeDescriptor())
        .setTypeParameterTypeDescriptors(ImmutableList.of())
        .setReturnTypeDescriptor(getReturnTypeDescriptor().toRawTypeDescriptor())
        .setParameterDescriptors(
            getParameterDescriptors()
                .stream()
                .map(ParameterDescriptor::toRawParameterDescriptor)
                .collect(toImmutableList()))
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
  public boolean isPolymorphic() {
    return !isStatic() && !isConstructor();
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

  public boolean isOrOverridesJsMember() {
    return isJsMember() || !getOverriddenJsMembers().isEmpty();
  }

  @Override
  @Memoized
  public boolean isOrOverridesJavaLangObjectMethod() {
    if (!isPolymorphic()) {
      return false;
    }

    if (TypeDescriptors.isJavaLangObject(getEnclosingTypeDescriptor())) {
      return true;
    }

    return getOverriddenMethodDescriptors()
        .stream()
        .map(MethodDescriptor::getEnclosingTypeDescriptor)
        .anyMatch(TypeDescriptors::isJavaLangObject);
  }

  @Override
  @Memoized
  public String getBinaryName() {
    return getOrigin() == MethodOrigin.SOURCE ? getName() : getOrigin().getName();
  }

  @Override
  public boolean isSameMember(MemberDescriptor thatMember) {
    // TODO(b/69130180): Ideally isSameMember should be not be overridden here and use the
    // implementation in MemberDescriptor, which relies in comparing the declarations directly.
    // The current codebase does not enforce the invariant and sometimes references to the same
    // member end up with different declarations.
    if (!(thatMember instanceof MethodDescriptor)) {
      return false;
    }

    // TODO(b/63118697): To work around nullability differences in method declarations,
    // just check that the methods have the same signatures and are in the same type.
    if (!inSameTypeAs(thatMember)) {
      return false;
    }

    MethodDescriptor thisMethod = this.getDeclarationDescriptor();
    MethodDescriptor thatMethod = (MethodDescriptor) thatMember.getDeclarationDescriptor();
    return thisMethod.getMethodSignature().equals(thatMethod.getMethodSignature());
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
        && !getEnclosingTypeDescriptor()
            .getTypeDeclaration()
            .getPackageName()
            .equals(that.getEnclosingTypeDescriptor().getTypeDeclaration().getPackageName())) {
      return false;
    }

    return this.getOverrideSignature().equals(that.getOverrideSignature());
  }

  /**
   * Returns whether this method descriptor overrides the provided method descriptor from the JS
   * source perspective.
   *
   * <p>In JS, methods override if they have the exact same name. Since we output package private
   * methods with a different name than public or protected methods the methods that override in our
   * output JS is slightly more restrictive than it is in the Java source.
   */
  public boolean isJsOverride(MethodDescriptor that) {
    if (!isOverride(that)) {
      return false;
    }

    Visibility thisVisibility = this.getVisibility();
    Visibility thatVisibility = that.getVisibility();

    return (thisVisibility.isPublicOrProtected() && thatVisibility.isPublicOrProtected())
        || thisVisibility == thatVisibility;
  }

  public Visibility getJsVisibility() {
    // TODO(29632512): stop overriding visibility and reflect the java visibilities.
    return getOrigin().overriddenJsVisibility;
  }

  public String getMethodSignature() {
    String name = getName();
    ImmutableList<TypeDescriptor> parameterTypeDescriptors = getParameterTypeDescriptors();
    return getSignature(name, parameterTypeDescriptors);
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
        .setBridge(false)
        .setJsFunction(false)
        .setUnusableByJsSuppressed(false)
        .setOrigin(MethodOrigin.SOURCE)
        .setParameterDescriptors(Collections.emptyList())
        .setTypeParameterTypeDescriptors(Collections.emptyList())
        .setReturnTypeDescriptor(PrimitiveTypes.VOID);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_MethodDescriptor.visit(processor, this);
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    String parameterString =
        getParameterDescriptors()
            .stream()
            .map(MethodDescriptor::getParameterReadableDescription)
            .collect(joining(", "));

    if (isConstructor()) {
      return J2clUtils.format(
          "%s(%s)", getEnclosingTypeDescriptor().getReadableDescription(), parameterString);
    }
    return J2clUtils.format(
        "%s %s.%s(%s)",
        getReturnTypeDescriptor().getReadableDescription(),
        getEnclosingTypeDescriptor().getReadableDescription(),
        getName(),
        parameterString);
  }

  private static String getParameterReadableDescription(ParameterDescriptor parameterDescriptor) {
    TypeDescriptor parameterTypeDescriptor =
        parameterDescriptor.getTypeDescriptor().toRawTypeDescriptor();
    if (parameterDescriptor.isVarargs()) {
      ArrayTypeDescriptor parameterArrayTypeDescriptor =
          (ArrayTypeDescriptor) parameterTypeDescriptor;
      return J2clUtils.format(
          "%s...",
          parameterArrayTypeDescriptor.getComponentTypeDescriptor().getReadableDescription());
    } else if (parameterDescriptor.isJsOptional()) {
      return J2clUtils.format("@JsOptional %s", parameterTypeDescriptor.getReadableDescription());
    }
    return J2clUtils.format("%s", parameterTypeDescriptor.getReadableDescription());
  }

  /** Returns a signature suitable for override checking from the Java source perspective. */
  @Memoized
  public String getOverrideSignature() {
    StringBuilder signatureBuilder = new StringBuilder("");

    signatureBuilder.append(getName());
    signatureBuilder.append("(");

    String separator = "";
    for (TypeDescriptor parameterType : getParameterTypeDescriptors()) {
      signatureBuilder.append(separator);
      signatureBuilder.append(parameterType.toRawTypeDescriptor().getQualifiedBinaryName());
      separator = ";";
    }
    signatureBuilder.append(")");
    return signatureBuilder.toString();
  }

  private Set<MethodDescriptor> getOverriddenJsMembers() {
    return Sets.filter(getOverriddenMethodDescriptors(), MethodDescriptor::isJsMember);
  }

  /** Returns a set of the method descriptors that are overridden by {@code methodDescriptor}. */
  @Memoized
  public Set<MethodDescriptor> getOverriddenMethodDescriptors() {
    return getEnclosingTypeDescriptor().getTypeDeclaration().getOverriddenMethodDescriptors(this);
  }

  public MethodDescriptor specializeTypeVariables(
      Map<TypeVariable, TypeDescriptor> applySpecializedTypeArgumentByTypeParameters) {
    return specializeTypeVariables(
        TypeDescriptors.mappingFunctionFromMap(applySpecializedTypeArgumentByTypeParameters));
  }

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
        parameterTypeDescriptors
            .stream()
            .map(
                typeDescriptor ->
                    typeDescriptor.specializeTypeVariables(replacingTypeDescriptorByTypeVariable))
            .collect(toImmutableList());

    return MethodDescriptor.Builder.from(this)
        .setReturnTypeDescriptor(specializedReturnTypeDescriptor)
        .updateParameterTypeDescriptors(specializedParameterTypeDescriptors)
        .build();
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

    public abstract Builder setBridge(boolean isBridge);

    public abstract Builder setJsFunction(boolean isJsFunction);

    public abstract Builder setUnusableByJsSuppressed(boolean isUnusableByJsSuppressed);

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

    public Builder updateParameterTypeDescriptors(List<TypeDescriptor> parameterTypeDescriptors) {
      checkArgument(parameterTypeDescriptors.size() == getParameterDescriptors().size());
      ImmutableList.Builder<ParameterDescriptor> listBuilder = ImmutableList.builder();
      Iterator<ParameterDescriptor> parameterDescriptorIterator =
          getParameterDescriptors().iterator();
      Iterator<TypeDescriptor> parameterTypeDescriptorIterator =
          parameterTypeDescriptors.iterator();
      while (parameterDescriptorIterator.hasNext()) {
        checkState(parameterTypeDescriptorIterator.hasNext());
        listBuilder.add(
            parameterDescriptorIterator
                .next()
                .toBuilder()
                .setTypeDescriptor(parameterTypeDescriptorIterator.next())
                .build());
      }
      return setParameterDescriptors(listBuilder.build());
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
      if (getDeclarationMethodDescriptorOrNullIfSelf() != null) {
        setDeclarationMethodDescriptorOrNullIfSelf(
            MethodDescriptor.Builder.from(getDeclarationMethodDescriptorOrNullIfSelf())
                .addParameterTypeDescriptors(index, parameterTypeDescriptors)
                .build());
      }
      return setParameterDescriptors(newParameterDescriptors);
    }

    public Builder addParameterTypeDescriptors(TypeDescriptor... parameterTypeDescriptors) {
      return addParameterTypeDescriptors(Arrays.asList(parameterTypeDescriptors));
    }

    public Builder addParameterTypeDescriptors(List<TypeDescriptor> parameterTypeDescriptors) {
      return addParameterTypeDescriptors(
          getParameterDescriptors().size(), parameterTypeDescriptors);
    }

    abstract MethodDescriptor getDeclarationMethodDescriptorOrNullIfSelf();

    abstract ImmutableList<ParameterDescriptor> getParameterDescriptors();

    static ImmutableList<ParameterDescriptor> toParameterDescriptors(
        Collection<TypeDescriptor> parameterTypeDescriptors) {
      return parameterTypeDescriptors
          .stream()
          .map(
              typeDescriptor ->
                  ParameterDescriptor.newBuilder().setTypeDescriptor(typeDescriptor).build())
          .collect(ImmutableList.toImmutableList());
    }

    public abstract Builder setParameterDescriptors(
        Iterable<ParameterDescriptor> parameterDescriptors);

    public abstract Builder setParameterDescriptors(
        ImmutableList<ParameterDescriptor> parameterDescriptors);

    public Builder removeParameterOptionality() {
      return setParameterDescriptors(
          getParameterDescriptors()
              .stream()
              .map(
                  parameterDescriptor ->
                      parameterDescriptor.toBuilder().setJsOptional(false).build())
              .collect(ImmutableList.toImmutableList()));
    }

    public Builder setDeclarationMethodDescriptor(MethodDescriptor declarationMethodDescriptor) {
      return setDeclarationMethodDescriptorOrNullIfSelf(declarationMethodDescriptor);
    }

    // Accessors to support validation, default construction and custom setters.
    abstract Builder setDeclarationMethodDescriptorOrNullIfSelf(
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
            !methodDescriptor.isBridge()
                || (!methodDescriptor.isAbstract() || !methodDescriptor.isNative()));
        // Bridge methods have to be marked synthetic,
        checkState(!methodDescriptor.isBridge() || methodDescriptor.isSynthetic());

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
        checkState(!methodDescriptor.isDefaultMethod() || !methodDescriptor.isBridge());

        // Default methods can only be in interfaces.
        checkState(
            !methodDescriptor.isDefaultMethod()
                || methodDescriptor.getEnclosingTypeDescriptor().isInterface());

        // At most 1 varargs parameter.
        checkState(
            methodDescriptor
                    .getParameterDescriptors()
                    .stream()
                    .filter(ParameterDescriptor::isVarargs)
                    .count()
                <= 1);

        // varargs parameter is the last one.
        checkState(
            !methodDescriptor.isVarargs()
                || Iterables.getLast(methodDescriptor.getParameterDescriptors()).isVarargs());
        if (methodDescriptor != methodDescriptor.getDeclarationDescriptor()) {
          List<TypeDescriptor> methodDeclarationParameterTypeDescriptors =
              methodDescriptor.getDeclarationDescriptor().getParameterTypeDescriptors();
          checkArgument(
              methodDeclarationParameterTypeDescriptors.size()
                  == methodDescriptor.getParameterTypeDescriptors().size(),
              "Method parameters (%s) for method %s don't match method declaration (%s)",
              methodDescriptor.getParameterTypeDescriptors(),
              methodDescriptor.getEnclosingTypeDescriptor().getSimpleSourceName()
                  + "."
                  + methodDescriptor.getName(),
              methodDeclarationParameterTypeDescriptors);
        }
      }
      return internedMethodDescriptor;
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
