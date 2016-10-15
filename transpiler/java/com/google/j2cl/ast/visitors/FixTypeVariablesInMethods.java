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

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.Member;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Temporary workaround for b/24476009.
 *
 * <p>Template declared in a method is not resolved inside the method by JSCompiler. To get rid of
 * the warnings, this class replaces cast to a type variable that is declared by a method with cast
 * to its bound.
 */
public class FixTypeVariablesInMethods extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new Rewriter());
  }

  private class Rewriter extends AbstractRewriter {
    @Override
    public Node rewriteJsDocAnnotatedExpression(JsDocAnnotatedExpression annotation) {
      if (annotation.isDeclaration() || !getCurrentMember().isMethod()) {
        return annotation;
      }
      TypeDescriptor castTypeDescriptor = annotation.getTypeDescriptor();
      TypeDescriptor boundType =
          replaceTypeVariableWithBound(castTypeDescriptor, (Method) getCurrentMember());
      return JsDocAnnotatedExpression.createCastAnnotatedExpression(
          annotation.getExpression(), boundType);
    }

  }

  private TypeDescriptor replaceTypeVariableWithBound(
      TypeDescriptor typeDescriptor, final Method method) {
    // If it is a type variable that is declared by the method, replace with its bound.
    if (isTypeVariableDeclaredByMethod(typeDescriptor, method)) {
      TypeDescriptor boundTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
      return boundTypeDescriptor.isParameterizedType()
          ? boundTypeDescriptor.getRawTypeDescriptor()
          : boundTypeDescriptor;
    }
    // If it is a JsFunction type, and the JsFunction method contains type variables, we have to
    // replace it with 'window.Function' to get rid of 'bad type annotation' error.
    if (typeDescriptor.isJsFunctionImplementation() || typeDescriptor.isJsFunctionInterface()) {
      if (containsTypeVariableDeclaredByMethodInJsFunction(typeDescriptor, method)) {
        // Match the nullability of the starting type descriptor.
        return TypeDescriptors.toGivenNullability(
            TypeDescriptors.NATIVE_FUNCTION, typeDescriptor.isNullable());
      }
      return typeDescriptor;
    }
    if (typeDescriptor.isParameterizedType()) {
      return TypeDescriptors.replaceTypeArgumentDescriptors(
          typeDescriptor,
          Lists.transform(
              typeDescriptor.getTypeArgumentDescriptors(),
              typeArgument -> replaceTypeVariableWithBound(typeArgument, method)));
    }
    if (typeDescriptor.isArray()) {
      TypeDescriptor boundTypeDescriptor =
          replaceTypeVariableWithBound(typeDescriptor.getLeafTypeDescriptor(), method);
      return TypeDescriptors.getForArray(
          boundTypeDescriptor, typeDescriptor.getDimensions(), typeDescriptor.isNullable());
    }
    return typeDescriptor;
  }

  private boolean isTypeVariableDeclaredByMethod(TypeDescriptor typeDescriptor, Member member) {
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
      TypeDescriptor typeDescriptor, Method method) {
    if (isTypeVariableDeclaredByMethod(typeDescriptor, method)) {
      return true;
    }
    if (typeDescriptor.isParameterizedType()) {
      for (TypeDescriptor typeArgumentTypeDescriptor :
          typeDescriptor.getTypeArgumentDescriptors()) {
        if (containsTypeVariableDeclaredByMethod(typeArgumentTypeDescriptor, method)) {
          return true;
        }
      }
      return false;
    }
    if (typeDescriptor.isArray()) {
      return containsTypeVariableDeclaredByMethod(typeDescriptor.getLeafTypeDescriptor(), method);
    }
    return false;
  }

  private boolean containsTypeVariableDeclaredByMethodInJsFunction(
      TypeDescriptor typeDescriptor, Method method) {
    checkState(
        typeDescriptor.isJsFunctionImplementation() || typeDescriptor.isJsFunctionInterface());
    MethodDescriptor jsFunctionMethodDescriptor =
        typeDescriptor.getConcreteJsFunctionMethodDescriptor();
    if (jsFunctionMethodDescriptor != null) {
      for (TypeDescriptor parameterTypeDescriptor :
          jsFunctionMethodDescriptor.getParameterTypeDescriptors()) {
        if (containsTypeVariableDeclaredByMethod(parameterTypeDescriptor, method)) {
          return true;
        }
      }
      return containsTypeVariableDeclaredByMethod(
          jsFunctionMethodDescriptor.getReturnTypeDescriptor(), method);
    }
    return false;
  }
}
