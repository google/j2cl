/*
 * Copyright 2023 Google Inc.
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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableSet;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.UnionTypeDescriptor;

/**
 * Inserts projection casts for method-call qualifiers, necessary for Kotlin.
 *
 * <p>Java allows captures of upper-bound wildcards to appear in method arguments, and at the
 * right-hand side of field assignment:
 *
 * <pre>{@code
 * interface Observer<E extends Event> {
 *   void observe(E event);
 * }
 *
 * class Observable<E extends Event> {
 *   Observer<E> observer;
 *
 *   void setObserver(Observer<E> observer) {
 *     this.observer = observer;
 *   }
 * }
 *
 * void test(Observable<*> observable) {
 *   observable.observer = event -> {};
 *   observable.setObserver(event -> {});
 * }
 * }</pre>
 *
 * <p>In the code above, the {@code event} parameter is resolved as capture of wildcard with {@code
 * Event} upper-bound. In Kotlin, it's resolved as {@code Nothing} and fails to compile. This pass
 * inserts necessary projection cast:
 *
 * <pre>{@code
 * void test(Observable<*> observable) {
 *   ((Observable<Event>) observable).observer = event -> {};
 *   ((Observable<Event>) observable).setObserver(event -> {});
 * }
 * }</pre>
 */
public final class InsertQualifierProjectionCasts extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // TODO(b/362477320): Consider switching to ConversionContextVisitor, once
    //  {@code getParent()} is implemented.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            return projectFieldAccessQualifierInLhs(binaryExpression);
          }

          @Override
          public Node rewriteInvocation(Invocation invocation) {
            return projectInvocationQualifier(invocation);
          }

          private Expression projectExpression(Expression expression) {
            TypeDescriptor typeDescriptor = expression.getTypeDescriptor();
            ImmutableSet<TypeVariable> currentTypeParameters =
                getCurrentTypeParameters(getCurrentMember().getDescriptor());
            TypeDescriptor projectedTypeDescriptor =
                projectTypeArgumentsUpperBound(typeDescriptor, currentTypeParameters);
            if (typeDescriptor.equals(projectedTypeDescriptor)) {
              return expression;
            }

            return CastExpression.newBuilder()
                .setExpression(expression)
                .setCastTypeDescriptor(projectedTypeDescriptor)
                .build();
          }

          private Expression projectFieldAccessQualifierInLhs(BinaryExpression binaryExpression) {
            if (!binaryExpression.getOperator().isSimpleAssignment()) {
              return binaryExpression;
            }

            Expression leftOperand = binaryExpression.getLeftOperand();
            if (!(leftOperand instanceof FieldAccess)) {
              return binaryExpression;
            }

            FieldAccess fieldAccess = (FieldAccess) leftOperand;
            Expression qualifier = fieldAccess.getQualifier();
            if (qualifier == null) {
              return binaryExpression;
            }

            if (!containsCaptureWithoutLowerBound(fieldAccess.getTypeDescriptor())) {
              return binaryExpression;
            }

            return BinaryExpression.Builder.from(binaryExpression)
                .setLeftOperand(
                    FieldAccess.Builder.from(fieldAccess)
                        .setQualifier(projectExpression(fieldAccess.getQualifier()))
                        .build())
                .build();
          }

          private Expression projectInvocationQualifier(Invocation invocation) {
            Expression qualifier = invocation.getQualifier();
            if (qualifier == null) {
              return invocation;
            }

            if (invocation.getTarget().getParameterTypeDescriptors().stream()
                .allMatch(it -> !containsCaptureWithoutLowerBound(it))) {
              return invocation;
            }

            return Invocation.Builder.from(invocation)
                .setQualifier(projectExpression(invocation.getQualifier()))
                .build();
          }
        });
  }

  private static TypeDescriptor projectTypeArgumentsUpperBound(
      TypeDescriptor typeDescriptor, ImmutableSet<TypeVariable> currentTypeParameters) {
    if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      return declaredTypeDescriptor.withTypeArguments(
          declaredTypeDescriptor.getTypeArgumentDescriptors().stream()
              .map(typeArgument -> projectUpperBound(typeArgument, currentTypeParameters))
              .collect(toImmutableList()));
    } else if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.getLowerBoundTypeDescriptor() == null) {
        return projectTypeArgumentsUpperBound(
            typeVariable.getUpperBoundTypeDescriptor(), currentTypeParameters);
      }
    }

    return typeDescriptor;
  }

  private static TypeDescriptor projectUpperBound(
      TypeDescriptor typeDescriptor, ImmutableSet<TypeVariable> currentTypeParameters) {
    if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (typeVariable.isWildcardOrCapture()
          && typeVariable.getLowerBoundTypeDescriptor() == null) {
        return projectFreeTypeVariables(
            typeVariable.getUpperBoundTypeDescriptor(), currentTypeParameters);
      }
    }
    return typeDescriptor;
  }

  /** Project non-recursive free type variables to their bounds. */
  private static TypeDescriptor projectFreeTypeVariables(
      TypeDescriptor typeDescriptor, ImmutableSet<TypeVariable> currentTypeParameters) {
    return typeDescriptor.specializeTypeVariables(
        typeVariable ->
            !currentTypeParameters.contains(typeVariable) && !typeVariable.hasRecursiveDefinition()
                ? typeVariable.getUpperBoundTypeDescriptor()
                : typeVariable);
  }

  private static ImmutableSet<TypeVariable> getCurrentTypeParameters(
      MemberDescriptor memberDescriptor) {
    ImmutableSet.Builder<TypeVariable> builder = ImmutableSet.builder();
    if (memberDescriptor instanceof MethodDescriptor) {
      MethodDescriptor methodDescriptor = (MethodDescriptor) memberDescriptor;
      builder.addAll(methodDescriptor.getTypeParameterTypeDescriptors());
    }

    if (!memberDescriptor.isStatic()) {
      builder.addAll(
          memberDescriptor
              .getEnclosingTypeDescriptor()
              .getTypeDeclaration()
              .getTypeParameterDescriptors());
    }
    return builder.build();
  }

  private static boolean containsCaptureWithoutLowerBound(TypeDescriptor typeDescriptor) {
    return containsCaptureWithoutLowerBound(typeDescriptor, ImmutableSet.of());
  }

  // TODO(b/362475932): Clean-up after type model visitor is implemented.
  private static boolean containsCaptureWithoutLowerBound(
      TypeDescriptor typeDescriptor, ImmutableSet<TypeVariable> seen) {
    if (typeDescriptor instanceof PrimitiveTypeDescriptor) {
      return false;
    } else if (typeDescriptor instanceof ArrayTypeDescriptor) {
      ArrayTypeDescriptor arrayTypeDescriptor = (ArrayTypeDescriptor) typeDescriptor;
      return containsCaptureWithoutLowerBound(
          arrayTypeDescriptor.getComponentTypeDescriptor(), seen);
    } else if (typeDescriptor instanceof DeclaredTypeDescriptor) {
      DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
      return declaredTypeDescriptor.getTypeArgumentDescriptors().stream()
          .anyMatch(it -> containsCaptureWithoutLowerBound(it, seen));
    } else if (typeDescriptor instanceof TypeVariable) {
      TypeVariable typeVariable = (TypeVariable) typeDescriptor;
      if (seen.contains(typeVariable)) {
        return false;
      }

      if (!typeVariable.isWildcardOrCapture()) {
        return false;
      }

      if (typeVariable.isCapture() && typeVariable.getLowerBoundTypeDescriptor() == null) {
        return true;
      }

      ImmutableSet<TypeVariable> newSeen =
          ImmutableSet.<TypeVariable>builder().addAll(seen).add(typeVariable).build();
      TypeDescriptor upperBound = typeVariable.getUpperBoundTypeDescriptor();
      TypeDescriptor lowerBound = typeVariable.getLowerBoundTypeDescriptor();
      return containsCaptureWithoutLowerBound(upperBound, newSeen)
          || (lowerBound != null && containsCaptureWithoutLowerBound(lowerBound, newSeen));
    } else if (typeDescriptor instanceof IntersectionTypeDescriptor) {
      return ((IntersectionTypeDescriptor) typeDescriptor)
          .getIntersectionTypeDescriptors().stream()
              .anyMatch(it -> containsCaptureWithoutLowerBound(it, seen));
    } else if (typeDescriptor instanceof UnionTypeDescriptor) {
      return ((UnionTypeDescriptor) typeDescriptor)
          .getUnionTypeDescriptors().stream()
              .anyMatch(it -> containsCaptureWithoutLowerBound(it, seen));
    } else {
      throw new AssertionError("Unknown type descriptor: " + typeDescriptor.getClass());
    }
  }
}
