package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.Node;

/**
 * Inserts a cast for the return type of methods which return generic types. This avoids a potential
 * error in JSCompiler where the combination of type tightening and invariant generics can lead to
 * mismatched types.
 *
 * <p>This should not be necessary when using new type inference in JSCompiler.
 */
public class InsertCastOnGenericReturnTypeVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new InsertCastOnGenericReturnTypeVisitor().insertCastOnInferredType(compilationUnit);
  }

  private void insertCastOnInferredType(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public boolean shouldProcessCastExpression(CastExpression castExpression) {
    if (castExpression.getExpression() instanceof MethodCall) {
      MethodCall methodCall = (MethodCall) castExpression.getExpression();
      for (Expression expression : methodCall.getArguments()) {
        // process arguments
        expression.accept(this);
      }
      // Don't rewrite the castExpression nor its sub expressions.
      return false;
    }

    return true;
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    if (methodCall.getTarget().getReturnTypeDescriptor().isParameterizedType()) {
      return CastExpression.createRaw(methodCall, methodCall.getTypeDescriptor());
    }
    return methodCall;
  }
}
