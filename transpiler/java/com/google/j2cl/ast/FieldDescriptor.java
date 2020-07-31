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

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.j2cl.common.ThreadLocalInterner;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;

/** A (by signature) reference to a field. */
@AutoValue
public abstract class FieldDescriptor extends MemberDescriptor {

  public abstract TypeDescriptor getTypeDescriptor();

  public abstract boolean isCompileTimeConstant();

  public abstract boolean isEnumConstant();

  public abstract boolean isVariableCapture();

  public abstract boolean isEnclosingInstanceCapture();

  public abstract boolean isDeprecated();

  @Override
  public abstract FieldOrigin getOrigin();

  /** Whether this field originates in the source code or is synthetic. */
  public enum FieldOrigin implements MemberDescriptor.Origin {
    SOURCE,
    SYNTHETIC_BACKING_FIELD("$"),
    SYNTHETIC_ORDINAL_FIELD("$ordinal$");

    private final String prefix;

    FieldOrigin() {
      this("");
    }

    FieldOrigin(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public String getPrefix() {
      return prefix;
    }
  }

  /**
   * Returns the descriptor of the field declaration. A field descriptor might describe a
   * specialized version of a field, e.g.
   *
   * <p>
   *
   * <pre>
   *   class A<T> {
   *     T f;  // Field declaration described as a field "A.f" with type "T".
   *   }
   *
   *   // Field access with field descriptor for field "A.f" with type "String" that has a
   *   // declaration descriptor for field "A.f" but with type "T". Note that both descriptors refer
   *   // to the same field "A.f".
   *   new A<String>().f;
   * <p>
   * </pre>
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
    return isVariableCapture() || isEnclosingInstanceCapture();
  }

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
  public boolean isSameMember(MemberDescriptor thatMember) {
    // TODO(b/69130180): Ideally isSameMember should be not be overridden here and use the
    // implementation in MemberDescriptor, which relies in comparing the declarations directly.
    // The current codebase does not enforce the invariant and sometimes references to the same
    // member end up with different declarations.
    if (!(thatMember instanceof FieldDescriptor)) {
      return false;
    }

    if (!inSameTypeAs(thatMember)) {
      return false;
    }

    FieldDescriptor thisField = getDeclarationDescriptor();
    FieldDescriptor thatField = (FieldDescriptor) thatMember.getDeclarationDescriptor();
    return thisField.getName().equals(thatField.getName());
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

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_FieldDescriptor.Builder()
        // Default values.
        .setVisibility(Visibility.PUBLIC)
        .setJsInfo(JsInfo.NONE)
        .setCompileTimeConstant(false)
        .setStatic(false)
        .setFinal(false)
        .setVariableCapture(false)
        .setEnclosingInstanceCapture(false)
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

  /** A Builder for FieldDescriptors. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setCompileTimeConstant(boolean compileTimeConstant);

    public abstract Builder setStatic(boolean isStatic);

    public abstract Builder setFinal(boolean isFinal);

    public abstract Builder setVariableCapture(boolean isVariableCapture);

    public abstract Builder setEnclosingInstanceCapture(boolean isEnclosingInstanceCapture);

    public abstract Builder setEnclosingTypeDescriptor(
        DeclaredTypeDescriptor enclosingTypeDescriptor);

    public abstract Builder setName(String name);

    public abstract Builder setEnumConstant(boolean isEnumConstant);

    public abstract Builder setSynthetic(boolean isSynthetic);

    public abstract Builder setTypeDescriptor(TypeDescriptor typeDescriptor);

    public abstract Builder setVisibility(Visibility visibility);

    public abstract Builder setJsInfo(JsInfo jsInfo);

    public abstract Builder setUnusableByJsSuppressed(boolean isUnusableByJsSuppressed);

    public abstract Builder setDeprecated(boolean isDeprecated);

    public abstract Builder setOrigin(FieldOrigin fieldOrigin);

    public Builder setDeclarationDescriptor(FieldDescriptor declarationFieldDescriptor) {
      return setDeclarationDescriptorOrNullIfSelf(declarationFieldDescriptor);
    }

    // Accessors to support validation, default construction and custom setters.
    abstract Builder setDeclarationDescriptorOrNullIfSelf(
        FieldDescriptor declarationFieldDescriptor);

    abstract Optional<String> getName();

    abstract FieldDescriptor autoBuild();

    public FieldDescriptor build() {
      checkState(getName().isPresent());
      FieldDescriptor fieldDescriptor = autoBuild();

      checkState(
          !fieldDescriptor.isVariableCapture() || !fieldDescriptor.isEnclosingInstanceCapture());

      return interner.intern(fieldDescriptor);
    }

    public static Builder from(FieldDescriptor fieldDescriptor) {
      return fieldDescriptor.toBuilder();
    }

    private static final ThreadLocalInterner<FieldDescriptor> interner =
        new ThreadLocalInterner<>();
  }
}
