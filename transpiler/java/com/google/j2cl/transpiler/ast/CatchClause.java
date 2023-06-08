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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.Collections;

/** Class for catch clause. */
@Visitable
public class CatchClause extends Node implements Cloneable<CatchClause> {
  // The visitors traverse the @Visitable members of the class in the order they appear.
  // The variable definition needs to be traversed before the body of the the catch clause.
  @Visitable Variable exceptionVariable;
  @Visitable Block body;

  private CatchClause(Variable exceptionVariable, Block body) {
    this.body = checkNotNull(body);
    this.exceptionVariable = checkNotNull(exceptionVariable);
  }

  public Block getBody() {
    return body;
  }

  public Variable getExceptionVariable() {
    return exceptionVariable;
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_CatchClause.visit(processor, this);
  }

  @Override
  public CatchClause clone() {
    Variable clonedExceptionVariable = exceptionVariable.clone();
    Block clonedBody =
        AstUtils.replaceDeclarations(
            ImmutableList.of(exceptionVariable),
            Collections.singletonList(clonedExceptionVariable),
            body.clone());

    return new CatchClause(clonedExceptionVariable, clonedBody);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for CatchClause. */
  public static class Builder {
    private Variable exceptionVariable;
    private Block body;

    public static Builder from(CatchClause catchClause) {
      return newBuilder()
          .setExceptionVariable(catchClause.getExceptionVariable())
          .setBody(catchClause.getBody());
    }

    public Builder setExceptionVariable(Variable exceptionVariable) {
      this.exceptionVariable = exceptionVariable;
      return this;
    }

    public Builder setBody(Block body) {
      this.body = body;
      return this;
    }

    public CatchClause build() {
      return new CatchClause(exceptionVariable, body);
    }
  }
}
