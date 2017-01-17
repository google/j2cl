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
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.J2clUtils;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

  public abstract boolean isConstructor();

  public abstract boolean isVarargs();

  public abstract boolean isDefault();

  public abstract boolean isSynthetic();

  public abstract ImmutableList<TypeDescriptor> getParameterTypeDescriptors();

  public abstract TypeDescriptor getReturnTypeDescriptor();

  /** Type parameters declared in the method. */
  public abstract ImmutableList<TypeDescriptor> getTypeParameterTypeDescriptors();

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

  public boolean isJsPropertyGetter() {
    return getJsInfo().getJsMemberType() == JsMemberType.GETTER;
  }

  public boolean isJsPropertySetter() {
    return getJsInfo().getJsMemberType() == JsMemberType.SETTER;
  }

  public boolean isJsMethod() {
    return getJsInfo().getJsMemberType() == JsMemberType.METHOD;
  }

  public boolean isJsFunction() {
    return getJsInfo().getJsMemberType() == JsMemberType.JS_FUNCTION;
  }

  public boolean isJsMember() {
    return getJsInfo().getJsMemberType() != JsMemberType.NONE;
  }

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

  public boolean isJsConstructor() {
    return getJsInfo().getJsMemberType() == JsMemberType.CONSTRUCTOR;
  }

  @Override
  public boolean isPolymorphic() {
    return !isStatic() && !isConstructor();
  }

  public abstract boolean isAbstract();

  public boolean isOrOverridesJsMember() {
    return isJsMember() || !getOverriddenJsMembers().isEmpty();
  }

  /**
   * Two methods are parameter erasure equal if the erasure of their parameters' types are equal.
   * Parameter erasure equal means that they are overriding signature equal, which means that they
   * are real overriding/overridden or accidental overriding/overridden.
   */
  public boolean overridesSignature(MethodDescriptor that) {
    if (this.isStatic() || that.isStatic()) {
      return false;
    }

    return this != that && this.getOverrideSignature().equals(that.getOverrideSignature());
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
        .setDefault(false)
        .setNative(false)
        .setStatic(false)
        .setVarargs(false)
        .setFinal(false)
        .setSynthetic(false)
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
    return J2clUtils.format(
        "%s%s.%s(%s)",
        isConstructor() ? "" : getReturnTypeDescriptor().getReadableDescription() + " ",
        getEnclosingClassTypeDescriptor().getReadableDescription(),
        isConstructor() ? getEnclosingClassTypeDescriptor().getReadableDescription() : getName(),
        getDeclarationMethodDescriptor()
            .getParameterTypeDescriptors()
            .stream()
            .map(type -> type.getRawTypeDescriptor().getReadableDescription())
            .collect(joining(", ")));
  }

  /** Returns a signature suitable for override checking. */
  private String getOverrideSignature() {
    StringBuilder signatureBuilder = new StringBuilder("");
    Visibility methodVisibility = getVisibility();
    if (methodVisibility.isPackagePrivate()) {
      signatureBuilder.append(":pp:");
      signatureBuilder.append(getEnclosingClassTypeDescriptor().getPackageName());
      signatureBuilder.append(":");
    } else if (methodVisibility.isPrivate()) {
      signatureBuilder.append(":p:");
      signatureBuilder.append(getEnclosingClassTypeDescriptor().getQualifiedBinaryName());
      signatureBuilder.append(":");
    }

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
  public Set<MethodDescriptor> getOverriddenMethodDescriptors() {
    return getEnclosingClassTypeDescriptor().getOverriddenMethodDescriptors(this);
  }

  /** A Builder for MethodDescriptors. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setDefault(boolean isDefault);

    public abstract Builder setNative(boolean isNative);

    public abstract Builder setStatic(boolean isStatic);

    public abstract Builder setVarargs(boolean isVarargs);

    public abstract Builder setConstructor(boolean isConstructor);

    public abstract Builder setAbstract(boolean isAbstract);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setSynthetic(boolean isSynthetic);

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

      MethodDescriptor methodDescriptor = autoBuild();

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
}
