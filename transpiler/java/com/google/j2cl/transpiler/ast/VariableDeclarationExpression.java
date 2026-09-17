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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import javax.annotation.Nullable;

/** Class for variable declaration expression. */
@Visitable
public class VariableDeclarationExpression extends Expression {
  @Visitable Variable variable;
  @Visitable @Nullable Expression initializer;

  private VariableDeclarationExpression(Variable variable, @Nullable Expression initializer) {
    this.variable = checkNotNull(variable);
    this.initializer = initializer;
  }

  public Variable getVariable() {
    return variable;
  }

  @Nullable
  public Expression getInitializer() {
    return initializer;
  }

  /** Returns true if the variable declaration needs to be JsDoc annotated on output. */
  public boolean needsTypeDeclaration() {
    return initializer == null
        || initializer instanceof NullLiteral
        || variable.getTypeDescriptor().isRaw();
  }

  @Override
  public TypeDescriptor getTypeDescriptor() {
    return PrimitiveTypes.VOID;
  }

  @Override
  public Precedence getPrecedence() {
    // Variable declaration expressions are never nested in places that need precedence.
    throw new UnsupportedOperationException();
  }

  @Override
  public VariableDeclarationExpression clone() {
    // DO NOT clone the variable here as it would make all the references be out of sync
    // pointing to a different variable instance. Variables are replaced explicitly by using
    // AstUtils.replaceVariables.
    return new VariableDeclarationExpression(variable, AstUtils.clone(initializer));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_VariableDeclarationExpression.visit(processor, this);
  }

  public Builder toBuilder() {
    return builder().setVariable(this.getVariable()).setInitializer(this.getInitializer());
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder for VariableDeclarationExpression. */
  public static class Builder {
    private Variable variable;
    private Expression initializer;

    @CanIgnoreReturnValue
    public Builder setVariable(Variable variable) {
      this.variable = variable;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setInitializer(@Nullable Expression initializer) {
      this.initializer = initializer;
      return this;
    }

    public VariableDeclarationExpression build() {
      return new VariableDeclarationExpression(variable, initializer);
    }
  }
}
