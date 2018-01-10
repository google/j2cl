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

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.IntersectionTypeDescriptor;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeVariable;
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
 * <code>
 *  class A<T extends Comparable<T> & CharSequence> {
 *    CharSequence g(T t) {
 *      Object o = (A & B) t;
 *      t.compareTo(null);  // Comparable method
 *      t.length();  // CharSequence method
 *      return t;
 *    }
 *  }
 * </code>
 * </pre>
 *
 * <p>gets translated as
 *
 * <p>
 *
 * <pre>
 * <code>
 *  class A<T extends Comparable<T>> {
 *    CharSequence g(T t) {
 *      Object o = /** @type {A} * / (B)(A) t;
 *      t.compareTo(null);
 *      ((CharSequence) t).length();
 *      return (CharSequence) t;
 *    }
 *  }
 * </code>
 * </pre>
 *
 * <p>The interesting case where type erasure casts can not be inserted is as follows:
 *
 * <p>
 *
 * <pre>
 * <code>
 * interface F<T extends A> {
 *   void do(T t);
 * }
 *
 * class C<T extends A & B> {
 *   void do(T t) { t.methodOfB(); }
 * }
 * </code>
 * </pre>
 *
 * <p>At runtime code like:
 *
 * <p>
 *
 * <pre>
 * <code>
 *   ((F) new C()).do(new A());
 * </code>
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
                  ParameterDescriptor parameterDescriptor, Expression argumentExpression) {
                return maybeInsertCastToMemberType(
                    parameterDescriptor.getTypeDescriptor(), argumentExpression);
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

    // Replace intersection casts with a sequence of type casts, i.e. transform (A & B) into (B)(A).
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
            // innermost cast.
            IntersectionTypeDescriptor intersectionTypeDescriptor =
                (IntersectionTypeDescriptor) castTypeDescriptor;
            for (TypeDescriptor intersectedTypeDescriptor :
                intersectionTypeDescriptor.getIntersectionTypeDescriptors()) {
              expression =
                  CastExpression.newBuilder()
                      .setExpression(expression)
                      .setCastTypeDescriptor(intersectedTypeDescriptor)
                      .build();
            }
            // Annotate the expression so that is typed (in closure) with the first type in the
            // intersection. Intersection types do not have a direct representation in closure.
            // Since in general we need a consistent view of the closure type of expressions,
            // we chose the to see expressions typed at an intersection cast as being explicitly
            // typed at the first component, which is also consistent with the JVM type (the erasure
            // of an intersection cast is the erasure of its first component).
            return JsDocCastExpression.newBuilder()
                .setCastType(intersectionTypeDescriptor.getFirstType())
                .setExpression(expression)
                .build();
          }
        });
  }

  @SuppressWarnings("ReferenceEquality")
  private static Expression maybeInsertCastToMemberType(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    if (toTypeDescriptor instanceof TypeVariable
        // TODO(b/68885310): primitives should be handled below.
        || toTypeDescriptor.isPrimitive()) {
      // Do not insert the runtime cast if the destination is not a declared type. For example if
      // the destination type is a type variable the corresponding cast will be inserted by the
      // pass that inserts casts due to type erasure.
      return expression;
    }

    TypeDescriptor expressionTypeDescriptor = expression.getTypeDescriptor();
    List<DeclaredTypeDescriptor> intersectedTypeDescriptors =
        maybeGetIntersectedTypeDescriptors(expressionTypeDescriptor);
    if (intersectedTypeDescriptors == null) {
      return expression;
    }

    TypeDescriptor targetInstanceTypeDescriptor =
        intersectedTypeDescriptors
            .stream()
            .filter(typeDescriptor -> typeDescriptor.isAssignableTo(toTypeDescriptor))
            .findFirst()
            .get();

    if (targetInstanceTypeDescriptor == intersectedTypeDescriptors.get(0)) {
      // No need to cast to the first member of the intersection type as this will be the type
      // for expression already.
      return expression;
    }

    if (expressionTypeDescriptor instanceof TypeVariable) {
      // A cast is needed because there is no guarantee that this check has already been performed
      // when going from a type variable with an intersection type as an upper bound to a member.
      return CastExpression.newBuilder()
          .setExpression(expression)
          .setCastTypeDescriptor(toTypeDescriptor)
          .build();
    }

    // No cast needed. Going from an intersection to a member type just requires a type annotation.
    return JsDocCastExpression.newBuilder()
        .setExpression(expression)
        .setCastType(targetInstanceTypeDescriptor)
        .build();
  }

  private static List<DeclaredTypeDescriptor> maybeGetIntersectedTypeDescriptors(
      TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isIntersection()) {
      return ((IntersectionTypeDescriptor) typeDescriptor).getIntersectionTypeDescriptors();
    }

    if (typeDescriptor instanceof TypeVariable) {
      return maybeGetIntersectedTypeDescriptors(
          ((TypeVariable) typeDescriptor).getBoundTypeDescriptor());
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
        expression.getTarget().getEnclosingTypeDescriptor();

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
