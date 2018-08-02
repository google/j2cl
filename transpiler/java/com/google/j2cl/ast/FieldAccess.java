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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.SourcePosition;
import java.util.Optional;

/** Class for field access. */
@Visitable
public class FieldAccess extends Expression implements MemberReference {
  @Visitable Expression qualifier;
  @Visitable FieldDescriptor targetFieldDescriptor;
  final Optional<SourcePosition> sourcePosition;

  private FieldAccess(
      Expression qualifier,
      FieldDescriptor targetFieldDescriptor,
      Optional<SourcePosition> sourcePosition) {
    this.targetFieldDescriptor = checkNotNull(targetFieldDescriptor);
    this.qualifier = checkNotNull(qualifier);
    this.sourcePosition = checkNotNull(sourcePosition);
  }

  @Override
  public Expression getQualifier() {
    return qualifier;
  }

  /**
   * Would normally be named getTargetFieldDescriptor() but in this situation it was more important
   * to implement the MemberReference interface.
   */
  @Override
  public FieldDescriptor getTarget() {
    return targetFieldDescriptor;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return targetFieldDescriptor.getTypeDescriptor();
  }

  public Optional<SourcePosition> getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public boolean isIdempotent() {
    return getQualifier() == null || getQualifier().isIdempotent();
  }

  @Override
  public FieldAccess clone() {
    return new FieldAccess(qualifier.clone(), targetFieldDescriptor, sourcePosition);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_FieldAccess.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for FieldAccess. */
  public static class Builder {

    private FieldDescriptor targetFieldDescriptor;
    private Expression qualifier;
    private Optional<SourcePosition> sourcePosition = Optional.empty();

    public static Builder from(FieldDescriptor targetFieldDescriptor) {
      return newBuilder().setTargetFieldDescriptor(targetFieldDescriptor);
    }

    public static Builder from(Field targetField) {
      return from(targetField.getDescriptor());
    }

    public static Builder from(FieldAccess fieldAccess) {
      return newBuilder()
          .setTargetFieldDescriptor(fieldAccess.getTarget())
          .setQualifier(fieldAccess.getQualifier())
          .setSourcePosition(fieldAccess.getSourcePosition());
    }

    public Builder setTargetFieldDescriptor(FieldDescriptor targetFieldDescriptor) {
      this.targetFieldDescriptor = targetFieldDescriptor;
      return this;
    }

    public Builder setQualifier(Expression qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    public Builder setSourcePosition(Optional<SourcePosition> sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public FieldAccess build() {
      return new FieldAccess(
          AstUtils.getExplicitQualifier(qualifier, targetFieldDescriptor),
          targetFieldDescriptor,
          sourcePosition);
    }
  }
}
