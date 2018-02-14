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

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.SourcePosition;

/** Base class for expressions. */
@Visitable
public abstract class Expression extends Node implements Cloneable<Expression> {

  /** Returns the type descriptor of the value that is returned by this expression. */
  public abstract TypeDescriptor getTypeDescriptor();

  /**
   * Returns true is the expression can be evaluated multiple times and always results in the same
   * value.
   *
   * <p>Note: that the expression might have side effects (e.g. cause some class initializers to
   * run). An expression is idempotent if when evaluated in the same state multiple times yields the
   * same resulting state and value.
   */
  public boolean isIdempotent() {
    return false;
  }

  /** Creates an ExpressionStatement with this expression as its code */
  public ExpressionStatement makeStatement(SourcePosition sourcePosition) {
    return new ExpressionStatement(sourcePosition, this);
  }

  /** Returns the expression enclosed as an expression with a comment. */
  public ExpressionWithComment withComment(String comment) {
    return new ExpressionWithComment(this, comment);
  }

  @Override
  public abstract Expression clone();

  @Override
  public Node accept(Processor processor) {
    return Visitor_Expression.visit(processor, this);
  }

}
