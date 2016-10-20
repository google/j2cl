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

import com.google.auto.value.AutoValue;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.Interner;
import javax.annotation.Nullable;

/** A (by signature) reference to a field. */
@AutoValue
@Visitable
public abstract class FieldDescriptor extends MemberDescriptor {

  @Override
  public abstract boolean isStatic();

  public abstract Visibility getVisibility();

  @Override
  public abstract TypeDescriptor getEnclosingClassTypeDescriptor();

  @Override
  public abstract String getName();

  public abstract TypeDescriptor getTypeDescriptor();

  public abstract boolean isJsOverlay();

  @Override
  public abstract JsInfo getJsInfo();

  public abstract boolean isCompileTimeConstant();

  @Nullable
  abstract FieldDescriptor getDeclarationFieldDescriptorOrNull();

  /**
   * Returns the descriptor of the field declaration or this instance if this is already the field
   * declaration or there is no field declaration. Field declarations descriptors describe the the
   * field at the declaration place, which might be different to the descriptor at the usage place
   * due to generic type variable instantiations. For example,
   *
   * <p>
   *
   * <pre>
   *   class A<T> {
   *     T f;  // field descriptor here has a type T
   *   }
   *
   *   A<String> a =....
   * </pre>
   */
  public FieldDescriptor getDeclarationFieldDescriptor() {
    return getDeclarationFieldDescriptorOrNull() == null
        ? this
        : getDeclarationFieldDescriptorOrNull();
  }

  @Override
  public boolean isNative() {
    return getEnclosingClassTypeDescriptor().isNative() && !isJsOverlay();
  }

  public boolean isFieldDescriptorForCapturedVariables() {
    return getName().startsWith(AstUtils.CAPTURES_PREFIX);
  }

  public boolean isFieldDescriptorForEnclosingInstance() {
    return getName().startsWith(AstUtils.ENCLOSING_INSTANCE_NAME);
  }

  public boolean isFieldDescriptorForAllCaptures() {
    return isFieldDescriptorForCapturedVariables() || isFieldDescriptorForEnclosingInstance();
  }

  public boolean isJsProperty() {
    return getJsInfo().getJsMemberType() == JsMemberType.PROPERTY;
  }

  @Override
  public boolean isPolymorphic() {
    return !isStatic();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_FieldDescriptor.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for FieldDescriptors. */
  public static class Builder {
    private boolean isStatic;
    private Visibility visibility = Visibility.PUBLIC;
    private TypeDescriptor enclosingClassTypeDescriptor;
    private String fieldName;
    private TypeDescriptor typeDescriptor;
    private boolean isJsOverlay = false;
    private JsInfo jsInfo = JsInfo.NONE;
    private boolean isCompileTimeConstant = false;
    private FieldDescriptor declarationFieldDescriptor;

    public static Builder from(FieldDescriptor fieldDescriptor) {
      Builder builder = new Builder();
      builder.isStatic = fieldDescriptor.isStatic();
      builder.visibility = fieldDescriptor.getVisibility();
      builder.enclosingClassTypeDescriptor = fieldDescriptor.getEnclosingClassTypeDescriptor();
      builder.fieldName = fieldDescriptor.getName();
      builder.typeDescriptor = fieldDescriptor.getTypeDescriptor();
      builder.isJsOverlay = fieldDescriptor.isJsOverlay();
      builder.jsInfo = fieldDescriptor.getJsInfo();
      builder.isCompileTimeConstant = fieldDescriptor.isCompileTimeConstant();
      builder.declarationFieldDescriptor = fieldDescriptor.getDeclarationFieldDescriptorOrNull();
      return builder;
    }

    public Builder setFieldName(String fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    public Builder setEnclosingClassTypeDescriptor(TypeDescriptor enclosingClassTypeDescriptor) {
      this.enclosingClassTypeDescriptor = enclosingClassTypeDescriptor;
      return this;
    }

    public Builder setIsStatic(boolean isStatic) {
      this.isStatic = isStatic;
      return this;
    }

    public Builder setIsJsOverlay(boolean isJsOverlay) {
      this.isJsOverlay = isJsOverlay;
      return this;
    }

    public Builder setIsCompileTimeConstant(boolean isCompileTimeConstant) {
      this.isCompileTimeConstant = isCompileTimeConstant;
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

    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public Builder setDeclarationFieldDescriptor(FieldDescriptor declarationFieldDescriptor) {
      this.declarationFieldDescriptor = declarationFieldDescriptor;
      return this;
    }

    private static final ThreadLocal<Interner<FieldDescriptor>> interner = new ThreadLocal<>();

    private static Interner<FieldDescriptor> getInterner() {
      if (interner.get() == null) {
        interner.set(new Interner<>());
      }
      return interner.get();
    }

    public FieldDescriptor build() {
      return getInterner()
          .intern(
              new AutoValue_FieldDescriptor(
                  isStatic,
                  visibility,
                  enclosingClassTypeDescriptor,
                  fieldName,
                  typeDescriptor,
                  isJsOverlay,
                  jsInfo,
                  isCompileTimeConstant,
                  declarationFieldDescriptor));
    }
  }
}
