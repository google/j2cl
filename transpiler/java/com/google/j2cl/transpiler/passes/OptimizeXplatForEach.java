/*
 * Copyright 2024 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.JsForInStatement;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PostfixOperator;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.Optional;

/**
 * Rewrites well-known iterables from Xplat to array-like or for-in like iteration.
 *
 * <p>For array-like types we'll rewrite code like:
 *
 * <pre>{@code
 * for (Object value : arrayLike.getIterable()) { ... }
 * }</pre>
 *
 * into:
 *
 * <pre>{@code
 * for (Object value : (Object[]) arrayLike) { ... }
 * }</pre>
 *
 * <p>For object-like types we'll rewrite code like:
 *
 * <pre>{@code
 * for (String key : objLike.getIterableKeys()) { ... }
 * }</pre>
 *
 * into:
 *
 * <pre>{@code
 * for (String key in objLike) { ... }
 * }</pre>
 */
public class OptimizeXplatForEach extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Statement rewriteForEachStatement(ForEachStatement forEachStatement) {
            Expression unwrappedIterableExpression =
                unwrapNoopExpressions(forEachStatement.getIterableExpression());
            if (!(unwrappedIterableExpression instanceof MethodCall)) {
              return forEachStatement;
            }
            MethodCall iterableMethodCall = (MethodCall) unwrappedIterableExpression;
            MethodDescriptor invokedMethod = iterableMethodCall.getTarget();
            Optional<WellKnownIterable> wellKnownIterable =
                WELL_KNOWN_ITERABLES.stream()
                    .filter(it -> it.matchesMethod(invokedMethod))
                    .findFirst();

            if (wellKnownIterable.isEmpty()) {
              return forEachStatement;
            }

