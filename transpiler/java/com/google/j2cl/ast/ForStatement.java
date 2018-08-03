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

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/**
 * For Statement.
 */
@Visitable
public class ForStatement extends Statement {
  // The visitors traverse the @Visitable members of the class in the order they appear.
  // The components of the ForStatement need to be traversed in order so that variable declarations
  // are visited before their references.
  @Visitable List<Expression> initializers = new ArrayList<>();
  @Visitable @Nullable Expression conditionExpression;
  @Visitable List<Expression> updates = new ArrayList<>();
  @Visitable Statement body;

  private ForStatement(
      SourcePosition sourcePosition,
      Expression conditionExpression,
      Statement body,
      List<Expression> initializers,
      List<Expression> updates) {
    super(sourcePosition);
    this.conditionExpression = conditionExpression;
    this.body = checkNotNull(body);
    this.initializers.addAll(checkNotNull(initializers));
    this.updates.addAll(checkNotNull(updates));
  }

  public Expression getConditionExpression() {
    return conditionExpression;
  }

  public Statement getBody() {
    return body;
  }

  public List<Expression> getInitializers() {
    return initializers;
  }

  public List<Expression> getUpdates() {
    return updates;
  }

  @Override
  public ForStatement clone() {
    return ForStatement.newBuilder()
        .setConditionExpression(AstUtils.clone(conditionExpression))
        .setBody(body.clone())
        .setInitializers(AstUtils.clone(initializers))
        .setUpdates(AstUtils.clone(updates))
        .setSourcePosition(getSourcePosition())
        .build();
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_ForStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for ForStatement. */
  public static class Builder {
    private List<Expression> initializers = new ArrayList<>();
    private Expression conditionExpression;
    private List<Expression> updates = new ArrayList<>();
    private Block.Builder bodyBuilder;
    private SourcePosition sourcePosition;

    public static Builder from(ForStatement forStatement) {
      return newBuilder()
          .setInitializers(forStatement.getInitializers())
          .setConditionExpression(forStatement.getConditionExpression())
          .setUpdates(forStatement.getUpdates())
          .setBody(forStatement.getBody())
          .setSourcePosition(forStatement.getSourcePosition());
    }

    public Builder setConditionExpression(Expression conditionExpression) {
      this.conditionExpression = conditionExpression;
      return this;
    }

    public Builder setInitializers(List<Expression> initializers) {
      this.initializers = new ArrayList<>(initializers);
      return this;
    }

    public Builder setInitializers(Expression... initializers) {
      return setInitializers(Arrays.asList(initializers));
    }

    public Builder setUpdates(List<Expression> updates) {
      this.updates = new ArrayList<>(updates);
      return this;
    }

    public Builder setUpdates(Expression... updates) {
      return setUpdates(Arrays.asList(updates));
    }

    public Builder setBody(Statement body) {
      this.bodyBuilder =
          (body instanceof Block)
              ? Block.Builder.from((Block) body)
              : Block.newBuilder().setSourcePosition(body.getSourcePosition()).setStatements(body);
      return this;
    }

    public Builder addStatement(int index, Statement statement) {
      this.bodyBuilder.addStatement(index, statement);
      return this;
    }

    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public ForStatement build() {
      return new ForStatement(
          sourcePosition, conditionExpression, bodyBuilder.build(), initializers, updates);
    }
  }
}
