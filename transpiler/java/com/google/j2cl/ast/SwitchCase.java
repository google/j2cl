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

import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.SourcePosition;
import javax.annotation.Nullable;

/**
 * Switch case statement.
 */
@Visitable
public class SwitchCase extends Statement {
  @Nullable @Visitable Expression matchExpression;

  public SwitchCase(SourcePosition sourcePosition, Expression matchExpression) {
    super(sourcePosition);
    this.matchExpression = matchExpression;
  }

  public SwitchCase(SourcePosition sourcePosition) {
    super(sourcePosition);
  }

  public boolean isDefault() {
    return matchExpression == null;
  }

  public Expression getMatchExpression() {
    return matchExpression;
  }

  @Override
  public SwitchCase clone() {
    return new SwitchCase(getSourcePosition(), AstUtils.clone(matchExpression));
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_SwitchCase.visit(processor, this);
  }
}
