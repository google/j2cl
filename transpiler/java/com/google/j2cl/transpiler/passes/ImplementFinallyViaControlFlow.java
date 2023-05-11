/*
 * Copyright 2023 Google Inc.
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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicates;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CatchClause;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Normalizes finally into explicit control flow.
 *
 * <p>Rewrites code of the form:
 *
 * <pre>{@code
 * FOR_BREAK:
 * for(;;) {
 *   try {
 *     if (a)  break FOR_BREAK;
 *     if (b) return 3;
 *     // … code that might throw ….
 *   } catch (Exception e) {
 *     // … code in catch ….
 *   } finally {
 *     // ….code in finally….
 *   }
 * }
 * }</pre>
 *
 * <p>into
 *
 * <pre>{@code
 * FOR_BREAK:
 * for(;;) {
 *     // Keeps track of the exit that needs to be taken from the finally block.
 *     int exitSelector = 0;
 *     // Records the return value that needs to be accessed to exit the finally block.
 *     int savedReturnValue;
 *     // Records the exception that needs to be rethrown exiting the finally block
 *     Throwable savedThrown;
 *
 *     FINALLY_LABEL: {
 *        try { // Catch all exceptions to be able to run finally and then rethrow.
 *          try {
 *           if (a) { exitSelector = 1; break FINALLY_LABEL;}
 *           if (b) { savedReturnValue = 3; exitSelector = 2 ; break FINALLY_LABEL;}
 *           // … code that might throw ….
 *         } catch (Exception e) {
 *           // … code in catch ….
 *         }
 *       } catch (Throwable t) {
 *         exitSelector = 3
 *         savedThrown = t
 *       }
 *     }
 *     // ….code in finally….
 *
 *     // And exitSelector value of 0 means that the control flow exited the try block normally
 *     // and is let flow straight without explicit handling here.
 *     switch (exitSelector) {
 *       case 1: break FOR_BREAK;
 *       case 2: return savedReturnValue;
 *       case 3: throw savedThrown;
 *     }
 *   }
 * }
 * }</pre>
 *
 * Note that once an inner try-finally statement is rewritten, an outer try-finally will rewrite the
 * newly created exit dispatching code at the end of the finally block.
 */
