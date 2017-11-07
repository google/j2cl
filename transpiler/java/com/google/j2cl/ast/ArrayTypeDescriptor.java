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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

/** An array type. */
@AutoValue
@Visitable
public abstract class ArrayTypeDescriptor extends TypeDescriptor {

  @Nullable
  public abstract TypeDescriptor getComponentTypeDescriptor();

  @Override
  @Memoized
  public ImmutableList<String> getClassComponents() {
    return ImmutableList.copyOf(
        AstUtils.synthesizeClassComponents(
            getComponentTypeDescriptor(), simpleName -> simpleName + "[]"));
  }

  @Memoized
  public TypeDescriptor getLeafTypeDescriptor() {
    TypeDescriptor componentTypeDescriptor = getComponentTypeDescriptor();

    if (componentTypeDescriptor.isArray()) {
      return ((ArrayTypeDescriptor) componentTypeDescriptor).getLeafTypeDescriptor();
    }

    return componentTypeDescriptor;
  }

  @Memoized
  public int getDimensions() {
    TypeDescriptor componentTypeDescriptor = getComponentTypeDescriptor();

    if (componentTypeDescriptor.isArray()) {
      return ((ArrayTypeDescriptor) componentTypeDescriptor).getDimensions() + 1;
    }

    return 1;
  }

  /** Returns true for arrays where raw JavaScript array representation is enough. */
  public boolean isUntypedArray() {
    if (getLeafTypeDescriptor().isPrimitive()) {
      return false;
    }
    DeclaredTypeDescriptor leafTypeDescriptor = (DeclaredTypeDescriptor) getLeafTypeDescriptor();
    return leafTypeDescriptor.isNative()
        || (TypeDescriptors.isJavaLangObject(getLeafTypeDescriptor()) && getDimensions() == 1);
  }

  @Override
  public abstract boolean isNullable();

  @Override
  @Memoized
  public ArrayTypeDescriptor getRawTypeDescriptor() {
    return toBuilder()
        .setComponentTypeDescriptor(getComponentTypeDescriptor().getRawTypeDescriptor())
        .setNullable(true)
        .build();
  }

  @Override
  @Nullable
  public DeclaredTypeDescriptor getMetadataTypeDescriptor() {
    return null;
  }

  @Override
  public boolean isArray() {
    return true;
  }

  @Override
  @Memoized
  public ArrayTypeDescriptor unparameterizedTypeDescriptor() {
    return toBuilder()
        .setComponentTypeDescriptor(getComponentTypeDescriptor().unparameterizedTypeDescriptor())
        .setNullable(true)
        .build();
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    ArrayTypeDescriptor thisRawTypeDescriptor = getRawTypeDescriptor();
    TypeDescriptor thatRawTypeDescriptor = that.getRawTypeDescriptor();
    if (!thatRawTypeDescriptor.isArray()) {
      return TypeDescriptors.isJavaLangObject(thatRawTypeDescriptor)
          || TypeDescriptors.isJavaIoSerializable(thatRawTypeDescriptor)
          || TypeDescriptors.isJavaLangCloneable(thatRawTypeDescriptor);
    }
    ArrayTypeDescriptor thatArrayTypeDescriptor = (ArrayTypeDescriptor) thatRawTypeDescriptor;
    return thisRawTypeDescriptor
        .getComponentTypeDescriptor()
        .isAssignableTo(thatArrayTypeDescriptor.getComponentTypeDescriptor());
  }

  @Override
  public Set<TypeDescriptor> getAllTypeVariables() {
    return getLeafTypeDescriptor().getAllTypeVariables();
  }

  @Override
  public String getUniqueId() {
    String prefix = isNullable() ? "?" : "!";
    return prefix + Strings.repeat("[]", getDimensions()) + getLeafTypeDescriptor().getUniqueId();
  }

  @Override
  public String getReadableDescription() {
    return getSimpleSourceName();
  }

  @Override
  @Memoized
  public TypeDescriptor toNullable() {
    if (isNullable()) {
      return this;
    }
    return toBuilder().setNullable(true).build();
  }

  @Override
  public TypeDescriptor toNonNullable() {
    if (!isNullable()) {
      return this;
    }
    return toBuilder().setNullable(false).build();
  }

  @Override
  public boolean canBeReferencedExternally() {
    // TODO(b/36363419): Decide what to do with arrays like String[], int[], JsFunction[], etc
    // which as of now do not give warnings.
    return getLeafTypeDescriptor().canBeReferencedExternally();
  }

  @Override
  public Map<TypeDescriptor, TypeDescriptor> getSpecializedTypeArgumentByTypeParameters() {
    return getLeafTypeDescriptor().getSpecializedTypeArgumentByTypeParameters();
  }

  @Override
  public ArrayTypeDescriptor specializeTypeVariables(
      Function<TypeDescriptor, TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    if (replacementTypeArgumentByTypeVariable == Function.<TypeDescriptor>identity()) {
      return this;
    }
    return toBuilder()
        .setComponentTypeDescriptor(
            getComponentTypeDescriptor()
                .specializeTypeVariables(replacementTypeArgumentByTypeVariable))
        .build();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_ArrayTypeDescriptor.visit(processor, this);
  }

  @Override
  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_ArrayTypeDescriptor.Builder()
        // Default values.
        .setNullable(true);
  }

  /** Builder for an ArrayTypeDescriptor. */
  @AutoValue.Builder
  public abstract static class Builder extends TypeDescriptor.Builder {

    public abstract Builder setComponentTypeDescriptor(TypeDescriptor leafTypeDescriptor);

    public abstract Builder setNullable(boolean isNullable);

    abstract ArrayTypeDescriptor autoBuild();

    private static final ThreadLocalInterner<ArrayTypeDescriptor> interner =
        new ThreadLocalInterner<>();

    public ArrayTypeDescriptor build() {
      ArrayTypeDescriptor typeDescriptor = autoBuild();
      return interner.intern(typeDescriptor);
    }
  }
}
