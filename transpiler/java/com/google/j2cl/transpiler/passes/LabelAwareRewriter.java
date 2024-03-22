/*
 * Copyright 2024 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import com.google.common.collect.ImmutableMap;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BreakOrContinueStatement;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import java.util.ArrayDeque;
import java.util.Deque;

/** An abstract class for rewriters that need awareness of labels. */
abstract class LabelAwareRewriter extends AbstractRewriter {
  private final ImmutableMap<Class<?>, Deque<Label>> enclosingLabelsByType =
      ImmutableMap.of(
          BreakStatement.class, new ArrayDeque<>(),
          ContinueStatement.class, new ArrayDeque<>());

  @Override
  public final boolean shouldProcessLoopStatement(LoopStatement loopStatement) {
    pushLabel(getEnclosingLabelOrCreateImplicit("LOOP"), loopStatement);
    return true;
  }

  @Override
  public final boolean shouldProcessSwitchStatement(SwitchStatement switchStatement) {
    pushLabel(getEnclosingLabelOrCreateImplicit("SWITCH"), switchStatement);
    return true;
  }

  private Label getEnclosingLabelOrCreateImplicit(String implicitLabelName) {
    return getParent() instanceof LabeledStatement
        ? ((LabeledStatement) getParent()).getLabel()
        : Label.newBuilder().setName(implicitLabelName).build();
  }

  @Override
  public final Statement rewriteLoopStatement(LoopStatement loopStatement) {
    return rewriteLoopStatement(loopStatement, popLabel(loopStatement));
  }

  /**
   * Provides the ability to transform a LoopStatement node into a different node. Use this method
   * for custom loop rewriting behavior.
   *
   * @param assignedLabel The label associated with the loop (if any) or a synthetically generated
   *     label for identification. Note that synthetic labels may not exist within the AST.
   */
  protected Statement rewriteLoopStatement(LoopStatement loopStatement, Label assignedLabel) {
    return loopStatement;
  }

  @Override
  public final Statement rewriteSwitchStatement(SwitchStatement switchStatement) {
    return rewriteSwitchStatement(switchStatement, popLabel(switchStatement));
  }

  /**
   * Provides the ability to transform a SwitchStatement node into a different node. Use this method
   * for custom loop rewriting behavior.
   *
   * @param assignedLabel The label associated with the switch (if any) or a synthetically generated
   *     label for identification. Note that synthetic labels may not exist within the AST.
   */
  protected Statement rewriteSwitchStatement(SwitchStatement switchStatement, Label assignedLabel) {
    return switchStatement;
  }

  private void pushLabel(Label label, Statement targetStatement) {
    enclosingLabelsByType.get(BreakStatement.class).push(label);
    if (targetStatement instanceof LoopStatement) {
      // Only loops are targets of continue statements.
      enclosingLabelsByType.get(ContinueStatement.class).push(label);
    }
  }

  private Label popLabel(Statement targetStatement) {
    if (targetStatement instanceof LoopStatement) {
      // Only loops are targets of continue statements.
      enclosingLabelsByType.get(ContinueStatement.class).pop();
    }
    return enclosingLabelsByType.get(BreakStatement.class).pop();
  }

  protected Label getTargetLabel(BreakOrContinueStatement breakOrContinueStatement) {
    return enclosingLabelsByType.get(breakOrContinueStatement.getClass()).peek();
  }
}
