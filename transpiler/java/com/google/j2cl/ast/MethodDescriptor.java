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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.Interner;
import com.google.j2cl.common.J2clUtils;
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

  @Override
  public abstract boolean isStatic();

  public abstract Visibility getVisibility();

  @Override
  public abstract TypeDescriptor getEnclosingClassTypeDescriptor();

  @Override
  public abstract String getName();

  public abstract boolean isConstructor();

  @Override
  public abstract boolean isNative();

  public abstract boolean isVarargs();

  public abstract boolean isDefault();

  @Nullable
  abstract MethodDescriptor getDeclarationMethodDescriptorOrNull();

  public abstract ImmutableList<TypeDescriptor> getParameterTypeDescriptors();

  public abstract TypeDescriptor getReturnTypeDescriptor();

  /**
   * Type parameters declared in the method.
   */
  public abstract ImmutableList<TypeDescriptor> getTypeParameterTypeDescriptors();

  @Override
  public abstract JsInfo getJsInfo();

  public boolean isInit() {
    return getName().equals(INIT_METHOD_NAME) && !isStatic();
  }

  /**
   * Returns the descriptor of the method declaration or this instance if this is already a method
   * declaration or there is no method declaration. Method declarations descriptors describe the the
   * method at the declaration place, which might be different to the descriptor at the usage place
   * due to generic type variable instantiations. For example,
   *
   * <p>
   *
   * <pre>
   *   class A<T> {
   *     T m();  // method descriptor here has a return type T
   *   }
   *
   *   A<String> a =....
   *   a.m() // the method descriptor here has a return type String and its declaration is the
   *         // one above.
   * </pre>
   */
  public MethodDescriptor getDeclarationMethodDescriptor() {
    return getDeclarationMethodDescriptorOrNull() == null
        ? this
        : getDeclarationMethodDescriptorOrNull();
  }

  public boolean isJsPropertyGetter() {
    return getJsInfo().getJsMemberType() == JsMemberType.GETTER;
  }

  public boolean isJsPropertySetter() {
    return getJsInfo().getJsMemberType() == JsMemberType.SETTER;
  }

  public boolean isJsMethod() {
    return getJsInfo().getJsMemberType() == JsMemberType.METHOD;
  }

  public boolean isJsOverlay() {
    return getJsInfo().isJsOverlay();
  }

  public boolean isJsFunction() {
    return getJsInfo().getJsMemberType() == JsMemberType.JS_FUNCTION;
  }

  /**
   * Returns true if it is a vararg method that can be referenced by JavaScript side. A
   * non-JsOverlay JsMethod, and a JsFunction can be referenced by JavaScript side.
   *
   * TODO: In our AST model, isJsMethod() and isJsOverlay() is NOT mutually-exclusive. We may want
   * to re-examine it after we import JsInteropRestrictionChecker and do refactoring on the AST.
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

  public String getMethodSignature() {
    String name = getName();
    ImmutableList<TypeDescriptor> parameterTypeDescriptors = getParameterTypeDescriptors();
    return MethodDescriptors.getSignature(name, parameterTypeDescriptors);
  }

  public static Builder newBuilder() {
    return new Builder();
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
        getName(),
        getDeclarationMethodDescriptor()
            .getParameterTypeDescriptors()
            .stream()
            .map(type -> type.getRawTypeDescriptor().getReadableDescription())
            .collect(joining(", ")));
  }

  /** A Builder for MethodDescriptors. */
  public static class Builder {
    private boolean isStatic;
    private Visibility visibility = Visibility.PUBLIC;
    private TypeDescriptor enclosingClassTypeDescriptor;
    private String name;
    private boolean isConstructor;
    private boolean isNative;
    private boolean isDefault;
    private boolean isVarargs;
    private MethodDescriptor declarationMethodDescriptor;
    private ImmutableList<TypeDescriptor> parameterTypeDescriptors = ImmutableList.of();
    private TypeDescriptor returnTypeDescriptor = TypeDescriptors.get().primitiveVoid;
    private ImmutableList<TypeDescriptor> typeParameterTypeDescriptors = ImmutableList.of();
    private JsInfo jsInfo = JsInfo.NONE;
    private boolean isAbstract;

    public static Builder from(MethodDescriptor methodDescriptor) {
      Builder builder = new Builder();
      builder.isStatic = methodDescriptor.isStatic();
      builder.visibility = methodDescriptor.getVisibility();
      builder.enclosingClassTypeDescriptor = methodDescriptor.getEnclosingClassTypeDescriptor();
      builder.name = methodDescriptor.isConstructor() ? null : methodDescriptor.getName();
      builder.isConstructor = methodDescriptor.isConstructor();
      builder.isNative = methodDescriptor.isNative();
      builder.isDefault = methodDescriptor.isDefault();
      builder.isVarargs = methodDescriptor.isVarargs();
      if (methodDescriptor.getDeclarationMethodDescriptor() != methodDescriptor) {
        builder.declarationMethodDescriptor = methodDescriptor.getDeclarationMethodDescriptor();
      }
      builder.parameterTypeDescriptors = methodDescriptor.getParameterTypeDescriptors();
      builder.returnTypeDescriptor = methodDescriptor.getReturnTypeDescriptor();
      builder.typeParameterTypeDescriptors = methodDescriptor.getTypeParameterTypeDescriptors();
      builder.jsInfo = methodDescriptor.getJsInfo();
      return builder;
    }

    public Builder setEnclosingClassTypeDescriptor(TypeDescriptor enclosingClassTypeDescriptor) {
      this.enclosingClassTypeDescriptor = enclosingClassTypeDescriptor;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setDeclarationMethodDescriptor(MethodDescriptor declarationMethodDescriptor) {
      this.declarationMethodDescriptor = declarationMethodDescriptor;
      return this;
    }

    public Builder setReturnTypeDescriptor(TypeDescriptor returnTypeDescriptor) {
      this.returnTypeDescriptor = returnTypeDescriptor;
      return this;
    }

    public Builder setParameterTypeDescriptors(TypeDescriptor... typeDescriptors) {
      this.parameterTypeDescriptors = ImmutableList.copyOf(typeDescriptors);
      return this;
    }

    public Builder setParameterTypeDescriptors(Iterable<TypeDescriptor> parameterTypeDescriptors) {
      this.parameterTypeDescriptors = ImmutableList.copyOf(parameterTypeDescriptors);
      return this;
    }

    public Builder setIsConstructor(boolean isConstructor) {
      this.isConstructor = isConstructor;
      return this;
    }

    public Builder setIsDefault(boolean isDefault) {
      this.isDefault = isDefault;
      return this;
    }

    public Builder setIsNative(boolean isNative) {
      this.isNative = isNative;
      return this;
    }

    public Builder setIsStatic(boolean isStatic) {
      this.isStatic = isStatic;
      return this;
    }

    public Builder setIsVarargs(boolean isVarargs) {
      this.isVarargs = isVarargs;
      return this;
    }

    public Builder setTypeParameterTypeDescriptors(
        Iterable<TypeDescriptor> typeParameterTypeDescriptors) {
      this.typeParameterTypeDescriptors = ImmutableList.copyOf(typeParameterTypeDescriptors);
      return this;
    }

    public Builder setVisibility(Visibility visibility) {
      this.visibility = visibility;
      return this;
    }

    public Builder setJsInfo(JsInfo jsInfo) {
      this.jsInfo = jsInfo;
      return this;
    }

    public Builder addParameter(TypeDescriptor parameterTypeDescriptor) {
      return setParameterTypeDescriptors(
          Iterables.concat(
              this.parameterTypeDescriptors, Lists.newArrayList(parameterTypeDescriptor)));
    }

    public Builder setIsAbstract(boolean isAbstract) {
      this.isAbstract = isAbstract;
      return this;
    }

    private static final ThreadLocal<Interner<MethodDescriptor>> interner = new ThreadLocal<>();

    private static Interner<MethodDescriptor> getInterner() {
      if (interner.get() == null) {
        interner.set(new Interner<>());
      }
      return interner.get();
    }

    public MethodDescriptor build() {
      // TODO: We should compute the constructor name instead of allowing empty method name to
      // percolate into the MethodDescriptor.
      checkState(!isConstructor || name == null, "Should not set names for constructors.");
      checkState(isConstructor || name != null, "Method name should not be null.");

      if (isConstructor) {
        // Choose consistent naming for constructors.
        name = enclosingClassTypeDescriptor.getSimpleSourceName();
      }

      if (declarationMethodDescriptor != null) {
        ImmutableList<TypeDescriptor> methodDeclarationParameterTypeDescriptors =
            declarationMethodDescriptor.getParameterTypeDescriptors();
        checkArgument(
            methodDeclarationParameterTypeDescriptors.size() == parameterTypeDescriptors.size(),
            "Method parameters (%s) don't match with method declaration (%s)",
            parameterTypeDescriptors,
            methodDeclarationParameterTypeDescriptors);
      }

      return getInterner()
          .intern(
              new AutoValue_MethodDescriptor(
                  isStatic,
                  visibility,
                  enclosingClassTypeDescriptor,
                  name,
                  isConstructor,
                  isNative,
                  isVarargs,
                  isDefault,
                  declarationMethodDescriptor,
                  ImmutableList.copyOf(parameterTypeDescriptors),
                  returnTypeDescriptor,
                  ImmutableList.copyOf(typeParameterTypeDescriptors),
                  jsInfo,
                  isAbstract));
    }
  }
}
