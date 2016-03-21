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

import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * For Statement.
 */
@Visitable
public class ForStatement extends Statement {
  @Visitable @Nullable Expression conditionExpression;
  @Visitable Statement body;
  @Visitable List<Expression> initializers = new ArrayList<>();
  @Visitable List<Expression> updates = new ArrayList<>();

  public ForStatement(
      Expression conditionExpression,
      Statement body,
      List<Expression> initializers,
      List<Expression> updates) {
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
  public Node accept(Processor processor) {
    return Visitor_ForStatement.visit(processor, this);
  }
}
