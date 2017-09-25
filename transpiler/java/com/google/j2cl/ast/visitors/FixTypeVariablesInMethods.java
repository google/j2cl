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

import com.google.common.base.Predicates;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Temporary workaround for b/24476009.
 *
 * <p>1) Template variables declared on a method do not resolve inside the method body. Code inside
 * the method body can only refer explicitly to template variables declared in the class
 * declaration.
 *
 * <p>2) Template variables do not resolve within the body of anonymous functions.
 *
 * <p>To avoid warnings by JsCompiler this pass replaces these "unreferenceable" template variables
 * by their bound. Note that there are only two AST construct of interest: {@link
 * JsDocAnnotatedExpression} and {@link FunctionExpression} because these are the only ones that
 * emit JsDoc inline (and therefore can produce template references).
 */
public class FixTypeVariablesInMethods extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Rewrite method bodies to avoid references to template variables declared by the method (1).
    for (Type type : compilationUnit.getTypes()) {
      for (Method method : type.getMethods()) {
        MethodDescriptor methodDescriptor = method.getDescriptor();
        Predicate<TypeDescriptor> shouldBeReplaced =
            typeDescriptor ->
                // TODO(b/37482332): Synthesized method (like bridges) may contain references to
                // type variables that are not in the enclosing scope (e.g. a type variable
                // introduced by the method that cause the bridge). When those method bodies are
                // specialized correctly there should be no need to filter type variables that
                // that are not declared by the class.
                (typeDescriptor.isTypeVariable()
                        && !type.getDeclaration()
                            .getTypeParameterDescriptors()
                            .contains(typeDescriptor))
                    || methodDescriptor.getTypeParameterTypeDescriptors().contains(typeDescriptor)
                    // JsFunction methods are annotated with @this {function(...):...} and loose
                    // the ability to refer to type variables declared in the class.
                    || methodDescriptor.isJsFunction();

        method.accept(new RewriteTypeVariablesInJsDocAnnotations(shouldBeReplaced));
      }
    }

    // Rewrite method bodies of anonymous functions to avoid references to template variables (2);
    // anonymous functions in the compiled output are a result of @JsFunction lambdas and
    // multiexpressions that declared variables.
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitFunctionExpression(FunctionExpression functionExpression) {
            removeAllTypeVariables(functionExpression.getBody());
            replaceTypeInFunctionExpressionParameters(
                functionExpression,
                typeDescriptor ->
                    typeDescriptor.isTypeVariable() || typeDescriptor.isWildCardOrCapture());
          }

          @Override
          public void exitMultiExpression(MultiExpression multiExpression) {
            if (multiExpression
                .getExpressions()
                .stream()
                .anyMatch(Predicates.instanceOf(VariableDeclarationExpression.class))) {
              removeAllTypeVariables(multiExpression);
            }
          }
        });
  }

  private static FunctionExpression replaceTypeInFunctionExpressionParameters(
      FunctionExpression functionExpression,
      Predicate<TypeDescriptor> isTypeVariableDeclaredByCurrentMember) {
    return AstUtils.replaceVariables(
        functionExpression.getParameters(),
        functionExpression
            .getParameters()
            .stream()
            .map(
                variable ->
                    Variable.Builder.from(variable)
                        .setTypeDescriptor(
                            replaceTypeVariableWithBound(
                                variable.getTypeDescriptor(),
                                isTypeVariableDeclaredByCurrentMember))
                        .build())
            .collect(Collectors.toList()),
        functionExpression);
  }

  private void removeAllTypeVariables(Node node) {
    node.accept(
        new RewriteTypeVariablesInJsDocAnnotations(
            typeDescriptor -> typeDescriptor.isTypeVariable()));
  }

  private static TypeDescriptor replaceTypeVariableWithBound(
      TypeDescriptor typeDescriptor, Predicate<TypeDescriptor> shouldBeReplaced) {
    return typeDescriptor.specializeTypeVariables(
        t -> shouldBeReplaced.test(t) ? t.getRawTypeDescriptor() : t);
  }

  /**
   * Replaces all occurrences or type variables that match {@code shouldBeReplaced} by their upper
   * bounds raw types for types that will appear in JsDoc annotations.
   */
  private static class RewriteTypeVariablesInJsDocAnnotations extends AbstractRewriter {

    private final Predicate<TypeDescriptor> shouldBeReplaced;

    private RewriteTypeVariablesInJsDocAnnotations(Predicate<TypeDescriptor> shouldBeReplaced) {
      this.shouldBeReplaced = shouldBeReplaced;
    }

    @Override
    public Node rewriteJsDocAnnotatedExpression(JsDocAnnotatedExpression annotation) {
      if (annotation.isDeclaration()) {
        return annotation;
      }
      TypeDescriptor castTypeDescriptor = annotation.getTypeDescriptor();
      TypeDescriptor replacedTypeDescriptor =
          replaceTypeVariableWithBound(castTypeDescriptor, shouldBeReplaced);
      return JsDocAnnotatedExpression.newBuilder()
          .setExpression(annotation.getExpression())
          .setAnnotationType(replacedTypeDescriptor)
          .build();
    }

    @Override
    public Expression rewriteFunctionExpression(FunctionExpression functionExpression) {
      return replaceTypeInFunctionExpressionParameters(functionExpression, shouldBeReplaced);
    }

    @Override
    public VariableDeclarationFragment rewriteVariableDeclarationFragment(
        VariableDeclarationFragment variableDeclarationFragment) {
      Variable variable = variableDeclarationFragment.getVariable();
      variable.setTypeDescriptor(
          replaceTypeVariableWithBound(variable.getTypeDescriptor(), shouldBeReplaced));
      return variableDeclarationFragment;
    }
  }
}
