package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.JsTypeAnnotation;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.Node;

/**
 * Inserts a cast for the return type of methods which return generic types. This avoids a potential
 * error in JSCompiler where the combination of type tightening and invariant generics can lead to
 * mismatched types.
 *
 * <p>This should not be necessary when using new type inference in JSCompiler.
 */
public class InsertTypeAnnotationOnGenericReturnTypes extends AbstractRewriter {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new InsertTypeAnnotationOnGenericReturnTypes());
  }

  @Override
  public boolean shouldProcessJsTypeAnnotation(JsTypeAnnotation annotation) {
    if (annotation.getExpression() instanceof MethodCall) {
      MethodCall methodCall = (MethodCall) annotation.getExpression();
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
      return JsTypeAnnotation.createTypeAnnotation(methodCall, methodCall.getTypeDescriptor());
    }
    return methodCall;
  }
}
