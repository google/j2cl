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

import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

/** Switch case. */
@Visitable
public class SwitchCase extends Node implements Cloneable<SwitchCase> {
  @Nullable @Visitable Expression caseExpression;
  @Visitable List<Statement> statements;

  private SwitchCase(Expression caseExpression, Collection<Statement> statements) {
    this.caseExpression = caseExpression;
    this.statements = new ArrayList<>(statements);
  }

  public boolean isDefault() {
    return caseExpression == null;
  }

  public Expression getCaseExpression() {
    return caseExpression;
  }

  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public SwitchCase clone() {
    return newBuilder()
        .setCaseExpression(AstUtils.clone(caseExpression))
        .setStatements(AstUtils.clone(statements))
        .build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_SwitchCase.visit(processor, this);
  }

  /** A Builder for SwitchCase. */
  public static class Builder {
    private Expression caseExpression = null;
    private List<Statement> statements = new ArrayList<>();

    public static Builder from(SwitchCase switchCase) {
      return newBuilder()
          .setCaseExpression(switchCase.getCaseExpression())
          .setStatements(switchCase.getStatements());
    }

    public Builder setCaseExpression(Expression caseExpression) {
      this.caseExpression = caseExpression;
      return this;
    }

    public Builder setStatements(Collection<Statement> statements) {
      this.statements = new ArrayList<>(statements);
      return this;
    }

    public Builder setStatements(Statement... statements) {
      return setStatements(Arrays.asList(statements));
    }

    public Builder addStatement(Statement statement) {
      this.statements.add(statement);
      return this;
    }

    public SwitchCase build() {
      return new SwitchCase(caseExpression, statements);
    }
  }
}
