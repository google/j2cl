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

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

/** A (by signature) reference to a method. */
@AutoValue
@Visitable
public abstract class MethodDescriptor extends MemberDescriptor {
  public static final String INIT_METHOD_NAME = "$init";
  public static final String VALUE_OF_METHOD_NAME = "valueOf"; // Boxed type valueOf() method.
  public static final String VALUE_METHOD_SUFFIX = "Value"; // Boxed type **Value() method.
  public static final String SAME_METHOD_NAME = "$same";
  public static final String NOT_SAME_METHOD_NAME = "$notSame";
  public static final String IS_INSTANCE_METHOD_NAME = "$isInstance";
  public static final String IS_ASSIGNABLE_FROM_METHOD_NAME = "$isAssignableFrom";
  public static final String TO_STRING_METHOD_NAME = "toString";
  public static final String CREATE_METHOD_NAME = "$create";
  public static final String MAKE_ENUM_NAME_METHOD_NAME = "$makeEnumName";

  public abstract boolean isAbstract();

  @Override
  public abstract boolean isConstructor();

  public abstract boolean isVarargs();

  @Override
  public abstract boolean isDefaultMethod();

  public abstract boolean isBridge();

  public abstract ImmutableList<TypeDescriptor> getParameterTypeDescriptors();

  public abstract TypeDescriptor getReturnTypeDescriptor();

  /** Type parameters declared in the method. */
  public abstract ImmutableList<TypeDescriptor> getTypeParameterTypeDescriptors();

  abstract BitSet getParameterOptionality();

  public boolean isParameterOptional(int i) {
    return getParameterOptionality().get(i);
  }

