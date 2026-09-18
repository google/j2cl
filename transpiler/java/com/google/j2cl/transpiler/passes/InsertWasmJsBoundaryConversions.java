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
import static com.google.j2cl.transpiler.ast.AstUtils.isAnnotatedWithWasm;

import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.WasmJsBoundaryUtils;

/**
 * Rewrites native method declarations for Wasm imports and inserts conversions for values crossing
 * the JS/Wasm boundary (in native method invocations, casts, and assignments).
 */
public class InsertWasmJsBoundaryConversions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // TODO(b/540393685): Make this handling of the different boundaries more uniform by:
    //  - converting the native method declarations together with the handling of exported methods.
    //  - handling all the boundary type conversions using the `fromJs` and `toJs` infrastructure
    //    for the different types.
    rewriteNativeMethodDeclarations(compilationUnit);
    insertConversionsForNativeMethodCalls(compilationUnit);
    insertConversionsInCastsAndAssignments(compilationUnit);
  }

  /**
   * Rewrites native method declarations to use the appropriate types at the boundary so that the
   * import declarations have the correct types.
   */
  private static void rewriteNativeMethodDeclarations(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            MethodDescriptor descriptor = method.getDescriptor();
            if (!isNativeJsMethod(descriptor)) {
              return method;
            }

            // Replace the types of the parameter variables.
            method
                .getParameters()
                .forEach(p -> p.setTypeDescriptor(getExternalType(p.getTypeDescriptor())));
            // Replace the method descriptor.
            return method.toBuilder()
                .setMethodDescriptor(createNativeImportMethodDescriptor(descriptor))
                .build();
          }
        });
  }

  private static MethodDescriptor createNativeImportMethodDescriptor(MethodDescriptor descriptor) {
    return descriptor.transform(
        builder ->
            builder
                .setReturnTypeDescriptor(getExternalType(builder.getReturnTypeDescriptor()))
                .setParameterDescriptors(
                    descriptor.getParameterDescriptors().stream()
                        .map(
                            pd ->
                                pd.toBuilder()
                                    .setTypeDescriptor(getExternalType(pd.getTypeDescriptor()))
                                    .setVarargs(false)
                                    .build())
                        .collect(toImmutableList())));
  }

  /**
   * Inserts JS/Wasm boundary conversions for arguments and return values of native method
   * invocations.
   */
  private static void insertConversionsForNativeMethodCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteInvocation(Invocation invocation) {
            if (!isNativeJsMethod(invocation.getTarget())) {
              return invocation;
            }
            // Insert boundary conversions for parameters and return value of native method
            // invocations.
            MethodDescriptor methodDescriptor = invocation.getTarget().getDeclarationDescriptor();
            Invocation newInvocation =
                invocation.toBuilder()
                    .setArguments(
                        Streams.zip(
                                invocation.getArguments().stream(),
                                methodDescriptor.getParameterTypeDescriptors().stream(),
                                InsertWasmJsBoundaryConversions::convertToExternal)
                            .collect(toImmutableList()))
                    .setTarget(createNativeImportMethodDescriptor(methodDescriptor))
                    .build();
            return convertToInternal(newInvocation, methodDescriptor.getReturnTypeDescriptor());
          }
        });
  }

  /** Inserts `Object.fromJs` and `Object.toJs` when needed in type conversion and cast contexts. */
  private static void insertConversionsInCastsAndAssignments(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ConversionContextVisitor.ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                return maybeInsertObjectBoundaryConversion(inferredTypeDescriptor, expression);
              }

              @Override
              public Expression rewriteCastContext(CastExpression castExpression) {
                TypeDescriptor toType = castExpression.getCastTypeDescriptor();
                Expression expression = castExpression.getExpression();

                Expression convertedExpression =
                    maybeInsertObjectBoundaryConversion(toType, expression);
                // If the expression was not converted, emit it as is with the cast.
                return convertedExpression == expression ? castExpression : convertedExpression;
              }
            }));
  }

  /**
   * Inserts {@code Object.fromJs} or {@code Object.toJs} conversion method calls when converting
   * between Java reference types and native JavaScript types.
   */
  private static Expression maybeInsertObjectBoundaryConversion(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    if (expression instanceof NullLiteral) {
      return expression;
    }

    TypeDescriptor fromTypeDescriptor = expression.getTypeDescriptor();

    if (!toTypeDescriptor.isNative() && fromTypeDescriptor.isNative()) {
      // Assignment or cast from native JS to Java.
      return RuntimeMethods.createFromJsMethodCall(
          TypeDescriptors.get().javaLangObject, expression);
    }

    if (toTypeDescriptor.isNative() && !fromTypeDescriptor.isNative()) {
      // Assignment or cast from Java to native JS.
      return RuntimeMethods.createToJsMethodCall(TypeDescriptors.get().javaLangObject, expression);
    }

    return expression;
  }

  private static boolean isNativeJsMethod(MethodDescriptor descriptor) {
    return descriptor.isNative() && !isAnnotatedWithWasm(descriptor);
  }

  private static TypeDescriptor getExternalType(TypeDescriptor typeDescriptor) {
    return WasmJsBoundaryUtils.getExternalType(typeDescriptor, /* isExport= */ false);
  }

  private static Expression convertToExternal(
      Expression expression, TypeDescriptor typeDescriptor) {
    return WasmJsBoundaryUtils.convertToExternal(expression, typeDescriptor, /* isExport= */ false);
  }

  private static Expression convertToInternal(
      Expression expression, TypeDescriptor typeDescriptor) {
    return WasmJsBoundaryUtils.convertToInternal(expression, typeDescriptor, /* isExport= */ false);
  }
}
