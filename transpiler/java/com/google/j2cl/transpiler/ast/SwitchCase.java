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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Switch case. */
@Visitable
public class SwitchCase extends Node implements Cloneable<SwitchCase> {
  @Visitable List<Expression> caseExpressions;
  @Visitable List<Statement> statements;
  private final boolean isDefault;
  private final boolean canFallthrough;

  private SwitchCase(
      Collection<Expression> caseExpressions,
      Collection<Statement> statements,
      boolean isDefault,
      boolean canFallthrough) {
    this.caseExpressions = new ArrayList<>(caseExpressions);
    this.statements = new ArrayList<>(statements);
    this.isDefault = isDefault;
    this.canFallthrough = canFallthrough;
    checkArgument(isDefault == caseExpressions.isEmpty());
  }

  public boolean isDefault() {
    return isDefault;
  }

  public boolean canFallthrough() {
    return canFallthrough;
  }

  public List<Expression> getCaseExpressions() {
    return caseExpressions;
  }

  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public SwitchCase clone() {
    return newBuilder()
        .setCaseExpressions(AstUtils.clone(caseExpressions))
        .setStatements(AstUtils.clone(statements))
        .setDefault(isDefault)
        .setCanFallthrough(canFallthrough)
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
    private List<Expression> caseExpressions = new ArrayList<>();
    private List<Statement> statements = new ArrayList<>();
    private boolean isDefault = false;
    // Switch cases may fallthrough by default.
    private boolean canFallthrough = true;

    public static Builder from(SwitchCase switchCase) {
      return newBuilder()
          .setCaseExpressions(switchCase.getCaseExpressions())
          .setStatements(switchCase.getStatements())
          .setDefault(switchCase.isDefault)
          .setCanFallthrough(switchCase.canFallthrough);
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

    @CanIgnoreReturnValue
    public Builder setStatements(Collection<Statement> statements) {
      this.statements = new ArrayList<>(statements);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setStatements(Statement... statements) {
      return setStatements(Arrays.asList(statements));
    }

    @CanIgnoreReturnValue
    public Builder addStatement(Statement statement) {
      this.statements.add(statement);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setDefault(boolean isDefault) {
      this.isDefault = isDefault;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setCanFallthrough(boolean canFallthrough) {
      this.canFallthrough = canFallthrough;
      return this;
    }

    public SwitchCase build() {
      return new SwitchCase(caseExpressions, statements, isDefault, canFallthrough);
    }
  }
}
