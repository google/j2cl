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

/** A (by signature) reference to a field. */
@AutoValue
@Visitable
public abstract class FieldDescriptor extends MemberDescriptor {
  public static FieldDescriptor create(
      boolean isStatic,
      Visibility visibility,
      TypeDescriptor enclosingClassTypeDescriptor,
      String fieldName,
      TypeDescriptor typeDescriptor,
      boolean isJsOverlay,
      JsInfo jsInfo,
      boolean isCompileTimeConstant) {
    return new AutoValue_FieldDescriptor(
        isStatic,
        visibility,
        enclosingClassTypeDescriptor,
        fieldName,
        typeDescriptor,
        isJsOverlay,
        jsInfo,
        isCompileTimeConstant);
  }

  /**
   * Creates a raw field reference.
   */
  public static FieldDescriptor createRaw(
      boolean isStatic,
      TypeDescriptor enclosingClassTypeDescriptor,
      String fieldName,
      TypeDescriptor typeDescriptor) {
    return new AutoValue_FieldDescriptor(
        isStatic,
        Visibility.PUBLIC,
        enclosingClassTypeDescriptor,
        fieldName,
        typeDescriptor,
        false,
        JsInfo.RAW_FIELD,
        false);
  }

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

  /**
   * A Builder for easily and correctly creating modified versions of FieldDescriptors.
   */
  public static class Builder {
    private boolean isStatic;
    private Visibility visibility;
    private TypeDescriptor enclosingClassTypeDescriptor;
    private String fieldName;
    private TypeDescriptor typeDescriptor;
    private boolean isJsOverlay;
    private JsInfo jsInfo;
    private boolean isCompileTimeConstant;

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
      return builder;
    }

    public static Builder fromDefault(
        TypeDescriptor enclosingClassTypeDescriptor,
        String fieldName,
        TypeDescriptor typeDescriptor) {
      Builder builder = new Builder();
      builder.visibility = Visibility.PUBLIC;
      builder.enclosingClassTypeDescriptor = enclosingClassTypeDescriptor;
      builder.fieldName = fieldName;
      builder.typeDescriptor = typeDescriptor;
      builder.jsInfo = JsInfo.NONE;
      builder.isCompileTimeConstant = false;
      return builder;
    }

    public Builder setEnclosingClassTypeDescriptor(TypeDescriptor enclosingClassTypeDescriptor) {
      this.enclosingClassTypeDescriptor = enclosingClassTypeDescriptor;
      return this;
    }

    public Builder setIsStatic(boolean isStatic) {
      this.isStatic = isStatic;
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

    public FieldDescriptor build() {
      return create(
          isStatic,
          visibility,
          enclosingClassTypeDescriptor,
          fieldName,
          typeDescriptor,
          isJsOverlay,
          jsInfo,
          isCompileTimeConstant);
    }
  }
}
