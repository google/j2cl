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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Default switch case. */
@Visitable
public final class SwitchCaseDefault extends SwitchCase {
  @Visitable List<Statement> statements;

  private SwitchCaseDefault(
      SourcePosition sourcePosition, Collection<Statement> statements, boolean canFallthrough) {
    super(sourcePosition, canFallthrough);
    this.statements = new ArrayList<>(statements);
  }

  @Override
  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public boolean isDefault() {
    return true;
  }

  @Override
  public SwitchCaseDefault clone() {
    return newBuilder()
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
    return Visitor_SwitchCaseDefault.visit(processor, this);
  }

  /** A Builder for SwitchCase. */
  public static class Builder extends SwitchCase.Builder<Builder, SwitchCaseDefault> {

    public static Builder from(SwitchCase switchCase) {
      return newBuilder()
          .setStatements(switchCase.getStatements())
          .setCanFallthrough(switchCase.canFallthrough())
          .setSourcePosition(switchCase.getSourcePosition());
    }

    @Override
    public SwitchCaseDefault build() {
      return new SwitchCaseDefault(sourcePosition, statements, canFallthrough);
    }
  }
}
