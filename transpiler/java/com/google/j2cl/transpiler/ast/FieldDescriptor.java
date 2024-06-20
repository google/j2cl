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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.ThreadLocalInterner;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

/** A (by signature) reference to a field. */
@Visitable
@AutoValue
public abstract class FieldDescriptor extends MemberDescriptor {

  // TODO(b/317164851): Remove hack that makes jsinfo ignored for non-native types in Wasm.
  private static final ThreadLocal<Boolean> ignoreNonNativeJsInfo =
      ThreadLocal.withInitial(() -> false);

  public static void setIgnoreNonNativeJsInfo() {
    ignoreNonNativeJsInfo.set(true);
  }

  public abstract TypeDescriptor getTypeDescriptor();

  @Override
  public abstract boolean isCompileTimeConstant();

  @Nullable
  public abstract Literal getConstantValue();

  @Override
  public abstract boolean isEnumConstant();

  @Override
  public abstract boolean isDeprecated();

  @Override
  public abstract FieldOrigin getOrigin();

  /** Whether this field originates in the source code or is synthetic. */
  public enum FieldOrigin implements MemberDescriptor.Origin {
    SOURCE,
    SYNTHETIC_OUTER_FIELD,
    SYNTHETIC_CAPTURE_FIELD,
    SYNTHETIC_BACKING_FIELD,
    SYNTHETIC_ORDINAL_FIELD,
    SYNTHETIC_INSTANCE_OF_SUPPORT_FIELD,
    ;

    @Override
    public String getPrefix() {
      switch (this) {
          // User written methods and bridges need to be mangled the same way.
        case SOURCE:
          return "f_";
          // Don't prefix the rest, they all start with "$"
        default:
          return "";
      }
    }

    @Override
    public boolean isSyntheticInstanceOfSupportMember() {
      return this == SYNTHETIC_INSTANCE_OF_SUPPORT_FIELD;
    }
  }

  /**
   * Returns the descriptor of the field declaration. A field descriptor might describe a
   * specialized version of a field, e.g.
   *
   * <p>
   *
   * <pre>{@code
   *   class A<T> {
   *     T f;  // Field declaration described as a field "A.f" with type "T".
   *   }
   *
   *   // Field access with field descriptor for field "A.f" with type "String" that has a
   *   // declaration descriptor for field "A.f" but with type "T". Note that both descriptors refer
   *   // to the same field "A.f".
   *   new A<String>().f;
   * <p>
   * }</pre>
   */
  @Override
  public FieldDescriptor getDeclarationDescriptor() {
    return getDeclarationDescriptorOrNullIfSelf() == null
        ? this
        : getDeclarationDescriptorOrNullIfSelf();
  }

  @Nullable
  // A field declaration can be itself but AutoValue does not allow for a property to be a
  // reference to the value object being created, so we use a backing nullable property where null
  // encodes a self reference for AutoValue purposes and provide the accessor above to hide
  // the details.
  abstract FieldDescriptor getDeclarationDescriptorOrNullIfSelf();

  @Override
  @Memoized
  public FieldDescriptor toRawMemberDescriptor() {
    return toBuilder()
        .setEnclosingTypeDescriptor(getEnclosingTypeDescriptor().toRawTypeDescriptor())
        .setTypeDescriptor(getTypeDescriptor().toRawTypeDescriptor())
        .build();
  }

  @Override
  public boolean isNative() {
    return getEnclosingTypeDescriptor().isNative() && !isJsOverlay();
  }

  public boolean isCapture() {
    return getOrigin() == FieldOrigin.SYNTHETIC_CAPTURE_FIELD
        || getOrigin() == FieldOrigin.SYNTHETIC_OUTER_FIELD;
  }

  @Override
  public boolean isJsProperty() {
    return getJsInfo().getJsMemberType() == JsMemberType.PROPERTY;
  }

  @Override
  public boolean isInstanceMember() {
    return !isStatic();
  }

  @Override
  public boolean isJsFunction() {
    return false;
  }

  @Override
  public boolean isField() {
    return true;
  }

  @Override
  public JsInfo getJsInfo() {
    return getOriginalJsInfo();
  }

  @Override
  public KtInfo getKtInfo() {
    return getOriginalKtInfo();
  }

  @Override
  @Memoized
  public String getBinaryName() {
    return getName();
  }

  @Memoized
  @Override
  public String getMangledName() {
    return computePropertyMangledName();
  }

  @Memoized
  public Literal getEnumOrdinalValue() {
    checkState(isEnumConstant());
    if (!isDeclaration()) {
      return getDeclarationDescriptor().getEnumOrdinalValue();
    }

    return checkNotNull(
        getEnclosingTypeDescriptor()
            .getTypeDeclaration()
            .getOrdinalValueByEnumFieldName()
            .get(getName()));
  }

