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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CastExpressionBuilder;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Temporary workaround for b/24476009.
 *
 * <p> Template declared in a method is not resolved inside the method by JSCompiler. To get rid of
 * the warnings, this class replaces cast to a type variable that is declared by a method with
 * cast to its bound.
 */
public class FixTypeVariableInMethodVisitors extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new FixTypeVariableInMethodVisitors().fixTypeVariableInMethod(compilationUnit);
  }

  private void fixTypeVariableInMethod(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteCastExpression(CastExpression castExpression) {
    Preconditions.checkArgument(
        castExpression.isRaw(),
        "FixTypeVariableInMethodVisitors should be run after NormalizeCastsVisitor.");
    TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
    return CastExpressionBuilder.from(castExpression)
        .castTypeDescriptor(replaceTypeVariableWithBound(castTypeDescriptor, getCurrentMethod()))
        .build();
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
        return TypeDescriptors.NATIVE_FUNCTION;
      }
      return typeDescriptor;
    }
    if (typeDescriptor.isParameterizedType()) {
      RegularTypeDescriptor regularTypeDescriptor = (RegularTypeDescriptor) typeDescriptor;
      return TypeDescriptor.createSyntheticParametricTypeDescriptor(
          regularTypeDescriptor,
          Lists.transform(
              regularTypeDescriptor.getTypeArgumentDescriptors(),
              new Function<TypeDescriptor, TypeDescriptor>() {
                @Override
                public TypeDescriptor apply(TypeDescriptor typeArgument) {
                  // Replace type variable in the arguments.
                  return replaceTypeVariableWithBound(typeArgument, method);
                }
              }));
    }
    if (typeDescriptor.isArray()) {
      return replaceTypeVariableWithBound(typeDescriptor.getLeafTypeDescriptor(), method)
          .getForArray(typeDescriptor.getDimensions());
    }
    return typeDescriptor;
  }

  private boolean isTypeVariableDeclaredByMethod(TypeDescriptor typeDescriptor, Method method) {
    // A JsFunction method uses @this tag to specify the type of 'this', which makes templates
    // unresolved inside the method.
    // TODO: double check if this is the same issue with b/24476009.
    return typeDescriptor.isTypeVariable()
        && method != null
        && (method.getDescriptor().getTypeParameterTypeDescriptors().contains(typeDescriptor)
            || (method.getDescriptor().isJsFunction()));
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
    Preconditions.checkState(
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
