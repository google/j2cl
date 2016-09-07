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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class for try statement.
 */
@Visitable
public class TryStatement extends Statement {
  // The visitors traverse the @Visitable members of the class in the order they appear.
  // The resource declarations contain variable definitions that need to be traversed before
  // the rest of the statements.
  @Visitable final List<VariableDeclarationExpression> resourceDeclarations = new ArrayList<>();
  @Visitable Block body;
  @Visitable List<CatchClause> catchClauses = new ArrayList<>();
  @Visitable @Nullable Block finallyBlock;

  public TryStatement(
      List<VariableDeclarationExpression> resourceDeclarations,
      Block body,
      List<CatchClause> catchClauses,
      Block finallyBlock) {
    this.body = checkNotNull(body);
    this.catchClauses.addAll(checkNotNull(catchClauses));
    this.finallyBlock = finallyBlock;
    this.resourceDeclarations.addAll(resourceDeclarations);
  }

  public Block getBody() {
    return body;
  }

  public List<CatchClause> getCatchClauses() {
    return catchClauses;
  }

  public Block getFinallyBlock() {
    return finallyBlock;
  }

  public List<VariableDeclarationExpression> getResourceDeclarations() {
    return resourceDeclarations;
  }

  @Override
  public TryStatement clone() {
    TryStatement tryStatement =
        new TryStatement(
            AstUtils.clone(resourceDeclarations),
            body.clone(),
            AstUtils.clone(catchClauses),
            AstUtils.clone(finallyBlock));
    tryStatement.setSourcePosition(this.getSourcePosition());
    return tryStatement;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TryStatement.visit(processor, this);
  }
}
