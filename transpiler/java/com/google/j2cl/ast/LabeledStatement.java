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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;

/** Labeled Statement. */
@Visitable
public class LabeledStatement extends Statement {

  private final String label;
  @Visitable Statement statement;

  private LabeledStatement(SourcePosition sourcePosition, String label, Statement statement) {
    super(sourcePosition);
    this.label = checkNotNull(label);
    this.statement = checkNotNull(statement);
  }

  public String getLabel() {
    return label;
  }

  public Statement getStatement() {
    return statement;
  }

  @Override
  public LabeledStatement clone() {
    return new LabeledStatement(getSourcePosition(), label, statement.clone());
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_LabeledStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for LabeledStatement. */
  public static class Builder {
    private Statement statement;
    private String label;
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

    public Builder setLabel(String label) {
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
