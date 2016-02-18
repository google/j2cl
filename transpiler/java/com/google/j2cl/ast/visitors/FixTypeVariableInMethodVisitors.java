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
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.RegularTypeDescriptor;
import com.google.j2cl.ast.TypeDescriptor;

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
    return CastExpression.createRaw(
        castExpression.getExpression(),
        replaceTypeVariableWithBound(castTypeDescriptor, getCurrentMethod()));
  }

  private TypeDescriptor replaceTypeVariableWithBound(
      TypeDescriptor typeDescriptor, final Method method) {
    // If it is a type variable that is declared by the method, replace with its bound.
    if (typeDescriptor.isTypeVariable()
        && method != null
        && (method.getDescriptor().getTypeParameterTypeDescriptors().contains(typeDescriptor)
            || (method.getDescriptor().isJsFunction()))) {
      // A JsFunction method uses @this tag to specify the type of 'this', which makes templates
      // unresolved inside the method.
      // TODO: double check if this is the same issue with b/24476009.
      TypeDescriptor boundTypeDescriptor = typeDescriptor.getRawTypeDescriptor();
      return boundTypeDescriptor.isParameterizedType()
          ? boundTypeDescriptor.getRawTypeDescriptor()
          : boundTypeDescriptor;
    }
    if (typeDescriptor.isParameterizedType()) {
      RegularTypeDescriptor regularTypeDescriptor = (RegularTypeDescriptor) typeDescriptor;
      return TypeDescriptor.create(
          regularTypeDescriptor.getPackageComponents(),
          regularTypeDescriptor.getClassComponents(),
          regularTypeDescriptor.isRaw(),
          regularTypeDescriptor.isNative(),
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
}
