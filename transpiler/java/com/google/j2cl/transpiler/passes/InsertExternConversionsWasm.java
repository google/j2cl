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
import com.google.j2cl.transpiler.ast.WasmExportBridgesUtils;

/** Inserts conversions for objects crossing the JS/Wasm boundary. */
public class InsertExternConversionsWasm extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // TODO(b/540393685): Make this handling of the different boundaries more uniform by:
    //  - converting the native methods declarations together with the handing of exported methods.
    //  - handling all the boundary type conversions using the  `fromJs` and `toJs` infrastructure
    //    for the different types.
    rewriteNativeMethodDeclarations(compilationUnit);
    insertConvertionsForNativeMethodCalls(compilationUnit);
    insertBoundaryConversionsInCastsAndAssignments(compilationUnit);
  }

  /**
   * Rewrites native method declarations to use the appropriate types at the boundary so that the
   * import declarations has the correct types.
   */
  private static void rewriteNativeMethodDeclarations(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            MethodDescriptor descriptor = method.getDescriptor();

            if (!descriptor.isNative() || isAnnotatedWithWasm(descriptor)) {
              return method;
            }

            // Replace the types of the parameter variables.
            method
                .getParameters()
                .forEach(
                    p ->
                        p.setTypeDescriptor(
                            WasmExportBridgesUtils.getExternalType(
                                p.getTypeDescriptor(), /* isExport= */ false)));
            // Replace the method descriptor.
            return method.toBuilder()
                .setMethodDescriptor(createExportedMethodDescriptor(descriptor))
                .build();
          }
        });
  }

  private static MethodDescriptor createExportedMethodDescriptor(MethodDescriptor descriptor) {
    return descriptor.transform(
        builder ->
            builder
                .setReturnTypeDescriptor(
                    WasmExportBridgesUtils.getExternalType(
                        builder.getReturnTypeDescriptor(), /* isExport= */ false))
                .setParameterDescriptors(
                    descriptor.getParameterDescriptors().stream()
                        .map(
                            pd ->
                                pd.toBuilder()
                                    .setTypeDescriptor(
                                        WasmExportBridgesUtils.getExternalType(
                                            pd.getTypeDescriptor(), /* isExport= */ false))
                                    .setVarargs(false)
                                    .build())
                        .collect(toImmutableList())));
  }

  /**
   * Inserts conversions for parameters and return value of native method invocations.
   *
   * <p>Native method descriptors parameters and return types need explicit conversion to and from
   * extern refs.
   */
  private static void insertConvertionsForNativeMethodCalls(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteInvocation(Invocation invocation) {
            if (!invocation.getTarget().isNative() || isAnnotatedWithWasm(invocation.getTarget())) {
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
                                (expression, typeDescriptor) ->
                                    WasmExportBridgesUtils.convertToExternal(
                                        expression, typeDescriptor, /* isExport= */ false))
                            .collect(toImmutableList()))
                    .setTarget(createExportedMethodDescriptor(methodDescriptor))
                    .build();
            return WasmExportBridgesUtils.convertToInternal(
                newInvocation, methodDescriptor.getReturnTypeDescriptor(), /* isExport= */ false);
          }
        });
  }

  /** Inserts `Object.fromJs` and `Object.toJs` when needed in type conversion and cast contexts. */
  private static void insertBoundaryConversionsInCastsAndAssignments(
      CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new ConversionContextVisitor(
            new ConversionContextVisitor.ContextRewriter() {
              @Override
              public Expression rewriteTypeConversionContext(
                  TypeDescriptor inferredTypeDescriptor,
                  TypeDescriptor declaredTypeDescriptor,
                  Expression expression) {
                return insertBoundaryConversion(inferredTypeDescriptor, expression);
              }

              @Override
              public Expression rewriteCastContext(CastExpression castExpression) {
                TypeDescriptor toType = castExpression.getCastTypeDescriptor();
                Expression expression = castExpression.getExpression();

                Expression convertedExpression = insertBoundaryConversion(toType, expression);
                // If the expression was not converted, emit it as is with the cast.
                return convertedExpression == expression ? castExpression : convertedExpression;
              }
            }));
  }

  /**
   * Inserts {@code Object.fromJs} or {@code Object.toJs} conversion method calls when converting
   * between Java reference types and native JavaScript types.
   */
  private static Expression insertBoundaryConversion(
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
}
