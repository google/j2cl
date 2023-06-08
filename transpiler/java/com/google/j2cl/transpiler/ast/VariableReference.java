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

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Class for variable reference. */
@Visitable
public class VariableReference extends Expression implements Reference<Variable> {
  // Do not mark the target variable as @Visitable, VariableReference acts as an opaque reference
  // from the visitor perspective.
  private final Variable variable;

  public VariableReference(Variable variable) {
    this.variable = checkNotNull(variable);
  }

  @Override
  public Variable getTarget() {
    return variable;
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return variable.getTypeDescriptor();
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }

  @Override
  public boolean isEffectivelyInvariant() {
    return variable.isFinal();
  }

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  @Override
  public boolean isLValue() {
    return true;
  }

  @Override
  public Precedence getPrecedence() {
    return Precedence.HIGHEST;
  }

  @Override
  public VariableReference clone() {
    return new VariableReference(variable);
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_VariableReference.visit(processor, this);
  }
}
