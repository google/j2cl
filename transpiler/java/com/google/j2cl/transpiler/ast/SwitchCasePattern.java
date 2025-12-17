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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

/** Switch case with a pattern and optionally a guard. */
@Visitable
public final class SwitchCasePattern extends SwitchCase {
  @Visitable Pattern pattern;
  @Visitable @Nullable Expression guard;
  @Visitable List<Statement> statements;

  private SwitchCasePattern(
      SourcePosition sourcePosition,
      Pattern pattern,
      Expression guard,
      Collection<Statement> statements,
      boolean canFallthrough) {
    super(sourcePosition, canFallthrough);
    this.pattern = checkNotNull(pattern);
    this.guard = guard;
    this.statements = new ArrayList<>(statements);
  }

  public Pattern getPattern() {
    return pattern;
  }

  public Expression getGuard() {
    return guard;
  }

  @Override
  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public SwitchCasePattern clone() {
    return newBuilder()
        .setPattern(AstUtils.clone(pattern))
        .setGuard(AstUtils.clone(guard))
        .setStatements(AstUtils.clone(statements))
        .setCanFallthrough(canFallthrough())
        .setSourcePosition(getSourcePosition())
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
    return Visitor_SwitchCasePattern.visit(processor, this);
  }

  /** A Builder for SwitchCasePattern. */
  public static class Builder extends SwitchCase.Builder<Builder, SwitchCasePattern> {
    private Pattern pattern;
    private Expression guard;

    public static Builder from(SwitchCasePattern switchCase) {
      return newBuilder()
          .setPattern(switchCase.getPattern())
          .setGuard(switchCase.getGuard())
          .setStatements(switchCase.getStatements())
          .setCanFallthrough(switchCase.canFallthrough())
          .setSourcePosition(switchCase.getSourcePosition());
    }

    @CanIgnoreReturnValue
    public Builder setPattern(Pattern pattern) {
      this.pattern = pattern;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setGuard(Expression guard) {
      this.guard = guard;
      return this;
    }

    @Override
    public SwitchCasePattern build() {
      return new SwitchCasePattern(sourcePosition, pattern, guard, statements, canFallthrough);
    }
  }
}
