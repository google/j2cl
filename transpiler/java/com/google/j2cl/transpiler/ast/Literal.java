/*
 * Copyright 2017 Google Inc.
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

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Abstract superclass for value literal expressions. */
@Visitable
public abstract class Literal extends Expression {

  public static Literal fromValue(Object constantValue, TypeDescriptor typeDescriptor) {
    if (constantValue instanceof Boolean value) {
      return BooleanLiteral.get(value);
    }
    if (constantValue instanceof Number value) {
      return new NumberLiteral(typeDescriptor.toUnboxedType(), value);
    }
    if (constantValue instanceof Character value) {
      return NumberLiteral.fromChar(value);
    }
    if (constantValue instanceof String value) {
      return new StringLiteral(value);
    }
    throw new InternalCompilerError(
        "Unexpected type for compile time constant: %s", constantValue.getClass().getSimpleName());
  }

  @Override
  public boolean isIdempotent() {
    return true;
  }

  @Override
  public boolean isEffectivelyInvariant() {
    return true;
  }

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  @Override
  public boolean isCompileTimeConstant() {
    return true;
  }

  @Override
  public boolean canBeNull() {
    return false;
  }

  public abstract String getSourceText();

  @Override
  public Precedence getPrecedence() {
    // Literals never need enclosing parens.
    return Precedence.HIGHEST;
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Literal.visit(processor, this);
  }
}
