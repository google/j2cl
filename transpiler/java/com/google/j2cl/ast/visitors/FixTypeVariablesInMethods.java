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
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.VariableDeclarationExpression;
import java.util.function.Predicate;

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
 * by their bound. Note that the AST construct of interest is {@link JsDocAnnotatedExpression}
 * because this is the only one that can result in a template variable reference in the output.
 */
public class FixTypeVariablesInMethods extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Rewrite method bodies to avoid references to template variables declared by the method (1).
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteJsDocAnnotatedExpression(JsDocAnnotatedExpression annotation) {
            if (annotation.isDeclaration() || !getCurrentMember().isMethod()) {
              return annotation;
            }
            TypeDescriptor castTypeDescriptor = annotation.getTypeDescriptor();
            TypeDescriptor boundType =
                replaceTypeVariableWithBound(
                    castTypeDescriptor,
                    typeDescriptor ->
                        isTypeVariableDeclaredByMember(typeDescriptor, getCurrentMember()));
            return JsDocAnnotatedExpression.newBuilder()
                .setExpression(annotation.getExpression())
                .setAnnotationType(boundType)
                .build();
          }
        });

    // Rewrite method bodies of anonymous functions to avoid references to template variables (2);
    // anonymous functions in the compiled output are a result of @JsFunction lambdas and
    // multiexpressions that declared variables.
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitFunctionExpression(FunctionExpression functionExpression) {
            removeAllTypeVariables(functionExpression);
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

  private void removeAllTypeVariables(Expression functionExpression) {
    functionExpression.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteJsDocAnnotatedExpression(JsDocAnnotatedExpression annotation) {
            TypeDescriptor castTypeDescriptor = annotation.getTypeDescriptor();
            TypeDescriptor boundType =
                replaceTypeVariableWithBound(castTypeDescriptor, Predicates.alwaysTrue());
            return JsDocAnnotatedExpression.newBuilder()
                .setExpression(annotation.getExpression())
                .setAnnotationType(boundType)
                .build();
          }
        });
  }

  private TypeDescriptor replaceTypeVariableWithBound(
      TypeDescriptor typeDescriptor, Predicate<TypeDescriptor> shouldBeReplaced) {
    // If it is a type variable that is declared by the method, replace with its bound.
    if (shouldBeReplaced.test(typeDescriptor)) {
      TypeDescriptor boundTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
      return boundTypeDescriptor.isParameterizedType()
          ? boundTypeDescriptor.getRawTypeDescriptor()
          : boundTypeDescriptor;
    }
    // If it is a JsFunction type, and the JsFunction method contains type variables, we have to
    // replace it with 'window.Function' to get rid of 'bad type annotation' error.
    if (typeDescriptor.isJsFunctionImplementation() || typeDescriptor.isJsFunctionInterface()) {
      if (containsTypeVariableDeclaredByMethodInJsFunction(typeDescriptor, shouldBeReplaced)) {
        return TypeDescriptors.NATIVE_FUNCTION;
      }
      return typeDescriptor;
    }
    if (typeDescriptor.isParameterizedType()) {
      return TypeDescriptors.replaceTypeArgumentDescriptors(
          typeDescriptor,
          Lists.transform(
              typeDescriptor.getTypeArgumentDescriptors(),
              typeArgument -> replaceTypeVariableWithBound(typeArgument, shouldBeReplaced)));
    }
    if (typeDescriptor.isArray()) {
      TypeDescriptor boundTypeDescriptor =
          replaceTypeVariableWithBound(typeDescriptor.getLeafTypeDescriptor(), shouldBeReplaced);
      return TypeDescriptors.getForArray(
          boundTypeDescriptor, typeDescriptor.getDimensions(), typeDescriptor.isNullable());
    }
    return typeDescriptor;
  }

  private static boolean isTypeVariableDeclaredByMember(
      TypeDescriptor typeDescriptor, Member member) {
    // A JsFunction method uses @this tag to specify the type of 'this', which makes templates
    // unresolved inside the method.
    // TODO: double check if this is the same issue with b/24476009.
    return typeDescriptor.isTypeVariable()
        && member.isMethod()
        && (((Method) member)
                .getDescriptor()
                .getTypeParameterTypeDescriptors()
                .contains(TypeDescriptors.toNonNullable(typeDescriptor))
            || (((Method) member).getDescriptor().isJsFunction()));
  }

  /**
   * Checks if the given type descriptor contains a type variable that is declared by the given
   * method. For a parameterized type descriptor, we check its type arguments recursively. For
   * example, Foo<Bar<T>> contains type variable 'T'.
   */
  private boolean containsTypeVariableDeclaredByMethod(
      TypeDescriptor typeDescriptor, Predicate<TypeDescriptor> shouldBeReplaced) {
    if (shouldBeReplaced.test(typeDescriptor)) {
      return true;
    }
    if (typeDescriptor.isParameterizedType()) {
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

  private boolean containsTypeVariableDeclaredByMethodInJsFunction(
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
}
