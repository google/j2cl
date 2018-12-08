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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.ast.annotations.Visitable;
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
public abstract class TypeVariable extends TypeDescriptor {

  @Override
  public abstract ImmutableList<String> getClassComponents();

  public DeclaredTypeDescriptor getEnclosingTypeDescriptor() {
    return getEnclosingTypeDescriptorSupplier().get();
  }

  public abstract Supplier<DeclaredTypeDescriptor> getEnclosingTypeDescriptorSupplier();

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
    // TODO(b/114074816): Remove this hack when modeling of type variables is improved and the name
    // is actually the source name of the variable and does not encode extra information.
    int lastUnderscore = getSimpleSourceName().lastIndexOf("_");
    return getSimpleSourceName().substring(lastUnderscore + 1);
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

  /** Builder for a TypeVariableDeclaration. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setEnclosingTypeDescriptorSupplier(
        Supplier<DeclaredTypeDescriptor> enclosingTypeDescriptorSupplier);

    public abstract Builder setBoundTypeDescriptorSupplier(
        Supplier<TypeDescriptor> boundTypeDescriptorFactory);

    public abstract Builder setUniqueKey(String uniqueKey);

    public abstract Builder setClassComponents(Iterable<String> name);

    public abstract Builder setWildcardOrCapture(boolean isWildcardOrCapture);

    private static final ThreadLocalInterner<TypeVariable> interner = new ThreadLocalInterner<>();

    abstract TypeVariable autoBuild();

    public TypeVariable build() {
      TypeVariable typeDeclaration = autoBuild();
      return interner.intern(typeDeclaration);
    }

    public static Builder from(TypeVariable typeVariable) {
      return typeVariable.toBuilder();
    }
  }
}
