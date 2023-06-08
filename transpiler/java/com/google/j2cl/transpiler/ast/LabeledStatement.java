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
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Labeled Statement. */
@Visitable
public class LabeledStatement extends Statement {

  @Visitable Label label;
  @Visitable Statement statement;

  private LabeledStatement(SourcePosition sourcePosition, Label label, Statement statement) {
    super(sourcePosition);
    this.label = checkNotNull(label);
    this.statement = checkNotNull(statement);
  }

  public Label getLabel() {
    return label;
  }

  public Statement getStatement() {
    return statement;
  }

  @Override
  public LabeledStatement clone() {
    Label newLabel = label.clone();
    return new LabeledStatement(
        getSourcePosition(),
        newLabel,
        AstUtils.replaceDeclarations(
            ImmutableList.of(label), ImmutableList.of(newLabel), statement.clone()));
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_LabeledStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for LabeledStatement. */
  public static class Builder {
    private Statement statement;
    private Label label;
    private SourcePosition sourcePosition;

    public static Builder from(LabeledStatement labeledStatement) {
      return newBuilder()
          .setSourcePosition(labeledStatement.getSourcePosition())
          .setStatement(labeledStatement.getStatement())
          .setLabel(labeledStatement.getLabel());
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public Builder setLabel(Label label) {
      this.label = label;
      return this;
    }

    public Builder setStatement(Statement statement) {
      this.statement = statement;
      return this;
    }

    public LabeledStatement build() {
      return new LabeledStatement(sourcePosition, label, statement);
    }
  }
}
