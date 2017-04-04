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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
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
import com.google.j2cl.ast.TypeDescriptors;
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
                methodDescriptor
                    .getTypeParameterTypeDescriptors()
                    .contains(TypeDescriptors.toNonNullable(typeDescriptor));

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
            replaceTypeInFunctionExpressionParameters(functionExpression, Predicates.alwaysTrue());
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
    node.accept(new RewriteTypeVariablesInJsDocAnnotations(Predicates.alwaysTrue()));
  }

  private static TypeDescriptor replaceTypeVariableWithBound(
      TypeDescriptor typeDescriptor, Predicate<TypeDescriptor> shouldBeReplaced) {
    switch (typeDescriptor.getKind()) {
      case TYPE_VARIABLE:
      case WILDCARD_OR_CAPTURE:
        // If it is a type variable that is declared by the method, replace with its bound.
        if (shouldBeReplaced.test(typeDescriptor)) {
          TypeDescriptor boundTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
          return boundTypeDescriptor.hasTypeArguments()
              ? boundTypeDescriptor.getRawTypeDescriptor()
              : boundTypeDescriptor;
        }
        return typeDescriptor;
      case ARRAY:
        TypeDescriptor replacedLeafTypeDescriptor =
            replaceTypeVariableWithBound(typeDescriptor.getLeafTypeDescriptor(), shouldBeReplaced);
        return TypeDescriptors.getForArray(
            replacedLeafTypeDescriptor,
            typeDescriptor.getDimensions(),
            typeDescriptor.isNullable());
      case ENUM:
      case CLASS:
      case INTERFACE:
        // If it is a JsFunction type, and the JsFunction method contains type variables, we have to
        // replace it with 'window.Function' to get rid of 'bad type annotation' error.
        if (typeDescriptor.isJsFunctionImplementation() || typeDescriptor.isJsFunctionInterface()) {
          if (containsTypeVariableDeclaredByMethodInJsFunction(typeDescriptor, shouldBeReplaced)) {
            return TypeDescriptors.NATIVE_FUNCTION;
          }
        } else if (typeDescriptor.hasTypeArguments()) {
          return TypeDescriptors.replaceTypeArgumentDescriptors(
              typeDescriptor,
              Lists.transform(
                  typeDescriptor.getTypeArgumentDescriptors(),
                  typeArgument -> replaceTypeVariableWithBound(typeArgument, shouldBeReplaced)));
        }
        return typeDescriptor;
      case UNION:
        return TypeDescriptors.createUnion(
            typeDescriptor
                .getUnionedTypeDescriptors()
                .stream()
                .map(
                    unionTypeDescriptor ->
                        replaceTypeVariableWithBound(unionTypeDescriptor, shouldBeReplaced))
                .collect(Collectors.toList()),
            typeDescriptor.getSuperTypeDescriptor());
      case INTERSECTION:
        throw new AssertionError();
      default:
        return typeDescriptor;
    }
  }

  /**
   * Checks if the given type descriptor contains a type variable that is declared by the given
   * method. For a parameterized type descriptor, we check its type arguments recursively. For
   * example, Foo<Bar<T>> contains type variable 'T'.
   */
  private static boolean containsTypeVariableDeclaredByMethod(
      TypeDescriptor typeDescriptor, Predicate<TypeDescriptor> shouldBeReplaced) {
    if (shouldBeReplaced.test(typeDescriptor)) {
      return true;
    }
    if (typeDescriptor.hasTypeArguments()) {
      for (TypeDescriptor typeArgumentTypeDescriptor :
          typeDescriptor.getTypeArgumentDescriptors()) {
        if (containsTypeVariableDeclaredByMethod(typeArgumentTypeDescriptor, shouldBeReplaced)) {
          return true;
        }
      }
      return false;
    }
    if (typeDescriptor.isArray()) {
      return containsTypeVariableDeclaredByMethod(
          typeDescriptor.getLeafTypeDescriptor(), shouldBeReplaced);
    }
    return false;
  }

  private static boolean containsTypeVariableDeclaredByMethodInJsFunction(
      TypeDescriptor typeDescriptor, Predicate<TypeDescriptor> shouldBeReplaced) {
    checkState(
        typeDescriptor.isJsFunctionImplementation() || typeDescriptor.isJsFunctionInterface());
    MethodDescriptor jsFunctionMethodDescriptor =
        typeDescriptor.getConcreteJsFunctionMethodDescriptor();
    if (jsFunctionMethodDescriptor != null) {
      for (TypeDescriptor parameterTypeDescriptor :
          jsFunctionMethodDescriptor.getParameterTypeDescriptors()) {
        if (containsTypeVariableDeclaredByMethod(parameterTypeDescriptor, shouldBeReplaced)) {
          return true;
        }
      }
      return containsTypeVariableDeclaredByMethod(
          jsFunctionMethodDescriptor.getReturnTypeDescriptor(), shouldBeReplaced);
    }
    return false;
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
      TypeDescriptor boundType = replaceTypeVariableWithBound(castTypeDescriptor, shouldBeReplaced);
      return JsDocAnnotatedExpression.newBuilder()
          .setExpression(annotation.getExpression())
          .setAnnotationType(boundType)
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
