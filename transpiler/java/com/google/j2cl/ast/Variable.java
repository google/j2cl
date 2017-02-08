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
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.ast.common.Cloneable;
import com.google.j2cl.ast.sourcemap.HasSourcePosition;
import com.google.j2cl.common.SourcePosition;

/** Class for local variable and parameter. */
@Visitable
public class Variable extends Node implements Cloneable<Variable>, HasSourcePosition {
  private final String name;
  @Visitable TypeDescriptor typeDescriptor;
  private final boolean isFinal;
  private final boolean isParameter;
  private final boolean isRaw;
  private SourcePosition sourcePosition = SourcePosition.UNKNOWN;

  private Variable(
      SourcePosition sourcePosition,
      String name,
      TypeDescriptor typeDescriptor,
      boolean isFinal,
      boolean isParameter,
      boolean isRaw) {
    this.name = checkNotNull(name);
    setTypeDescriptor(typeDescriptor);
    this.isFinal = isFinal;
    this.isParameter = isParameter;
    this.isRaw = isRaw;
    this.sourcePosition = sourcePosition;
  }

  public String getName() {
    return name;
  }

  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isParameter() {
    return isParameter;
  }

  /**
   * Returns whether this is a Raw variable. Raw variables are not aliased in the output and thus
   * can be used to represent JS native variables, for example, 'arguments'.
   */
  public boolean isRaw() {
    return isRaw;
  }

  public void setTypeDescriptor(TypeDescriptor typeDescriptor) {
    this.typeDescriptor = checkNotNull(typeDescriptor);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_Variable.visit(processor, this);
  }

  public Expression getReference() {
    return new VariableReference(this);
  }

  @Override
  public Variable clone() {
    return Variable.Builder.from(this).build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public void setSourcePosition(SourcePosition sourcePosition) {
    this.sourcePosition = sourcePosition;
  }

  /** Builder for Variable. */
  public static class Builder {

    private String name;
    private TypeDescriptor typeDescriptor;
    private boolean isFinal;
    private boolean isParameter;
    private boolean isRaw;
    private SourcePosition sourcePosition = SourcePosition.UNKNOWN;

    public static Builder from(Variable variable) {
      Builder builder = new Builder();
      builder.name = variable.getName();
      builder.typeDescriptor = variable.getTypeDescriptor();
      builder.isRaw = variable.isRaw();
      builder.isFinal = variable.isFinal();
      builder.isParameter = variable.isParameter;
      builder.sourcePosition = variable.sourcePosition;
      return builder;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setTypeDescriptor(TypeDescriptor typeDescriptor) {
      this.typeDescriptor = typeDescriptor;
      return this;
    }

    public Builder setIsRaw(boolean isRaw) {
      this.isRaw = isRaw;
      return this;
    }

    public Builder setIsParameter(boolean isParameter) {
      this.isParameter = isParameter;
      return this;
    }

    public Builder setIsFinal(boolean isFinal) {
      this.isFinal = isFinal;
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Variable build() {
      checkState(name != null);
      checkState(typeDescriptor != null);
      return new Variable(sourcePosition, name, typeDescriptor, isFinal, isParameter, isRaw);
    }
  }
}