  @Override
  public FieldDescriptor specializeTypeVariables(
      Map<TypeVariable, TypeDescriptor> applySpecializedTypeArgumentByTypeParameters) {
    return specializeTypeVariables(
        TypeDescriptors.mappingFunctionFromMap(applySpecializedTypeArgumentByTypeParameters));
  }

  @Override
  public FieldDescriptor specializeTypeVariables(
      Function<TypeVariable, ? extends TypeDescriptor> replacingTypeDescriptorByTypeVariable) {
    if (AstUtils.isIdentityFunction(replacingTypeDescriptorByTypeVariable)) {
      return this;
    }

    return FieldDescriptor.Builder.from(this)
        .setTypeDescriptor(
            getTypeDescriptor().specializeTypeVariables(replacingTypeDescriptorByTypeVariable))
        .build();
  }

  public FieldDescriptor transform(Consumer<? super Builder> transformer) {
    Builder builder = toBuilder();
    if (!isDeclaration()) {
      builder.setDeclarationDescriptor(getDeclarationDescriptor().transform(transformer));
    }
    transformer.accept(builder);
    return builder.build();
  }

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_FieldDescriptor.Builder()
        // Default values.
        .setVisibility(Visibility.PUBLIC)
        .setOriginalJsInfo(JsInfo.NONE)
        .setOriginalKtInfo(KtInfo.NONE)
        .setCompileTimeConstant(false)
        .setStatic(false)
        .setFinal(false)
        .setSynthetic(false)
        .setUnusableByJsSuppressed(false)
        .setDeprecated(false)
        .setEnumConstant(false)
        .setOrigin(FieldOrigin.SOURCE);
  }

  /** Returns a description that is useful for error messages. */
  @Override
  public String getReadableDescription() {
    return String.format("%s.%s", getEnclosingTypeDescriptor().getReadableDescription(), getName());
  }

  @Override
  MemberDescriptor acceptInternal(Processor processor) {
    return Visitor_FieldDescriptor.visit(processor, this);
  }

  /** A Builder for FieldDescriptors. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setCompileTimeConstant(boolean compileTimeConstant);

    public abstract Builder setConstantValue(@Nullable Literal constantValue);

    public abstract Builder setStatic(boolean isStatic);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setEnclosingTypeDescriptor(
        DeclaredTypeDescriptor enclosingTypeDescriptor);

    public abstract Builder setName(String name);

    public abstract Builder setEnumConstant(boolean isEnumConstant);

    public abstract Builder setSynthetic(boolean isSynthetic);

    public abstract Builder setTypeDescriptor(TypeDescriptor typeDescriptor);

    public abstract TypeDescriptor getTypeDescriptor();

    public abstract Builder setVisibility(Visibility visibility);

    public abstract Builder setOriginalJsInfo(JsInfo jsInfo);

    public abstract Builder setOriginalKtInfo(KtInfo ktInfo);

    public abstract Builder setUnusableByJsSuppressed(boolean isUnusableByJsSuppressed);

    public abstract Builder setDeprecated(boolean isDeprecated);

    public abstract Builder setOrigin(FieldOrigin fieldOrigin);

    @CanIgnoreReturnValue
    public Builder setDeclarationDescriptor(FieldDescriptor declarationFieldDescriptor) {
      return setDeclarationDescriptorOrNullIfSelf(declarationFieldDescriptor);
    }

    // Accessors to support validation, default construction and custom setters.
    abstract Builder setDeclarationDescriptorOrNullIfSelf(
        FieldDescriptor declarationFieldDescriptor);

    abstract Optional<String> getName();

    @Nullable
    abstract Literal getConstantValue();

    abstract boolean isCompileTimeConstant();

    abstract DeclaredTypeDescriptor getEnclosingTypeDescriptor();

    abstract FieldDescriptor autoBuild();

    public FieldDescriptor build() {
      checkState(getName().isPresent());
      checkState(getConstantValue() == null || isCompileTimeConstant());

      boolean isNative = getEnclosingTypeDescriptor().isNative();
      if (!isNative && ignoreNonNativeJsInfo.get()) {
        setOriginalJsInfo(JsInfo.NONE);
      }

      FieldDescriptor fieldDescriptor = autoBuild();

      return interner.intern(fieldDescriptor);
    }

    public static Builder from(FieldDescriptor fieldDescriptor) {
      return fieldDescriptor.toBuilder();
    }

    private static final ThreadLocalInterner<FieldDescriptor> interner =
        new ThreadLocalInterner<>();
  }
}
