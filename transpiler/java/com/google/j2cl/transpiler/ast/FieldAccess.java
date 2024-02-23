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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for field access. */
@Visitable
public class FieldAccess extends MemberReference {
  private final SourcePosition sourcePosition;

  private FieldAccess(Expression qualifier, FieldDescriptor target, SourcePosition sourcePosition) {
    super(qualifier, target);
    this.sourcePosition = checkNotNull(sourcePosition);
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return getTarget().getTypeDescriptor();
  }

  @Override
  public TypeDescriptor getDeclaredTypeDescriptor() {
    return getTarget().getDeclarationDescriptor().getTypeDescriptor();
  }

  @Override
  public FieldDescriptor getTarget() {
    return (FieldDescriptor) super.getTarget();
  }

  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public boolean isIdempotent() {
    return getQualifier() == null || getQualifier().isIdempotent();
  }

  @Override
  public boolean isEffectivelyInvariant() {
    // Only captured fields are considered invariant.

    // While it seems logical to consider access to final fields to be invariant, that is not the
    // case in general. Access to final static fields has side effects due to the triggering of the
    // class initializer. Even access to final instance fields can not be considered invariant
    // since they can be observed in their uninitialized state.

    checkState(!getTarget().isCapture() || getQualifier().isEffectivelyInvariant());
    return getTarget().isCapture();
  }

  @Override
  public boolean hasSideEffects() {
    // Access to captures does not have side effects, all others might.
    // Access to instance fields might trigger an NPE and access to static fields might trigger
    // class initialization.
    return !getTarget().isCapture();
  }

  @Override
  public boolean isLValue() {
    return true;
  }

  @Override
  public boolean canBeNull() {
    return !getTarget().isEnumConstant()
        && !getTarget().isCompileTimeConstant()
        && super.canBeNull();
  }

  @Override
  public FieldAccess clone() {
    return new FieldAccess(AstUtils.clone(qualifier), getTarget(), sourcePosition);
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_FieldAccess.visit(processor, this);
  }

  @Override
  Builder createBuilder() {
    return new Builder(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for FieldAccess. */
  public static class Builder
      extends MemberReference.Builder<Builder, FieldAccess, FieldDescriptor> {
    private SourcePosition sourcePosition = SourcePosition.NONE;

    public static Builder from(FieldDescriptor targetFieldDescriptor) {
      return newBuilder().setTarget(targetFieldDescriptor);
    }

    public static Builder from(Field targetField) {
      return from(targetField.getDescriptor());
    }

    public static Builder from(FieldAccess fieldAccess) {
      return newBuilder()
          .setTarget(fieldAccess.getTarget())
          .setQualifier(fieldAccess.getQualifier())
          .setSourcePosition(fieldAccess.getSourcePosition());
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    @Override
    public FieldAccess build() {
      return new FieldAccess(getQualifier(), getTarget(), sourcePosition);
    }

    private Builder(FieldAccess fieldAccess) {
      super(fieldAccess);
      this.sourcePosition = fieldAccess.getSourcePosition();
    }

    private Builder() {}
  }
}
