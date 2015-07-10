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

import com.google.j2cl.ast.processors.Visitable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Class for try statement.
 * TODO: add resources field.
 */
@Visitable
public class TryStatement extends Statement {
  @Visitable Block body;
  @Visitable List<CatchClause> catchClauses = new ArrayList<>();
  @Visitable @Nullable Block finallyBlock;

  public TryStatement(Block body, List<CatchClause> catchClauses, Block finallyBlock) {
    this.body = body;
    this.catchClauses.addAll(catchClauses);
    this.finallyBlock = finallyBlock;
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

  public void setBody(Block body) {
    this.body = body;
  }

  public void setFinallyBlock(Block finallyBlock) {
    this.finallyBlock = finallyBlock;
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_TryStatement.visit(processor, this);
  }
}
