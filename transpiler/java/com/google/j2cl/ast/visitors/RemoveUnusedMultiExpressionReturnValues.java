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
package com.google.j2cl.ast.visitors;

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.OperatorSideEffectUtils;
import com.google.j2cl.ast.Statement;
import java.util.List;

/**
 * Finds times when the return value in a MultiExpression is unused, and removes it.
 *
 * <p>This does not do general purpose removal of all possible side-effect-free unused return
 * values. Only the FieldAccess case is addressed since it is the only case we generate that
 * exhibits this problem at this time and since we don't have side-effect tracking.
 *
 * <p>While technically this amounts to a local optimization the actual motivation is to avoid
 * triggering errors in JSCompiler which does not like seeing unused return values.
 */
public class RemoveUnusedMultiExpressionReturnValues extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          /**
           * Examines the case when a MultiExpression is directly contained in an
           * ExpressionStatement since we know for certain that in this situation the
           * MultiExpression's returned value must be unused.
           */
          @Override
          public Statement rewriteExpressionStatement(ExpressionStatement expressionStatement) {
            // Ignore non multi expressions.
            if (!(expressionStatement.getExpression() instanceof MultiExpression)) {
              return expressionStatement;
            }
            MultiExpression multiExpression = (MultiExpression) expressionStatement.getExpression();
            List<Expression> expressions = multiExpression.getExpressions();
            // Can't do anything if the multi expression contains no expressions.
            if (expressions.isEmpty()) {
              return expressionStatement;
            }
            // Only target return values that are FieldAccesses since we know they are side effect
            // free and we know that we generate this case.
            // TODO(rluble): This is not technically correct as field accesses could trigger
            // clinits, but multiexpressions only come about from our normalization and in those
            // transformations clinit would have been already triggered. Add a verifier pass to make
            // sure the semantics do not change.
            if (!OperatorSideEffectUtils.canExpressionBeEvaluatedTwice(
                Iterables.getLast(expressions))) {
              return expressionStatement;
            }

            // Return a replacement with the unused return value expression trimmed off.
            return MultiExpression.newBuilder()
                .setExpressions(expressions.subList(0, expressions.size() - 1))
                .build()
                .makeStatement();
          }
        });
  }
}