public class ImplementFinallyViaControlFlow extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          // Keep track of the finally blocks in a method to assign more meaningful names to
          // variables to make it easier when looking at AST dumps.
          private int currentFinallyBlock = 0;

          @Override
          public Statement rewriteTryStatement(TryStatement tryStatement) {
            if (tryStatement.getFinallyBlock() == null) {
              // No finally block, no rewriting needed.
              return tryStatement;
            }

            return new FinallyHandlerBuilder(
                    tryStatement,
                    (MethodDescriptor) getCurrentMember().getDescriptor(),
                    this::isLabelEnclosingTry,
                    currentFinallyBlock++)
                .build();
          }

          /** Returns true if the label encloses the original try-finally statement. */
          private boolean isLabelEnclosingTry(Label label) {
            return getParent(
                    n ->
                        n instanceof LabeledStatement && ((LabeledStatement) n).getLabel() == label)
                != null;
          }

          @Override
          public Method rewriteMethod(Method method) {
            if (currentFinallyBlock == 0) {
              // Method did not have a finally block.
              return method;
            }
            // Reset state for the start of the next method.
            currentFinallyBlock = 0;

            MethodDescriptor methodDescriptor = method.getDescriptor();
            if (TypeDescriptors.isPrimitiveVoid(methodDescriptor.getReturnTypeDescriptor())
                || method.isConstructor()) {
              return method;
            }
            // The new control flow makes it difficult for the wasm verifier to determine that the
            // method is guaranteed to return. Throwing an exception at the end ensures Wasm
            // invariants are is kept.
            return Method.Builder.from(method)
                .addStatements(
                    ThrowStatement.newBuilder()
                        .setExpression(TypeDescriptors.get().javaLangThrowable.getNullValue())
                        .setSourcePosition(method.getSourcePosition())
                        .build())
                .build();
          }
        });
  }

  private static class FinallyHandlerBuilder {
    private final TryStatement originalTryStatement;
    private final Predicate<Label> isLabelEnclosingTry;

    // Statements to exit the finally block with the appropriate action.
    private final List<Statement> exitStatements = new ArrayList<>();

    // Variable to keep the id of the exit taken out of the try block to be able to dispatch
    // with the right code after finally is executed. A value of 0 in the exit selector
    // means that the code exited through the normal control flow path; for all other exits
    // a unique value is generated.
    private final Variable exitSelectorVariable;

    // Variable to track the return value so that it can be returned from the finally block.
    private final Variable savedReturnValueVariable;

    // Variable to track the exception if the exit was via "throw" (so it can be rethrown
    // if needed).
    private final Variable savedThrownVariable;

    // Label to jump into the finally block.
    private final Label finallyLabel;

    FinallyHandlerBuilder(
        TryStatement originalTryStatement,
        MethodDescriptor enclosingMethodDescriptor,
        Predicate<Label> isLabelEnclosingTry,
        int finallyBlockNumber) {
      this.originalTryStatement = originalTryStatement;
      this.isLabelEnclosingTry = isLabelEnclosingTry;

      // Create the variables to save the all the state needed to exit the finally block.
      this.exitSelectorVariable =
          Variable.newBuilder()
              .setName("exitSelector." + finallyBlockNumber)
              .setTypeDescriptor(PrimitiveTypes.INT)
              .build();

      this.savedThrownVariable =
          Variable.newBuilder()
              .setName("savedThrown." + finallyBlockNumber)
              .setTypeDescriptor(TypeDescriptors.get().javaLangThrowable)
              .build();

      TypeDescriptor returnTypeDescriptor = enclosingMethodDescriptor.getReturnTypeDescriptor();
      this.savedReturnValueVariable =
          TypeDescriptors.isPrimitiveVoid(returnTypeDescriptor)
                  || enclosingMethodDescriptor.isConstructor()
              ? null
              : Variable.newBuilder()
                  .setName("savedReturnValue." + finallyBlockNumber)
                  .setTypeDescriptor(returnTypeDescriptor)
                  .build();

      this.finallyLabel = Label.newBuilder().setName("FINALLY." + finallyBlockNumber).build();
    }

    /** Performs the control flow transformation of the try-catch-finally. */
    public Statement build() {
      Statement variableDeclarations = createVariableDeclarations();

      // Remove the finally block. It will be moved out to the end.
      TryStatement tryWithoutFinally =
          TryStatement.Builder.from(originalTryStatement).setFinallyBlock(null).build();

      TryStatement tryWithRewrittenExits = rewriteControlFlow(tryWithoutFinally);

      Statement tryWrappedWithExceptionHandling =
          wrapTryToHandleExitsViaThrow(tryWithRewrittenExits);

      return Block.newBuilder()
          .setStatements(
              // Declare introduced tracking variables.
              variableDeclarations,
              // Wrap the try-catch block with a finally exit labelled block, that
              // goes straight to execute the finally block.
              LabeledStatement.newBuilder()
                  .setLabel(finallyLabel)
                  .setStatement(tryWrappedWithExceptionHandling)
                  .setSourcePosition(originalTryStatement.getSourcePosition())
                  .build(),
              // Add the code from the original finally block.
              originalTryStatement.getFinallyBlock(),
              // And finally using the exit selector and the saved values do the appropriate
              // dispatch.
              createExitSwitch())
          .build();
    }

    /**
     * Rewrites the control flow in the try-finally block by replacing all the exits from returns,
     * breaks and continues by code to save needed values and a break into the finally block.
     */
    private TryStatement rewriteControlFlow(TryStatement tryStatement) {

      // Rewrite all non-exceptional exits (breaks, continues and returns) from the try.
      tryStatement.accept(
          new AbstractRewriter() {
            @Override
            public Node rewriteReturnStatement(ReturnStatement returnStatement) {
              if (savedReturnValueVariable == null) {
                // This is a method that returns void.
                return rewriteExit(returnStatement);
              }
              return rewriteExit(
                  // Save the return expression value. Since this assignment will be executed at
                  // the location where the return was, it preserves the evaluation order by
                  // keeping the evaluation of the return expression in the original place.
                  BinaryExpression.Builder.asAssignmentTo(savedReturnValueVariable)
                      .setRightOperand(returnStatement.getExpression())
                      .build()
                      .makeStatement(returnStatement.getSourcePosition()),
                  // At the finally exit, the saved value will be returned.
                  ReturnStatement.Builder.from(returnStatement)
                      .setExpression(savedReturnValueVariable.createReference())
                      .build());
            }

            @Override
            public Statement rewriteContinueStatement(ContinueStatement continueStatement) {
              Label targetLabel = continueStatement.getLabelReference().getTarget();
              if (!isLabelEnclosingTry.test(targetLabel)) {
                return continueStatement;
              }
              return rewriteExit(continueStatement);
            }

            @Override
            public Statement rewriteBreakStatement(BreakStatement breakStatement) {
              Label targetLabel = breakStatement.getLabelReference().getTarget();
              if (!isLabelEnclosingTry.test(targetLabel)) {
                return breakStatement;
              }
              return rewriteExit(breakStatement);
            }
          });

      return tryStatement;
    }

    /**
     * Encloses the rewritten try-catch block with a try-catch statement to save exceptions that are
     * thrown to be able to rethrow later.
     */
    private TryStatement wrapTryToHandleExitsViaThrow(TryStatement rewrittenTryStatement) {
      var sourcePosition = originalTryStatement.getSourcePosition();

      // Exception variable declared in the catch clause of the wrapping try-catch.
      var throwableVariable =
          Variable.newBuilder()
              .setName("t")
              .setTypeDescriptor(TypeDescriptors.get().javaLangThrowable)
              .build();

      // Create the catchClause to handle the exceptional exits via throw.
      CatchClause exceptionExitCatchClause =
          CatchClause.newBuilder()
              .setExceptionVariable(throwableVariable)
              .setBody(
                  rewriteExit(
                      // Code to save the thrown exception to rethrow after finally.
                      BinaryExpression.Builder.asAssignmentTo(savedThrownVariable)
                          .setRightOperand(throwableVariable.createReference())
                          .build()
                          .makeStatement(sourcePosition),
                      // Code to execute after finally: throw the saved exception.
                      ThrowStatement.newBuilder()
                          .setSourcePosition(sourcePosition)
                          .setExpression(savedThrownVariable.createReference())
                          .build()))
              .build();

      return TryStatement.newBuilder()
          .setBody(rewrittenTryStatement)
          .setCatchClauses(exceptionExitCatchClause)
          .setSourcePosition(sourcePosition)
          .build();
    }

    private Statement rewriteExit(Statement statement) {
      return rewriteExit(Statement.createNoopStatement(), statement);
    }

    /**
     * Rewrites a statement for an exit of the try-finally into code that saves the values needed to
     * perform the exit, set the exit selector code and breaks out of the block to the code that
     * will execute the finally block.
     */
    private Block rewriteExit(Statement saveValueStatement, Statement exitStatement) {
      // Register the statement that needs to be executed at the end of the finally block.
      int exitSelector = registerExit(exitStatement);

      // Emit the caching of values and which exit to dispatch and break out of the try-finally
      // into the finally clause.
      return Block.newBuilder()
          .setStatements(
              saveValueStatement,
              BinaryExpression.Builder.asAssignmentTo(exitSelectorVariable)
                  .setRightOperand(NumberLiteral.fromInt(exitSelector))
                  .build()
                  .makeStatement(exitStatement.getSourcePosition()),
              BreakStatement.newBuilder()
                  .setLabelReference(finallyLabel.createReference())
                  .setSourcePosition(originalTryStatement.getSourcePosition())
                  .build())
          .build();
    }

    /** Registers an exit of a try-finally and assigns an exit selector value to it. */
    private int registerExit(Statement exitStatement) {
      exitStatements.add(exitStatement);
      return exitStatements.size();
    }

    /** Creates the code to perform the exit of the try finally block. */
    private Statement createExitSwitch() {
      SwitchStatement.Builder dispatchStatementBuilder =
          SwitchStatement.newBuilder()
              .setSwitchExpression(exitSelectorVariable.createReference())
              .setSourcePosition(originalTryStatement.getSourcePosition());
      // The normal control flow path has an exit selector of 0; returns, breaks, continues and
      // throws will have their own value starting from 1 and will have a branch in the switch.
      int exitSelectorValue = 1;
      for (Statement exitStatement : exitStatements) {
        dispatchStatementBuilder.addCases(
            SwitchCase.newBuilder()
                .setCaseExpression(NumberLiteral.fromInt(exitSelectorValue))
                .setStatements(exitStatement)
                .build());
        exitSelectorValue++;
      }
      return dispatchStatementBuilder.build();
    }

    /**
     * Creates the declaration for the variables that will hold the state to perform the exit from
     * the finally block.
     */
    private Statement createVariableDeclarations() {
      return VariableDeclarationExpression.newBuilder()
          .addVariableDeclarations(
              Stream.of(exitSelectorVariable, savedReturnValueVariable, savedThrownVariable)
                  .filter(Predicates.notNull())
                  .collect(toImmutableList()))
          .build()
          .makeStatement(originalTryStatement.getSourcePosition());
    }
  }
}