  public boolean isInit() {
    return getName().equals(INIT_METHOD_NAME) && !isStatic();
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
  public MethodDescriptor getDeclarationMethodDescriptor() {
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

  /**
   * Returns true if it is a vararg method that can be referenced by JavaScript side. A
   * non-JsOverlay JsMethod, and a JsFunction can be referenced by JavaScript side.
   *
   * <p>TODO: In our AST model, isJsMethod() and isJsOverlay() is NOT mutually-exclusive. We may
   * want to re-examine it after we import JsInteropRestrictionChecker and do refactoring on the
   * AST.
   */
  public boolean isJsMethodVarargs() {
    return isVarargs() && ((isJsMethod() && !isJsOverlay()) || isJsFunction() || isConstructor());
  }

  @Override
  public boolean isPolymorphic() {
    return !isStatic() && !isConstructor();
  }

  @Override
  public boolean isMethod() {
    return true;
  }

  public boolean isOrOverridesJsMember() {
    return isJsMember() || !getOverriddenJsMembers().isEmpty();
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
        && !getEnclosingClassTypeDescriptor()
            .getPackageName()
            .equals(that.getEnclosingClassTypeDescriptor().getPackageName())) {
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

  public String getMethodSignature() {
    String name = getName();
    ImmutableList<TypeDescriptor> parameterTypeDescriptors = getParameterTypeDescriptors();
    return MethodDescriptors.getSignature(name, parameterTypeDescriptors);
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
        .setVarargs(false)
        .setFinal(false)
        .setSynthetic(false)
        .setBridge(false)
        .setJsFunction(false)
        .setParameterOptionality(new BitSet())
        .setUnusableByJsSuppressed(false)
        .setParameterTypeDescriptors(Collections.emptyList())
        .setTypeParameterTypeDescriptors(Collections.emptyList())
        .setReturnTypeDescriptor(TypeDescriptors.get().primitiveVoid);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_MethodDescriptor.visit(processor, this);
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    // TODO(b/36493405): Add a parameter abstraction and simplify this and all code that traverses
    // parameters positionally.

    String parameterString = "";
    String separator = "";
    ImmutableList<TypeDescriptor> parameterTypeDescriptors =
        getDeclarationMethodDescriptor().getParameterTypeDescriptors();
    int varargsParameterIndex = parameterTypeDescriptors.size() - 1;
    for (int i = 0; i < parameterTypeDescriptors.size(); i++) {
      TypeDescriptor parameterTypeDescriptor =
          parameterTypeDescriptors.get(i).getRawTypeDescriptor();
      parameterString +=
          separator
              + (isJsMethodVarargs() && varargsParameterIndex == i
                  ? parameterTypeDescriptor.getComponentTypeDescriptor().getReadableDescription()
                      + "..."
                  : parameterTypeDescriptor.getReadableDescription());
      separator = ", ";
    }


    if (isConstructor()) {
      return J2clUtils.format(
          "%s(%s)", getEnclosingClassTypeDescriptor().getReadableDescription(), parameterString);
    }
    return J2clUtils.format(
        "%s %s.%s(%s)",
        getReturnTypeDescriptor().getReadableDescription(),
        getEnclosingClassTypeDescriptor().getReadableDescription(),
        getName(),
        parameterString);
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
      signatureBuilder.append(parameterType.getRawTypeDescriptor().getQualifiedBinaryName());
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
    return getEnclosingClassTypeDescriptor()
        .getTypeDeclaration()
        .getOverriddenMethodDescriptors(this);
  }

  /** A Builder for MethodDescriptors. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setDefaultMethod(boolean isDefault);

    public abstract Builder setNative(boolean isNative);

    public abstract Builder setStatic(boolean isStatic);

    public abstract Builder setVarargs(boolean isVarargs);

    public abstract Builder setConstructor(boolean isConstructor);

    public abstract Builder setAbstract(boolean isAbstract);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setSynthetic(boolean isSynthetic);

    public abstract Builder setBridge(boolean isBridge);

    public abstract Builder setJsFunction(boolean isJsFunction);

    public abstract Builder setUnusableByJsSuppressed(boolean isUnusableByJsSuppressed);

    public abstract Builder setEnclosingClassTypeDescriptor(
        TypeDescriptor enclosingClassTypeDescriptor);

    public abstract Builder setName(String name);

    public abstract Builder setReturnTypeDescriptor(TypeDescriptor returnTypeDescriptor);

    public abstract Builder setVisibility(Visibility visibility);

    public abstract Builder setJsInfo(JsInfo jsInfo);

    public abstract Builder setTypeParameterTypeDescriptors(
        Iterable<TypeDescriptor> typeParameterTypeDescriptors);

    public abstract Builder setParameterTypeDescriptors(TypeDescriptor... parameterTypeDescriptors);

    public abstract Builder setParameterTypeDescriptors(
        List<TypeDescriptor> parameterTypeDescriptors);

    public Builder addParameterTypeDescriptors(
        int index, TypeDescriptor... parameterTypeDescriptors) {
      return addParameterTypeDescriptors(index, Arrays.asList(parameterTypeDescriptors));
    }

    public Builder addParameterTypeDescriptors(
        int index, List<TypeDescriptor> parameterTypeDescriptors) {
      List<TypeDescriptor> newParameterTypeDescriptors =
          new ArrayList<>(getParameterTypeDescriptors());
      newParameterTypeDescriptors.addAll(index, parameterTypeDescriptors);
      return setParameterTypeDescriptors(newParameterTypeDescriptors);
    }

    public Builder addParameterTypeDescriptors(TypeDescriptor... parameterTypeDescriptors) {
      return addParameterTypeDescriptors(Arrays.asList(parameterTypeDescriptors));
    }

    public Builder addParameterTypeDescriptors(List<TypeDescriptor> parameterTypeDescriptors) {
      List<TypeDescriptor> newParameterTypeDescriptors =
          new ArrayList<>(getParameterTypeDescriptors());
      newParameterTypeDescriptors.addAll(parameterTypeDescriptors);
      return setParameterTypeDescriptors(newParameterTypeDescriptors);
    }

    abstract Builder setParameterOptionality(BitSet parameterOptionality);

    abstract BitSet getParameterOptionality();

    public Builder setParameterOptionality(int i, boolean isOptional) {
      BitSet parameterOptionality = getParameterOptionality();
      parameterOptionality.set(i, isOptional);
      return setParameterOptionality(parameterOptionality);
    }

    public Builder removeParameterOptionality() {
      return setParameterOptionality(new BitSet());
    }

    public Builder setDeclarationMethodDescriptor(MethodDescriptor declarationMethodDescriptor) {
      return setDeclarationMethodDescriptorOrNullIfSelf(declarationMethodDescriptor);
    }

    // Accessors to support validation, default construction and custom setters.
    abstract Builder setDeclarationMethodDescriptorOrNullIfSelf(
        MethodDescriptor declarationMethodDescriptor);

    abstract ImmutableList<TypeDescriptor> getParameterTypeDescriptors();

    abstract boolean isConstructor();

    abstract Optional<String> getName();

    abstract MethodDescriptor autoBuild();

    public MethodDescriptor build() {
      if (isConstructor()) {
        checkState(!getName().isPresent(), "Should not set names for constructors.");
        // Choose consistent naming for constructors.
        setName("<ctor>");
      }

      checkState(getName().isPresent());
      MethodDescriptor methodDescriptor = autoBuild();

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
      if (methodDescriptor != methodDescriptor.getDeclarationMethodDescriptor()) {
        List<TypeDescriptor> methodDeclarationParameterTypeDescriptors =
            methodDescriptor.getDeclarationMethodDescriptor().getParameterTypeDescriptors();
        checkArgument(
            methodDeclarationParameterTypeDescriptors.size()
                == methodDescriptor.getParameterTypeDescriptors().size(),
            "Method parameters (%s) for method %s don't match method declaration (%s)",
            methodDescriptor.getParameterTypeDescriptors(),
            methodDescriptor.getEnclosingClassTypeDescriptor().getSimpleSourceName()
                + "."
                + methodDescriptor.getName(),
            methodDeclarationParameterTypeDescriptors);
      }
      return interner.intern(methodDescriptor);
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

  public MethodDescriptor specializeTypeVariables(
      Map<TypeDescriptor, TypeDescriptor> applySpecializedTypeArgumentByTypeParameters) {
    if (applySpecializedTypeArgumentByTypeParameters.isEmpty()) {
      return this;
    }

    // Original type variables.
    TypeDescriptor returnTypeDescriptor = getReturnTypeDescriptor();
    ImmutableList<TypeDescriptor> parameterTypeDescriptors = getParameterTypeDescriptors();

    // Specialized type variables (possibly recursively).
    TypeDescriptor specializedReturnTypeDescriptor =
        returnTypeDescriptor.specializeTypeVariables(applySpecializedTypeArgumentByTypeParameters);
    ImmutableList<TypeDescriptor> specializedParameterTypeDescriptors =
        parameterTypeDescriptors
            .stream()
            .map(
                typeDescriptor ->
                    typeDescriptor.specializeTypeVariables(
                        applySpecializedTypeArgumentByTypeParameters))
            .collect(toImmutableList());

    return MethodDescriptor.Builder.from(this)
        .setReturnTypeDescriptor(specializedReturnTypeDescriptor)
        .setParameterTypeDescriptors(specializedParameterTypeDescriptors)
        .build();
  }
}
