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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** For Statement. */
@Visitable
public class ForStatement extends LoopStatement {
  // The visitors traverse the @Visitable members of the class in the order they appear.
  // The components of the ForStatement need to be traversed in order so that variable declarations
  // are visited before their references.
  @Visitable List<Expression> initializers = new ArrayList<>();
  @Visitable Expression conditionExpression;
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

  @Override
  public Expression getConditionExpression() {
    return conditionExpression;
  }

  @Override
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
  Node acceptInternal(Processor processor) {
    return Visitor_ForStatement.visit(processor, this);
  }

  @Override
  Builder toBuilder() {
    return new Builder(this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for ForStatement. */
  public static class Builder extends LoopStatement.Builder<Builder, ForStatement> {
    private List<Expression> initializers = new ArrayList<>();
    private List<Expression> updates = new ArrayList<>();

    public static Builder from(ForStatement forStatement) {
      return new Builder(forStatement);
    }

    private Builder() {}

    private Builder(ForStatement forStatement) {
      super(forStatement);
      setInitializers(forStatement.getInitializers());
      setUpdates(forStatement.getUpdates());
    }

    @CanIgnoreReturnValue
    public Builder setInitializers(List<Expression> initializers) {
      this.initializers = new ArrayList<>(initializers);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setInitializers(Expression... initializers) {
      return setInitializers(Arrays.asList(initializers));
    }

    @CanIgnoreReturnValue
    public Builder setUpdates(List<Expression> updates) {
      this.updates = new ArrayList<>(updates);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setUpdates(Expression... updates) {
      return setUpdates(Arrays.asList(updates));
    }

    @Override
    protected ForStatement doCreateInvocation(
        Expression conditionExpression, Statement body, SourcePosition sourcePosition) {
      return new ForStatement(sourcePosition, conditionExpression, body, initializers, updates);
    }
  }
}
