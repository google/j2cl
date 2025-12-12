/*
 * Copyright 2025 Google Inc.
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Switch case with case expressions. */
@Visitable
public final class SwitchCaseExpressions extends SwitchCase {
  @Visitable List<Expression> caseExpressions;
  @Visitable List<Statement> statements;

  private SwitchCaseExpressions(
      Collection<Expression> caseExpressions,
      Collection<Statement> statements,
      boolean canFallthrough) {
    super(canFallthrough);
    this.caseExpressions = new ArrayList<>(caseExpressions);
    this.statements = new ArrayList<>(statements);
  }

  @Override
  public List<Expression> getCaseExpressions() {
    return caseExpressions;
  }

  @Override
  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public SwitchCaseExpressions clone() {
    return newBuilder()
        .setCaseExpressions(AstUtils.clone(caseExpressions))
        .setStatements(AstUtils.clone(getStatements()))
        .setCanFallthrough(canFallthrough())
        .build();
  }

  @Override
  public Builder toBuilder() {
    return Builder.from(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_SwitchCaseExpressions.visit(processor, this);
  }

  /** A Builder for ExpressionsSwitchCase. */
  public static class Builder extends SwitchCase.Builder<Builder, SwitchCaseExpressions> {
    private List<Expression> caseExpressions = new ArrayList<>();

    public static Builder from(SwitchCaseExpressions switchCase) {
      return newBuilder()
          .setCaseExpressions(switchCase.getCaseExpressions())
          .setStatements(switchCase.getStatements())
          .setCanFallthrough(switchCase.canFallthrough());
    }

    @CanIgnoreReturnValue
    public Builder setCaseExpressions(Collection<Expression> caseExpressions) {
      this.caseExpressions = new ArrayList<>(caseExpressions);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addCaseExpressions(List<Expression> caseExpressions) {
      this.caseExpressions.addAll(caseExpressions);
      return this;
    }

    @Override
    public SwitchCaseExpressions build() {
      return new SwitchCaseExpressions(caseExpressions, statements, canFallthrough);
    }
  }
}