            switch (wellKnownIterable.get().iterationType()) {
              case FOR_IN:
                return convertToForInObject(forEachStatement, iterableMethodCall);
              case FOR_ARRAY:
                return convertToArrayForLoop(
                    forEachStatement, iterableMethodCall, wellKnownIterable.get());
            }
            throw new AssertionError("exhaustive switch");
          }
        });
  }

  /** Unwraps enclosing cast and not-null postfix expression */
  private static Expression unwrapNoopExpressions(Expression expression) {
    if (expression instanceof JsDocCastExpression) {
      return unwrapNoopExpressions(((JsDocCastExpression) expression).getExpression());
    } else if (expression instanceof PostfixExpression
        && ((PostfixExpression) expression).getOperator() == PostfixOperator.NOT_NULL_ASSERTION) {
      return unwrapNoopExpressions(((PostfixExpression) expression).getOperand());
    }
    return expression;
  }

  private Statement convertToForInObject(
      ForEachStatement forEachStatement, MethodCall iterableMethodCall) {
    Variable loopVariable = forEachStatement.getLoopVariable();
    TypeDescriptor loopVariableType = loopVariable.getTypeDescriptor();
    Expression jsIterableExpression = iterableMethodCall.getQualifier();

    // for..in loops in JS will always return string values. If the variable type is already String
    // then we don't need to do extra coercion work.
    if (loopVariableType.hasSameRawType(TypeDescriptors.get().javaLangString)) {
      return JsForInStatement.Builder.from(forEachStatement)
          .setLoopVariable(loopVariable)
          .setIterableExpression(jsIterableExpression)
          .build();
    }

    // If the loop variable isn't a String we'll attempt to coerce it within the body of the loop.
    // Given the original loop:
    //
    // for (int value : foo) { ... }
    //
    // it will be rewritten to:
    //
    // for (String stringElement : foo) {
    //   int value = Number(stringElement, 10);
    //   ...
    // }
    checkState(
        loopVariableType.hasSameRawType(TypeDescriptors.get().javaLangInteger)
            || loopVariableType.hasSameRawType(PrimitiveTypes.INT));

    Variable propertyVariable =
        Variable.newBuilder()
            .setName("property")
            .setTypeDescriptor(TypeDescriptors.get().javaLangString)
            .build();

    // loopVariable = (int) Number(property)
    VariableDeclarationExpression coercedLoopVariable =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                loopVariable,
                JsDocCastExpression.newBuilder()
                    .setExpression(
                        RuntimeMethods.createNumberCall(propertyVariable.createReference()))
                    .setCastTypeDescriptor(PrimitiveTypes.INT)
                    .build())
            .build();

    return JsForInStatement.Builder.from(forEachStatement)
        .setLoopVariable(loopVariable)
        .setIterableExpression(jsIterableExpression)
        .setLoopVariable(propertyVariable)
        .setBodyStatements(
            coercedLoopVariable.makeStatement(loopVariable.getSourcePosition()),
            forEachStatement.getBody())
        .build();
  }

  private Statement convertToArrayForLoop(
      ForEachStatement forEachStatement,
      MethodCall iterableMethodCall,
      WellKnownIterable wellKnownIterable) {
    TypeDescriptor valueType =
        Iterables.getFirst(
            ((DeclaredTypeDescriptor) iterableMethodCall.getTypeDescriptor())
                .getTypeArgumentDescriptors(),
            TypeDescriptors.get().javaLangObject);

    // In the case of types like JsArrayInteger, the backing array will be holding primitive int
    // values. However, the iterable would have been typed as Iterable<Integer>. Therefore, if it's
    // well-known that the backing array holds primitive, we'll unbox the type.
    if (wellKnownIterable.holdsPrimitiveValues()) {
      valueType = valueType.toUnboxedType();
    }

    // TODO(b/118299062): JsEnum arrays are not currently allowed, thankfully the values in the
    //   originally array will be boxed anyway.
    if (valueType.isJsEnum() && AstUtils.isNonNativeJsEnum(valueType)) {
      valueType = TypeDescriptors.getEnumBoxType(valueType);
    }

    // Cast the underlying array like type to a Java Array to let it be normalized by later passes.
    //
    // For example:
    //
    // for (Object value : arrayLike.getIterable()) { ... }
    //
    // will be rewritten to:
    //
    // for (Object value : (Object[]) arrayLike) { ... }
    Expression target = iterableMethodCall.getQualifier();
    return ForEachStatement.Builder.from(forEachStatement)
        .setIterableExpression(
            JsDocCastExpression.newBuilder()
                .setCastTypeDescriptor(
                    ArrayTypeDescriptor.newBuilder().setComponentTypeDescriptor(valueType).build())
                .setExpression(target)
                .build())
        .build();
  }

  private static final ImmutableList<WellKnownIterable> WELL_KNOWN_ITERABLES =
      ImmutableList.of(
          WellKnownIterable.builder(IterationType.FOR_ARRAY)
              .enclosingClassQualifiedName("com.google.gwt.corp.collections.AbstractJsArray")
              .build(),
          WellKnownIterable.builder(IterationType.FOR_ARRAY)
              .enclosingClassQualifiedName("com.google.gwt.corp.collections.JsArray")
              .build(),
          WellKnownIterable.builder(IterationType.FOR_ARRAY)
              .enclosingClassQualifiedName("com.google.gwt.corp.collections.ImmutableJsArray")
              .build(),
          WellKnownIterable.builder(IterationType.FOR_ARRAY)
              .enclosingClassQualifiedName("com.google.gwt.corp.collections.UnmodifiableJsArray")
              .build(),
          WellKnownIterable.builder(IterationType.FOR_ARRAY)
              .enclosingClassQualifiedName(
                  "com.google.apps.docs.xplat.collections.AbstractJsArrayInteger")
              .holdsPrimitiveValues(true)
              .build(),
          WellKnownIterable.builder(IterationType.FOR_ARRAY)
              .enclosingClassQualifiedName("com.google.apps.docs.xplat.collections.JsArrayInteger")
              .holdsPrimitiveValues(true)
              .build(),
          WellKnownIterable.builder(IterationType.FOR_ARRAY)
              .enclosingClassQualifiedName(
                  "com.google.apps.docs.xplat.collections.SerializedJsArray")
              .build(),
          WellKnownIterable.builder(IterationType.FOR_IN)
              .enclosingClassQualifiedName("com.google.apps.docs.xplat.structs.SparseArray")
              .build(),
          WellKnownIterable.builder(IterationType.FOR_IN)
              .enclosingClassQualifiedName("com.google.apps.docs.xplat.collections.SerializedJsMap")
              .build(),
          WellKnownIterable.builder(IterationType.FOR_IN)
              .enclosingClassQualifiedName("com.google.apps.docs.xplat.collections.UnsafeJsMap")
              .build(),
          WellKnownIterable.builder(IterationType.FOR_IN)
              .enclosingClassQualifiedName(
                  "com.google.apps.docs.xplat.collections.UnsafeJsMapInteger")
              .holdsPrimitiveValues(true)
              .build(),
          WellKnownIterable.builder(IterationType.FOR_IN)
              .enclosingClassQualifiedName("com.google.apps.docs.xplat.collections.UnsafeJsSet")
              .build());

  @AutoValue
  abstract static class WellKnownIterable {
    public abstract String enclosingClassQualifiedName();

    public abstract String iterableMethodName();

    public abstract IterationType iterationType();

    public abstract boolean holdsPrimitiveValues();

    public final boolean matchesMethod(MethodDescriptor methodDescriptor) {
      return enclosingClassQualifiedName()
              .equals(methodDescriptor.getEnclosingTypeDescriptor().getQualifiedSourceName())
          && iterableMethodName().equals(methodDescriptor.getName());
    }

    public static Builder builder(IterationType iterationType) {
      return new AutoValue_OptimizeXplatForEach_WellKnownIterable.Builder()
          .iterationType(iterationType)
          .iterableMethodName(
              iterationType == IterationType.FOR_ARRAY ? "getIterable" : "getIterableKeys")
          .holdsPrimitiveValues(false);
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder enclosingClassQualifiedName(String enclosingClassQualifiedName);

      abstract Builder iterableMethodName(String iterableMethodName);

      abstract Builder iterationType(IterationType iterationType);

      abstract Builder holdsPrimitiveValues(boolean holdsPrimitiveValues);

      abstract WellKnownIterable build();
    }
  }

  enum IterationType {
    FOR_IN,
    FOR_ARRAY;
  }
}
