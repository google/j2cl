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
      SourcePosition sourcePosition,
      List<VariableDeclarationExpression> resourceDeclarations,
      Block body,
      List<CatchClause> catchClauses,
      Block finallyBlock) {
    super(sourcePosition);
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
    return TryStatement.newBuilder()
        .setSourcePosition(getSourcePosition())
        .setResourceDeclarations(AstUtils.clone(resourceDeclarations))
        .setBody(body.clone())
        .setCatchClauses(AstUtils.clone(catchClauses))
        .setFinallyBlock(AstUtils.clone(finallyBlock))
        .build();
  }

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_TryStatement.visit(processor, this);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for TryStatement. */
  public static class Builder {
    private List<VariableDeclarationExpression> resourceDeclarations = new ArrayList<>();
    private List<CatchClause> catchClauses = new ArrayList<>();
    private Block body;
    private Block finallyBlock;
    private SourcePosition sourcePosition;

    public static Builder from(TryStatement tryStatement) {
      return newBuilder()
          .setResourceDeclarations(tryStatement.getResourceDeclarations())
          .setBody(tryStatement.getBody())
          .setCatchClauses(tryStatement.getCatchClauses())
          .setFinallyBlock(tryStatement.getFinallyBlock())
          .setSourcePosition(tryStatement.getSourcePosition());
    }

    public Builder setResourceDeclarations(
        List<VariableDeclarationExpression> resourceDeclarations) {
      this.resourceDeclarations = new ArrayList<>(resourceDeclarations);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setResourceDeclarations(VariableDeclarationExpression... resourceDeclarations) {
      return setResourceDeclarations(Arrays.asList(resourceDeclarations));
    }

    @CanIgnoreReturnValue
    public Builder setCatchClauses(List<CatchClause> catchClauses) {
      this.catchClauses = new ArrayList<>(catchClauses);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setCatchClauses(CatchClause... catchClauses) {
      return setCatchClauses(Arrays.asList(catchClauses));
    }

    @CanIgnoreReturnValue
    public Builder setBody(Block body) {
      this.body = body;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setBody(Statement... body) {
      this.body = Block.newBuilder().setStatements(body).build();
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setFinallyBlock(Block finallyBlock) {
      this.finallyBlock = finallyBlock;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder setSourcePosition(SourcePosition sourcePosition) {
      this.sourcePosition = sourcePosition;
      return this;
    }

    public TryStatement build() {
      return new TryStatement(
          sourcePosition, resourceDeclarations, body, catchClauses, finallyBlock);
    }
  }
}
