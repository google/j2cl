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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Switch case. */
@Visitable
public abstract sealed class SwitchCase extends Node implements Cloneable<SwitchCase>
    permits SwitchCaseDefault, SwitchCaseExpressions {
  private final boolean canFallthrough;

  protected SwitchCase(boolean canFallthrough) {
    this.canFallthrough = canFallthrough;
  }

  public boolean isDefault() {
    return false;
  }

  public boolean canFallthrough() {
    return canFallthrough;
  }

  public List<Expression> getCaseExpressions() {
    return ImmutableList.of();
  }

  /**
   * Returns the statements to be executed if the switch case is selected.
   *
   * <p>Even though the "statements" are common to all subclasses, they need to be provided by
   * subclasses explicitly so that the visitor processes them in the right order (e.g. after the
   * case expressions, etc).
   */
  public abstract List<Statement> getStatements();

  @Override
  public abstract SwitchCase clone();

  public abstract Builder<?, ?> toBuilder();

  /** A Builder for SwitchCase. */
  public abstract static class Builder<B extends Builder<B, S>, S extends SwitchCase> {
    protected List<Statement> statements = new ArrayList<>();
    // Switch cases may fallthrough by default.
    protected boolean canFallthrough = true;

    @CanIgnoreReturnValue
    public B setStatements(Collection<Statement> statements) {
      this.statements = new ArrayList<>(statements);
      return getThis();
    }

    @CanIgnoreReturnValue
    public B setStatements(Statement... statements) {
      return setStatements(Arrays.asList(statements));
    }

    @CanIgnoreReturnValue
    public B addStatement(Statement statement) {
      this.statements.add(statement);
      return getThis();
    }

    @CanIgnoreReturnValue
    public B setCanFallthrough(boolean canFallthrough) {
      this.canFallthrough = canFallthrough;
      return getThis();
    }

    @SuppressWarnings("unchecked")
    private B getThis() {
      return (B) this;
    }

    public abstract S build();
  }
}
