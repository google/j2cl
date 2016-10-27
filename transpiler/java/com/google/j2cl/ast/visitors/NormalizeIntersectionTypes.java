
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;
import java.util.List;

/**
 * Intersection types can not be directly represented in the closure type system. This pass
 * (following the ideas in the Java type erasure process) conceptualizes the intersection types as
 * being the type of the first one in the intersection, intersection casts into a series of simple
 * casts and introduces casts when the type expected is not the first in the intersection.
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
 *      return ((CharSequence) t);
 *    }
 *  }
 * </pre>
 */
public final class NormalizeIntersectionTypes extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Add casts at the required context sites.
    compilationUnit.accept(new ConversionContextVisitor(getContextRewriter()));

    // Add casts at method calls and field references.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(final FieldAccess fieldAccess) {
            return maybeInsertIntersectionCastExpression(fieldAccess);
          }

          @Override
          public Node rewriteMethodCall(final MethodCall methodCall) {
            return maybeInsertIntersectionCastExpression(methodCall);
          }
        });

    // Replace casts.
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

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {
      @Override
      public Expression rewriteAssignmentContext(
          TypeDescriptor toTypeDescriptor, Expression expression) {
        return maybeInsertIntersectionCastExpression(toTypeDescriptor, expression);
      }

      @Override
      public Expression rewriteMethodInvocationContext(
          TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
        return maybeInsertIntersectionCastExpression(parameterTypeDescriptor, argumentExpression);
      }
    };
  }

  private static Expression maybeInsertIntersectionCastExpression(
      TypeDescriptor toTypeDescriptor, Expression expression) {
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

    return CastExpression.newBuilder()
        .setExpression(expression)
        .setCastTypeDescriptor(targetInstanceTypeDescriptor)
        .build();
  }

  private static List<TypeDescriptor> maybeGetIntersectedTypeDescriptors(
      TypeDescriptor expressionTypeDescriptor) {
    if (expressionTypeDescriptor.isIntersection()) {
      return expressionTypeDescriptor.getIntersectedTypeDescriptors();
    }

    if (expressionTypeDescriptor.isTypeVariable()
        && expressionTypeDescriptor.getBoundTypeDescriptor() != null) {
      return maybeGetIntersectedTypeDescriptors(expressionTypeDescriptor.getBoundTypeDescriptor());
    }

    return null;
  }

  private static <T extends Expression & MemberReference>
      Expression maybeInsertIntersectionCastExpression(T expression) {
    if (expression.getQualifier() == null) {
      return expression;
    }

    Expression qualifier = expression.getQualifier();
    final TypeDescriptor requiredTypeDescriptor =
        expression.getTarget().getEnclosingClassTypeDescriptor();

    // This is a member reference to a method or field on a type that is not the first type
    // in the intersection type, a cast is needed.
    Expression newQualifier =
        maybeInsertIntersectionCastExpression(requiredTypeDescriptor, qualifier);

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
