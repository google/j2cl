/*
 * Copyright 2021 Google Inc.
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

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangVoid;

import com.google.j2cl.common.Problems.Severity;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.Annotation;
import com.google.j2cl.transpiler.ast.ArrayConstant;
import com.google.j2cl.transpiler.ast.AssertStatement;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.HasAnnotations;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.TypeVariable;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.passes.ConversionContextVisitor.ContextRewriter;
import java.util.function.Supplier;

/**
 * Inserts NOT_NULL_ASSERTION (!!) in places where Java performs implicit null-check, and when
 * conversion is needed from nullable to non-null type.
 */
public final class InsertNotNullAssertionsOnNullabilityMismatch extends NormalizationPass {

  private static boolean isNonNullAssertionOnNullAllowed(SourcePosition sourcePosition) {
    String sourcePath = sourcePosition.getFilePath();
    if (sourcePath != null) {
      for (String path : J2ktAllowLists.NONNULL_ASSERTION_ON_NULL_PATHS) {
        if (sourcePath.contains(path)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // Insert non-null assertions when converting from nullable to non-null type.
    // We run this first before adding any other not null assertions since the surrounding context
    // is obscured from this rewriter. If we ran this later we may emit double not-null assertions
    // we would be unaware that we're already surrounded by one.
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {

                // Do not insert a null assertion if the return type of a call inferred to be
                // non-null. Kotlin does not null check these situations
                // (https://youtrack.jetbrains.com/issue/KT-8135) and there is existing code
                // that takes advantage of that.
                if (expression instanceof MethodCall
                    && !expression.getTypeDescriptor().canBeNull()
                    && !isWildcardOrCaptureAnnotatedNonNullable(expression.getTypeDescriptor())) {
                  return expression;
                }

                return !inferredTypeDescriptor.canBeNull() || !declaredTypeDescriptor.canBeNull()
                    ? insertNotNullAssertionIfNeeded(
                        getSourcePosition(),
                        expression,
                        inferredTypeDescriptor,
                        this::isNullnessSuppressed)
                    : expression;
              }

              // Insert null assertions if necessary on places where the construct requires them.
              // TODO(b/253062274): Revisit when the bug is fixed. The method above should be enough
              // to emit most of these assertions. But in the current state the inferred types do
              // not take into consideration the nullability of the original type variable
              // in the inference.
              @Override
              public Expression rewriteNonNullTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                return insertNotNullAssertionIfNeeded(
                    getSourcePosition(),
                    expression,
                    inferredTypeDescriptor,
                    this::isNullnessSuppressed);
              }

              private boolean isNullnessSuppressed() {
                return getParents()
                    .anyMatch(InsertNotNullAssertionsOnNullabilityMismatch::isNullnessSuppressed);
              }
            }));

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteAssertStatement(AssertStatement assertStatement) {
            Expression message = assertStatement.getMessage();
            if (message == null) {
              return assertStatement;
            }

            return AssertStatement.Builder.from(assertStatement)
                .setMessage(insertElvisIfNeeded(message, new StringLiteral("null")))
                .build();
          }
        });
  }

  private Expression insertNotNullAssertionIfNeeded(
      SourcePosition sourcePosition,
      Expression expression,
      TypeDescriptor inferredTypeDescriptor,
      Supplier<Boolean> hasSuppressNullnessAnnotationSupplier) {
    if (isInferredAsNonNullInKotlin(expression)) {
      // Don't insert null-check for expressions which are known to be non-null, regardless of
      // nullability annotations.
      return expression;
    }

    if (isJavaLangVoid(inferredTypeDescriptor)) {
      getProblems()
          .error(
              sourcePosition,
              "Non-null assertion applied to java.lang.Void expression. Make sure that Void types"
                  + " appearing in the surrounding code are annotated @Nullable. If Void type is"
                  + " not specified explicitly (in example in method type arguments), try"
                  + " specifying it explicitly, e.g.: Futures.<@Nullable Void, @Nullable"
                  + " Void>transform(...).");
    } else if (expression.isAlwaysNull() && !hasSuppressNullnessAnnotationSupplier.get()) {
      getProblems()
          .log(
              isNonNullAssertionOnNullAllowed(sourcePosition) ? Severity.WARNING : Severity.ERROR,
              sourcePosition,
              "Non-null assertion applied to null. Use @SuppressWarnings(\"nullness\") to"
                  + " suppress.");
    }

    return expression.postfixNotNullAssertion();
  }

  private static Expression insertElvisIfNeeded(
      Expression expression, Expression nonNullExpression) {
    if (isInferredAsNonNullInKotlin(expression)) {
      // Don't insert null-check for expressions which are known to be non-null, regardless of
      // nullability annotations.
      return expression;
    }

    if (expression instanceof NullLiteral) {
      return nonNullExpression;
    }

    MultiExpression.Builder elvisExpressionBuilder = MultiExpression.newBuilder();
    if (!expression.isIdempotent()) {
      Variable elvisVariable =
          Variable.newBuilder()
              .setName("tmp")
              .setFinal(true)
              .setTypeDescriptor(expression.getTypeDescriptor())
              .build();
      elvisExpressionBuilder.addExpressions(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(elvisVariable, expression)
              .build());
      expression = elvisVariable.createReference();
    }

    return elvisExpressionBuilder
        .addExpressions(
            ConditionalExpression.newBuilder()
                .setConditionExpression(expression.infixEqualsNull())
                .setTrueExpression(nonNullExpression)
                .setFalseExpression(expression.clone().postfixNotNullAssertion())
                .setTypeDescriptor(TypeDescriptors.get().javaLangObject.toNonNullable())
                .build())
        .build();
  }

  private static boolean isInferredAsNonNullInKotlin(Expression expression) {
    return !expression.canBeNull()
        && !isWildcardOrCaptureAnnotatedNonNullable(expression.getTypeDescriptor());
  }

  // TODO(b/361088311): Remove this workaround when the Kotlin bug is fixed.
  private static boolean isWildcardOrCaptureAnnotatedNonNullable(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isWildcardOrCapture()) {
      return ((TypeVariable) typeDescriptor).isAnnotatedNonNullable();
    }
    return false;
  }

  private static boolean isNullnessSuppressed(Object object) {
    return switch (object) {
      case Type type -> isNullnessSuppressed(type.getDeclaration());
      case Method method -> isNullnessSuppressed(method.getDescriptor());
      default -> false;
    };
  }

  private static boolean isNullnessSuppressed(HasAnnotations node) {
    Annotation annotation = node.getAnnotation("java.lang.SuppressWarnings");
    return annotation != null
        && ((ArrayConstant) annotation.getValues().get("value"))
            .getValueExpressions().stream()
                .anyMatch(v -> ((StringLiteral) v).getValue().startsWith("nullness"));
  }
}
