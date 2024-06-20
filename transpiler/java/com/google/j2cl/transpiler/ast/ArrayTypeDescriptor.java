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

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.ThreadLocalInterner;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;

/** An array type. */
@Visitable
@AutoValue
public abstract class ArrayTypeDescriptor extends TypeDescriptor {

  public abstract TypeDescriptor getComponentTypeDescriptor();

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

  @Override
  public boolean isSameBaseType(TypeDescriptor other) {
    if (!(other instanceof ArrayTypeDescriptor)) {
      return false;
    }
    ArrayTypeDescriptor otherArrayType = (ArrayTypeDescriptor) other;
    return getDimensions() == otherArrayType.getDimensions()
        && getLeafTypeDescriptor().isSameBaseType(otherArrayType.getLeafTypeDescriptor());
  }

  /** Returns true for arrays where raw JavaScript array representation is enough. */
  public boolean isUntypedArray() {
    if (getLeafTypeDescriptor().isPrimitive()) {
      return false;
    }
    TypeDescriptor rawLeafTypeDescriptor = getLeafTypeDescriptor().toRawTypeDescriptor();
    return rawLeafTypeDescriptor.isNative()
        || (TypeDescriptors.isJavaLangObject(rawLeafTypeDescriptor) && getDimensions() == 1);
  }

  /** Returns true if the leaf type is native. */
  @Override
  public boolean isNativeJsArray() {
    return getLeafTypeDescriptor().toRawTypeDescriptor().isNative();
  }

  /**
   * Returns true for arrays where raw wasm array representation is enough. These arrays are located
   * in {@see javaemul.internal.WasmArrays}.
   */
  public abstract boolean isMarkedAsNativeWasmArray();

  @Override
  public boolean isNativeWasmArray() {
    if (isMarkedAsNativeWasmArray()) {
      return true;
    }

    if (getComponentTypeDescriptor().toRawTypeDescriptor() instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor componentTypeDescriptor =
          (DeclaredTypeDescriptor) getComponentTypeDescriptor().toRawTypeDescriptor();
      return componentTypeDescriptor.getTypeDeclaration().getWasmInfo() != null;
    }

    return false;
  }

  @Override
  public abstract boolean isNullable();

  @Override
  @Memoized
  public ArrayTypeDescriptor toRawTypeDescriptor() {
    return toBuilder()
        .setComponentTypeDescriptor(getComponentTypeDescriptor().toRawTypeDescriptor())
        .setNullable(true)
        .build();
  }

  @Override
  @Nullable
  public TypeDeclaration getMetadataTypeDeclaration() {
    return null;
  }

  @Override
  @Nullable
  public MethodDescriptor getMethodDescriptor(String methodName, TypeDescriptor... parameters) {
    return TypeDescriptors.get().javaLangObject.getMethodDescriptor(methodName, parameters);
  }

  @Override
  public boolean isArray() {
    return true;
  }

  @Override
  public boolean isPrimitiveArray() {
    return getComponentTypeDescriptor().isPrimitive();
  }

  @Override
  @Memoized
  public ArrayTypeDescriptor toUnparameterizedTypeDescriptor() {
    return toBuilder()
        .setComponentTypeDescriptor(getComponentTypeDescriptor().toUnparameterizedTypeDescriptor())
        .setNullable(true)
        .build();
  }

  @Override
  public boolean isAssignableTo(TypeDescriptor that) {
    ArrayTypeDescriptor thisRawTypeDescriptor = toRawTypeDescriptor();
    TypeDescriptor thatRawTypeDescriptor = that.toRawTypeDescriptor();
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
  public Set<TypeVariable> getAllTypeVariables() {
    return getLeafTypeDescriptor().getAllTypeVariables();
  }

  @Override
  @Memoized
  public String getUniqueId() {
    String prefix = isNullable() ? "?" : "!";
    String suffix = isNativeWasmArray() ? "(native)" : "";
    return prefix + "[]" + getComponentTypeDescriptor().getUniqueId() + suffix;
  }

  @Override
  @Memoized
  public String getMangledName() {
    return "arrayOf_".repeat(getDimensions()) + getLeafTypeDescriptor().getMangledName();
  }

  @Override
  @Memoized
  public String getReadableDescription() {
    return synthesizeArrayName(getLeafTypeDescriptor().getReadableDescription());
  }

  private String synthesizeArrayName(String leafName) {
    return leafName + "[]".repeat(getDimensions());
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
  public ArrayTypeDescriptor toNonNullable() {
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
  TypeDescriptor replaceInternalTypeDescriptors(TypeReplacer fn, ImmutableSet<TypeVariable> seen) {
    TypeDescriptor component = getComponentTypeDescriptor();
    TypeDescriptor newComponent = replaceTypeDescriptors(component, fn, seen);
    if (component != newComponent) {
      return Builder.from(this).setComponentTypeDescriptor(newComponent).build();
    }
    return this;
  }

  @Override
  ArrayTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable,
      ImmutableSet<TypeVariable> seen) {
    if (AstUtils.isIdentityFunction(replacementTypeArgumentByTypeVariable)) {
      return this;
    }
    return toBuilder()
        .setComponentTypeDescriptor(
            getComponentTypeDescriptor()
                .specializeTypeVariables(replacementTypeArgumentByTypeVariable, seen))
        .build();
  }

  @Override
  public ArrayTypeDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacementTypeArgumentByTypeVariable) {
    return specializeTypeVariables(replacementTypeArgumentByTypeVariable, ImmutableSet.of());
  }

  @Override
  boolean isDenotable(ImmutableSet<TypeVariable> seen) {
    return getComponentTypeDescriptor().isDenotable(seen);
  }

  @Override
  boolean hasReferenceTo(TypeVariable typeVariable, ImmutableSet<TypeVariable> seen) {
    return getComponentTypeDescriptor().hasReferenceTo(typeVariable, seen);
  }

  @Override
  TypeDescriptor acceptInternal(Processor processor) {
    return Visitor_ArrayTypeDescriptor.visit(processor, this);
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_ArrayTypeDescriptor.Builder()
        // Default values.
        .setNullable(true)
        .setMarkedAsNativeWasmArray(false);
  }

  /** Builder for an ArrayTypeDescriptor. */
  @AutoValue.Builder
  public abstract static class Builder {
    public static Builder from(ArrayTypeDescriptor arrayTypeDescriptor) {
      return arrayTypeDescriptor.toBuilder();
    }

    public abstract Builder setComponentTypeDescriptor(TypeDescriptor leafTypeDescriptor);

    public abstract Builder setNullable(boolean isNullable);

    public abstract Builder setMarkedAsNativeWasmArray(boolean wasmNative);

    abstract ArrayTypeDescriptor autoBuild();

    private static final ThreadLocalInterner<ArrayTypeDescriptor> interner =
        new ThreadLocalInterner<>();

    public ArrayTypeDescriptor build() {
      ArrayTypeDescriptor typeDescriptor = autoBuild();
      return interner.intern(typeDescriptor);
    }
  }
}
