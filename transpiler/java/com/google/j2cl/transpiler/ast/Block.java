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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Block Statement. */
@Visitable
public class Block extends Statement {
  @Visitable List<Statement> statements;

  private Block(SourcePosition sourcePosition, List<Statement> statements) {
    super(sourcePosition);
    this.statements = statements;
  }

  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public boolean isNoop() {
    return statements.stream().allMatch(Statement::isNoop);
  }

  @Override
  public Block clone() {
    return Block.newBuilder()
        .setSourcePosition(getSourcePosition())
        .setStatements(AstUtils.clone(statements))
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Block.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** A Builder for Block. */
  public static class Builder {
    private final List<Statement> statements = new ArrayList<>();
    // Ok to have blocks without source position, since there is not code that they directly
    // execute.
    private SourcePosition sourcePosition = SourcePosition.NONE;

    public static Builder from(Block block) {
      return newBuilder()
          .setSourcePosition(block.getSourcePosition())
          .setStatements(block.getStatements());
    }

    public Builder setStatements(Statement... statements) {
      return setStatements(Arrays.asList(statements));
    }

    public Builder setStatements(Collection<Statement> statements) {
      this.statements.clear();
      return addStatements(statements);
    }

    public Builder addStatement(Statement statement) {
      this.statements.add(statement);
      return this;
    }

    public Builder addStatement(int index, Statement statement) {
      this.statements.add(index, statement);
      return this;
    }

    public Builder addStatements(Collection<Statement> statements) {
      this.statements.addAll(statements);
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Block build() {
      return new Block(sourcePosition, new ArrayList<>(statements));
    }
  }
}
