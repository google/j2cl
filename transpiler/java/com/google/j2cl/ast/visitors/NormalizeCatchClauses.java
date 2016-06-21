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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CatchClause;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.Visibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Since JavaScript doesn't support multiple catch clauses, we convert multiple catch clauses to a
 * single catch clause that contains control flow to route to the correct block of code.
 *
 * <p>In Java:
 *
 * <p><code> try { throw new ClassCastException(); } catch (NullPointerException |
 * ClassCastException e) { // expected empty body. } catch (RuntimeException r) { r = null; // used
 * to show exception variable is transpiled correctly. }
 *
 * <p>is transpiled to JavaScript:
 *
 * <p>try { throw ClassCastException.$create(); } catch (e) { if
 * (NullPointerException.$isInstance(e) || ClassCastException.$isInstance(e)) { } else if
 * (RuntimeException.$isInstance(e)) { let r = e; r = null; } else { throw e; } } </code>
 */
public class NormalizeCatchClauses extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private static class Rewriter extends AbstractRewriter {
    @Override
    public Node rewriteTryStatement(TryStatement originalStatement) {
      if (originalStatement.getCatchClauses().isEmpty()) {
        return originalStatement;
      }
      return new TryStatement(
          originalStatement.getResourceDeclarations(),
          originalStatement.getBody(),
          Arrays.asList(mergeClauses(originalStatement.getCatchClauses())),
          originalStatement.getFinallyBlock());
    }

    private CatchClause mergeClauses(List<CatchClause> clauses) {
      if (clauses.isEmpty()) {
        return null;
      }
      Variable mainVariable =
          new Variable(
              clauses.get(0).getExceptionVar().getName(),
              (TypeDescriptors.get().javaLangObject),
              false,
              false);
      Statement body = bodyBuilder(clauses, mainVariable);
      return new CatchClause(new Block(body), mainVariable);
    }

    private Statement bodyBuilder(List<CatchClause> clauses, Variable mainVariable) {
      // Base case. If no more clauses left the last statement throws the exception.
      if (clauses.isEmpty()) {
        Statement noMatchThrowException = new ThrowStatement(mainVariable.getReference());
        return new Block(Arrays.asList(noMatchThrowException));
      }

      CatchClause clause = clauses.get(0);
      Variable caughtVariable = clause.getExceptionVar();
      TypeDescriptor exceptionTypeDescriptor = caughtVariable.getTypeDescriptor();
      List<TypeDescriptor> typesToCheck =
          exceptionTypeDescriptor.isUnion()
              ? exceptionTypeDescriptor.getUnionedTypeDescriptors()
              : Arrays.asList(exceptionTypeDescriptor);
      Expression condition = checkTypeExpression(mainVariable, typesToCheck);
      List<Statement> catchClauseBody = new ArrayList<>(clause.getBody().getStatements());

      // We need to reassign the main variable to the caught variable.
      if (!mainVariable.getName().equals(caughtVariable.getName())) {
        Variable caughtVariableWithThrowableType =
            new Variable(
                caughtVariable.getName(), TypeDescriptors.get().javaLangObject, false, false);
        VariableDeclarationFragment assignmentFragment =
            new VariableDeclarationFragment(
                caughtVariableWithThrowableType, mainVariable.getReference());
        ExpressionStatement assignment =
            new ExpressionStatement(new VariableDeclarationExpression(assignmentFragment));
        catchClauseBody.add(0, assignment);
      }
      Statement rest = bodyBuilder(clauses.subList(1, clauses.size()), mainVariable);
      IfStatement ifStatment = new IfStatement(condition, new Block(catchClauseBody), rest);
      return ifStatment;
    }

    /**
     * Given a list of types t1, t2, t3.. and an exceptionVariable e, this method generates an
     * expression that checks if e is of type t1 or t2, or t3...
     *
     * <p>t1.$isInstance(e) || t2.$isInstance(e) || t3.$isInstance(e) ...
     */
    private Expression checkTypeExpression(
        Variable exceptionVariable, List<TypeDescriptor> typeDescriptors) {
      List<Expression> methodCalls = new ArrayList<>();
      for (TypeDescriptor typeDescriptor : typeDescriptors) {
        methodCalls.add(checkIsInstanceCall(typeDescriptor, exceptionVariable.getReference()));
      }
      return AstUtils.joinExpressionsWithBinaryOperator(
          TypeDescriptors.get().primitiveBoolean, BinaryOperator.CONDITIONAL_OR, methodCalls);
    }

    /**
     * Generate method call:
     *
     * <p>Class.$isInstance(exceptionVariable).
     */
    private MethodCall checkIsInstanceCall(
        TypeDescriptor descriptor, Expression exceptionVariable) {
      MethodDescriptor methodDescriptor =
          MethodDescriptor.Builder.fromDefault()
              .setMethodName(MethodDescriptor.IS_INSTANCE_METHOD_NAME)
              .setIsStatic(true)
              .setJsInfo(JsInfo.RAW)
              .setEnclosingClassTypeDescriptor(descriptor)
              .setVisibility(Visibility.PUBLIC)
              .setParameterTypeDescriptors(Arrays.asList(TypeDescriptors.get().javaLangObject))
              .setReturnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
              .build();
      return MethodCall.createMethodCall(null, methodDescriptor, exceptionVariable);
    }
  }
  
}
