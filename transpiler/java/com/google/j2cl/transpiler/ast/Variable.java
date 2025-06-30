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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for local variable and parameter. */
@Visitable
public class Variable extends NameDeclaration implements Cloneable<Variable>, HasAnnotations {
  @Visitable TypeDescriptor typeDescriptor;
  private boolean isFinal;
  private boolean isParameter;
  private final SourcePosition sourcePosition;
  private final ImmutableList<Annotation> annotations;

  private Variable(
      SourcePosition sourcePosition,
      String name,
      TypeDescriptor typeDescriptor,
      boolean isFinal,
      boolean isParameter,
      ImmutableList<Annotation> annotations) {
    super(name);
    setTypeDescriptor(typeDescriptor);
    this.isFinal = isFinal;
    this.isParameter = isParameter;
    this.sourcePosition = checkNotNull(sourcePosition);
    this.annotations = annotations;
  }

  public void setTypeDescriptor(TypeDescriptor typeDescriptor) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
  }

  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  public void setFinal(boolean isFinal) {
    this.isFinal = isFinal;
  }

  public void setParameter(boolean isParameter) {
    this.isParameter = isParameter;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isParameter() {
    return isParameter;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Variable.visit(processor, this);
  }

  @Override
  public VariableReference createReference() {
    return new VariableReference(this);
  }

  @Override
  public Variable clone() {
    return Variable.Builder.from(this).build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public ImmutableList<Annotation> getAnnotations() {
    return annotations;
  }

  /** Builder for Variable. */
  public static class Builder {

    private String name;
    private TypeDescriptor typeDescriptor;
    private boolean isFinal;
    private boolean isParameter;
    private SourcePosition sourcePosition = SourcePosition.NONE;
    private ImmutableList<Annotation> annotations = ImmutableList.of();

    public static Builder from(Variable variable) {
      Builder builder = new Builder();
      builder.name = variable.getName();
      builder.typeDescriptor = variable.getTypeDescriptor();
      builder.isFinal = variable.isFinal();
      builder.isParameter = variable.isParameter;
      builder.sourcePosition = variable.sourcePosition;
      builder.annotations = variable.annotations;
      return builder;
    }

    @CanIgnoreReturnValue
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setParameter(boolean isParameter) {
      this.isParameter = isParameter;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setFinal(boolean isFinal) {
      this.isFinal = isFinal;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setAnnotations(ImmutableList<Annotation> annotations) {
      this.annotations = annotations;
      return this;
    }

    public Variable build() {
      checkState(name != null);
      checkState(typeDescriptor != null);
      return new Variable(sourcePosition, name, typeDescriptor, isFinal, isParameter, annotations);
    }
  }
}
