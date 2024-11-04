/*
 * Copyright 2017 Google Inc.
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

import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;

/**
 * Provide more precise type to jscompiler by annotating expressions that are typed at type
 * variables (bounded) where a more precise type can be inferred. (This pass assumes that that
 * passes that introduce constructs that need to be handled, like unboxing, are already performed).
 *
 * <p>Closure's type system lacks a way to specify constraints on type variables (aka templates),
 * which means that there is loss of type information when expressing Java's bounded type variables.
 * For example code like:
 *
 * <pre>
 * <code>{@code
 *   class A< T extends List<?>> {
 *     void clear(T list) {
 *       list.clear();
 *     }
 *   }
 * }</code>
 * </pre>
 *
 * <p>would get emitted as:
 *
 * <pre>
 * <code>
 *   /**
 *    * @template {T}
 *    * /
 *   class A {
 *     /**
 *      * @param {T} l
 *      * /
 *     clear(list) {
 *       list.clear();
 *     }
 *   }
 * </code>
 * </pre>
 *
 * <p>Note that now the type of the parameter {@code list} in {@code clear} is not inferred by
 * closure and is considered not typed. This has detrimental effects for property disambiguation,
 * etc.
 *
 * <p>This pass remedies the issue by always inserting a closure cast when a more precise type can
 * inferred, e.g when calling a method or accessing a property of a variable typed at {@code T
 * extends ... }:
 *
 * <pre>
 * <code>{@code
 *   /**
 *    * @template {T}
 *    * /
 *   class A {
 *     /**
 *      * @param {T} list
 *      * /
 *     clear(list) {
 *       return /** @type {List<?>} * / (list).clear();
 *     }
 *   }
 * }</code>
 * </pre>
 */
public class InsertJsDocCastsToTypeBounds extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                return maybeAddJsDocAnnotation(inferredTypeDescriptor, expression);
              }
            }));
  }

  private static Expression maybeAddJsDocAnnotation(
      TypeDescriptor inferredType, Expression expression) {

    TypeDescriptor typeDescriptor = expression.getTypeDescriptor();
    if (isBoundedTypeVariable(typeDescriptor)
        && !inferredType.isTypeVariable()
        && !inferredType.isIntersection()
        && !TypeDescriptors.isJavaLangObject(inferredType)) {
      return JsDocCastExpression.newBuilder()
          .setCastTypeDescriptor(inferredType)
          .setExpression(expression)
          .build();
    }

    return expression;
  }

  private static boolean isBoundedTypeVariable(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isTypeVariable()) {
      // Quick check to see if there is really a bound.
      return !TypeDescriptors.isJavaLangObject(typeDescriptor.toRawTypeDescriptor());
    }
    return false;
  }
}
