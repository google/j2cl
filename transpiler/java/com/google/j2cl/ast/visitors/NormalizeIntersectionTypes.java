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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.JsDocAnnotatedExpression;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import java.util.List;

/**
 * Intersection types can not be directly represented in the closure type system. This pass
 * (following the ideas in the Java type erasure process) conceptualizes the intersection types as
 * being the type of the first one in the intersection, intersection casts into a series of simple
 * casts, introduces erasure style cast when implicitly going from type variables with intersection
 * type upper bounds to intersection member types and introduces type annotations when going from an
 * intersection type to one of its member types.
 *
 * <p>
 *
 * <pre>
 *  class A<T extends Comparable<T> & CharSequence> {
 *    CharSequence g(T t) {
 *      Object o = (A & B) t;
 *      t.compareTo(null);  // Comparable method
 *      t.length();  // CharSequence method
 *      return t;
 *    }
 *  }
 * </pre>
 *
 * <p>gets translated as
 *
 * <p>
 *
 * <pre>
 *  class A<T extends Comparable<T>> {
 *    CharSequence g(T t) {
 *      Object o = (A)(B) t;
 *      t.compareTo(null);
 *      ((CharSequence) t).length();
 *      return (CharSequence) t;
 *    }
 *  }
 * </pre>
 *
 * <p>The interesting case where type erasure casts can not be inserted is as follows:
 *
 * <p>
 *
 * <pre>
 * interface F<T extends A> {
 *   void do(T t);
 * }
 *
 * class C<T extends A & B> {
 *   void do(T t) { t.methodOfB(); }
 * }
 * </pre>
 *
 * <p>At runtime code like:
 *
 * <p>
 *
 * <pre>
 *   ((F) new C()).do(new A());
 * </pre>
 *
 * <p>should fail with a ClassCastException. Normally a bridge would have been constructed to
 * delegate F.do instrumented with the necessary casts. But in this situation F.do has the same
 * erasure as C.do due to the erasure rules for intersection. The only other option is to instrument
 * at the usage time like javac does.
 */
public final class NormalizeIntersectionTypes extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Add casts at the required context sites.
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ConversionContextVisitor.ContextRewriter() {
              @Override
              public Expression rewriteAssignmentContext(
                  TypeDescriptor toTypeDescriptor, Expression expression) {
                return maybeInsertCastToMemberType(toTypeDescriptor, expression);
              }

              @Override
              public Expression rewriteMethodInvocationContext(
                  TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
                return maybeInsertCastToMemberType(parameterTypeDescriptor, argumentExpression);
              }
            }));

    // Add casts at method calls and field references.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(final FieldAccess fieldAccess) {
            return maybeInsertCastToMemberType(fieldAccess);
          }

          @Override
          public Node rewriteMethodCall(final MethodCall methodCall) {
            return maybeInsertCastToMemberType(methodCall);
          }
        });

    // Replace intersection casts with a sequence of type casts, i.e. transform (A & B) into (A)(B).
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteCastExpression(final CastExpression castExpression) {
            TypeDescriptor castTypeDescriptor = castExpression.getCastTypeDescriptor();
            if (!castTypeDescriptor.isIntersection()) {
              return castExpression;
            }

            Expression expression = castExpression.getExpression();
            // Emit the casts so that the first type in the intersection corresponds to the
            // outermost cast.
            for (TypeDescriptor intersectedTypeDescriptor :
                Lists.reverse(castTypeDescriptor.getIntersectedTypeDescriptors())) {
              expression =
                  CastExpression.newBuilder()
                      .setExpression(expression)
                      .setCastTypeDescriptor(intersectedTypeDescriptor)
                      .build();
            }
            return expression;
          }
        });
  }

  private static Expression maybeInsertCastToMemberType(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    if (!toTypeDescriptor.isClass()
        && !toTypeDescriptor.isInterface()
        && !toTypeDescriptor.isEnum()) {
      // Do not insert the runtime cast if the destination is not a declared type. For example if
      // the destination type is a type variable the corresponding cast will be inserted by the
      // pass that inserts casts due to type erasure.
      return expression;
    }

    TypeDescriptor expressionTypeDescriptor = expression.getTypeDescriptor();
    List<TypeDescriptor> intersectedTypeDescriptors =
        maybeGetIntersectedTypeDescriptors(expressionTypeDescriptor);
    if (intersectedTypeDescriptors == null) {
      return expression;
    }

    TypeDescriptor targetInstanceTypeDescriptor =
        intersectedTypeDescriptors
            .stream()
            .filter(toTypeDescriptor::isSupertypeOf)
            .findFirst()
            .get();

    if (targetInstanceTypeDescriptor == intersectedTypeDescriptors.get(0)) {
      // No need to cast to the first member of the intersection type as this will be the type
      // for expression already.
      return expression;
    }

    if (expressionTypeDescriptor.isTypeVariable()
        || expressionTypeDescriptor.isWildCardOrCapture()) {
      // A cast is needed because there is no guarantee that this check has already been performed
      // when going from a type variable with an intersection type as an upper bound to a member.
      return CastExpression.newBuilder()
          .setExpression(expression)
          .setCastTypeDescriptor(toTypeDescriptor)
          .build();
    }

    // No cast needed. Going from an intersection to a member type just requires a type annotation.
    return JsDocAnnotatedExpression.newBuilder()
        .setExpression(expression)
        .setAnnotationType(targetInstanceTypeDescriptor)
        .build();
  }

  private static List<TypeDescriptor> maybeGetIntersectedTypeDescriptors(
      TypeDescriptor expressionTypeDescriptor) {
    if (expressionTypeDescriptor.isIntersection()) {
      return expressionTypeDescriptor.getIntersectedTypeDescriptors();
    }

    if ((expressionTypeDescriptor.isTypeVariable()
            || expressionTypeDescriptor.isWildCardOrCapture())
        && expressionTypeDescriptor.getBoundTypeDescriptor() != null) {
      return maybeGetIntersectedTypeDescriptors(expressionTypeDescriptor.getBoundTypeDescriptor());
    }

    return null;
  }

  private static <T extends Expression & MemberReference> Expression maybeInsertCastToMemberType(
      T expression) {
    if (expression.getQualifier() == null) {
      return expression;
    }

    Expression qualifier = expression.getQualifier();
    final TypeDescriptor requiredTypeDescriptor =
        expression.getTarget().getEnclosingClassTypeDescriptor();

    // This is a member reference to a method or field on a type that is not the first type
    // in the intersection type, a cast is needed.
    Expression newQualifier = maybeInsertCastToMemberType(requiredTypeDescriptor, qualifier);

    if (newQualifier == qualifier) {
      // No cast added to the qualifier, just return the expression.
      return expression;
    }

    if (expression instanceof MethodCall) {
      return MethodCall.Builder.from((MethodCall) expression).setQualifier(newQualifier).build();
    } else {
      checkArgument(expression instanceof FieldAccess);
      return FieldAccess.Builder.from((FieldAccess) expression).setQualifier(newQualifier).build();
    }
  }
}
