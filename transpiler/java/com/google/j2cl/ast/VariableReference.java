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
import com.google.j2cl.ast.processors.common.Processor;

/**
 * Class for variable reference.
 */
@Visitable
public class VariableReference extends Expression {
  // Do not mark the target as @Visitable, VariableReference acts as an opaque reference from
  // the visitor perspective.
  private Variable target;

  public VariableReference(Variable target) {
    setTarget(target);
  }

  public Variable getTarget() {
    return target;
  }

  public void setTarget(Variable target) {
    this.target = checkNotNull(target);
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return target.getTypeDescriptor();
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }

  @Override
  public boolean isEffectivelyInvariant() {
    return target.isFinal();
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
    return new VariableReference(target);
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_VariableReference.visit(processor, this);
  }
}
