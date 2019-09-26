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

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.processors.common.Processor;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A definition-site or usage-site reference to a type variable.
 *
 * <p>Type variables are used to model both named variables and unnamed variables such as wildcards
 * and captures.
 *
 * <p>Some properties are lazily calculated since type relationships are cyclic graphs and the
 * TypeVariable class is a value type. Those properties are set through {@code Supplier}.
 */
@AutoValue
@Visitable
public abstract class TypeVariable extends TypeDescriptor implements HasName {

  public abstract String getName();

  @Memoized
  public TypeDescriptor getBoundTypeDescriptor() {
    TypeDescriptor boundTypeDescriptor = getBoundTypeDescriptorSupplier().get();
    return boundTypeDescriptor != null ? boundTypeDescriptor : TypeDescriptors.get().javaLangObject;
  }

  public abstract Supplier<TypeDescriptor> getBoundTypeDescriptorSupplier();

  @Nullable
  abstract String getUniqueKey();

  @Override
  public boolean isTypeVariable() {
    return true;
  }

  @Override
  public boolean isNullable() {
    // TODO(b/68726480): Implement nullability of type variables.
    return true;
  }

  @Override
  public TypeDescriptor toNullable() {
    // TODO(b/68726480): Implement nullability of type variables.
    return this;
  }

  @Override
  public TypeDescriptor toNonNullable() {
    // TODO(b/68726480): Implement nullability of type variables.
    return this;
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    return this.getBoundTypeDescriptor().isAssignableTo(that);
  }

  /** Return true if it is an unnamed type variable, i.e. a wildcard or capture. */
  public abstract boolean isWildcardOrCapture();

  @Override
  public boolean isNoopCast() {
    return toRawTypeDescriptor().isNoopCast();
  }

  @Nullable
  @Override
  public TypeDeclaration getMetadataTypeDeclaration() {
    return getBoundTypeDescriptor().getMetadataTypeDeclaration();
  }

  @Override
  public TypeDescriptor toRawTypeDescriptor() {
    return getBoundTypeDescriptor().toRawTypeDescriptor();
  }

  @Override
  public PrimitiveTypeDescriptor toUnboxedType() {
    return toRawTypeDescriptor().toUnboxedType();
  }

  @Override
  public TypeDescriptor toUnparameterizedTypeDescriptor() {
    return this;
  }

  @Override
  public boolean canBeReferencedExternally() {
    return toRawTypeDescriptor().canBeReferencedExternally();
  }

  @Override
  public Map<TypeVariable, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    return getBoundTypeDescriptor().getSpecializedTypeArgumentByTypeParameters();
  }

  @Override
  public Set<TypeVariable> getAllTypeVariables() {
    if (!isWildcardOrCapture()) {
      return ImmutableSet.of(this);
    }
    return ImmutableSet.of();
  }

  @Override
  public TypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return replacementTypeArgumentByTypeVariable.apply(this);
  }

  @Override
  public String getReadableDescription() {
    return getName();
  }

  @Override
  public String getUniqueId() {
    String prefix = isNullable() ? "?" : "!";
    return prefix + getUniqueKey();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TypeVariable.visit(processor, this);
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_TypeVariable.Builder().setWildcardOrCapture(false);
  }

  /** Creates a wildcard type variable with a specific bound. */
  public static TypeVariable createWildcardWithBound(TypeDescriptor bound) {
    return TypeVariable.newBuilder()
        .setWildcardOrCapture(true)
        .setBoundTypeDescriptorSupplier(() -> bound)
        // Create an unique key that does not conflict with the keys used for other types nor for
        // type variables coming from JDT, which follow "<declaring_type>:<name>...".
        // {@see org.eclipse.jdt.core.BindingKey}.
        .setUniqueKey("<??>" + bound.getUniqueId())
        .setName("?")
        .build();
  }

  /** Builder for a TypeVariableDeclaration. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setBoundTypeDescriptorSupplier(
        Supplier<TypeDescriptor> boundTypeDescriptorFactory);

    public abstract Builder setUniqueKey(String uniqueKey);

    public abstract Builder setName(String name);

    public abstract Builder setWildcardOrCapture(boolean isWildcardOrCapture);

    private static final ThreadLocalInterner<TypeVariable> interner = new ThreadLocalInterner<>();

    abstract TypeVariable autoBuild();

    public TypeVariable build() {
      TypeVariable typeVariable = autoBuild();
      return interner.intern(typeVariable);
    }

    public static Builder from(TypeVariable typeVariable) {
      return typeVariable.toBuilder();
    }
  }
}
