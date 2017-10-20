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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * Provide more precise type to jscompiler by annotating expressions that are typed at a bounded
 * type variable on method calls and field accesses. (This pass assumes that intersection types have
 * already been normalized and that passes that introduce constructs that need to be handled, like
 * unboxing, are already performed).
 *
 * <p>Closure's type system lacks a way to specify constraints on type variables (aka templates),
 * which means that there is loss of type information when expressing Java's bounded type variables.
 * For example code like:
 *
 * <pre>
 * <code>
 *   class A< T extends List<?>> {
 *     void clear(T list) {
 *       list.clear();
 *     }
 *   }
 * </code>
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
 * <p>This pass remedies the issue by always inserting a closure cast when calling a method or
 * accessing a property of a variable typed at {@code T extends ... }. E.g:
 *
 * <pre>
 * <code>
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
 * </code>
 * </pre>
 */
public class InsertCastsToTypeBounds extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public FieldAccess rewriteFieldAccess(FieldAccess fieldAccess) {
            Expression qualifier =
                maybeAddJsDocAnnotation(fieldAccess.getTarget(), fieldAccess.getQualifier());
            // /** @type {Bound} */ (a).f */
            return FieldAccess.Builder.from(fieldAccess).setQualifier(qualifier).build();
          }

          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            Expression qualifier =
                maybeAddJsDocAnnotation(methodCall.getTarget(), methodCall.getQualifier());
            // /** @type {Bound} */ (a).m() */
            return MethodCall.Builder.from(methodCall).setQualifier(qualifier).build();
          }
        });
  }

  private static Expression maybeAddJsDocAnnotation(MemberDescriptor target, Expression qualifier) {
    if (!target.isPolymorphic()) {
      // Nothing to do for static methods or constructors. For static methods the qualifier is an
      // explicit constructor reference, whereas constructors do not have a qualifier at all so
      // cannot be a bounded type variable.
      return qualifier;
    }

    TypeDescriptor typeDescriptor = qualifier.getTypeDescriptor();
    if (isBoundedTypeVariable(typeDescriptor)) {
      return JsDocAnnotatedExpression.newBuilder()
          .setAnnotationType(typeDescriptor.getRawTypeDescriptor())
          .setExpression(qualifier)
          .build();
    }

    return qualifier;
  }

  private static boolean isBoundedTypeVariable(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isTypeVariable() || typeDescriptor.isWildCardOrCapture()) {
      // If there is a bound, getRawTypeDescriptor returns its erasure. Note that intersection types
      // have already been handled and we only care about the first type.
      return !TypeDescriptors.isJavaLangObject(typeDescriptor.getRawTypeDescriptor());
    }
    return false;
  }
}
