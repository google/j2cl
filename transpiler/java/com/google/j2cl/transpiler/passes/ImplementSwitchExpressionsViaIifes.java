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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.LambdaAdaptorTypeDescriptors;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.SwitchExpression;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.YieldStatement;

/**
 * Implements switch expressions as an immediately invoked function expressions that executes a
 * switch statement..
 *
 * <p>In JavaScript switch constructs are only allowed as statements. This pass converts the switch
 * expression into switch statement and wraps it into an IIFE which allows us to execute a statement
 * as if it were an expression.
 */
public class ImplementSwitchExpressionsViaIifes extends NormalizationPass {

  // Since there is no construct that is equivalent to a switch expression in JavaScript,
  // expressions like
  //
  //    int x = switch(x) {
  //        case 1, 3 -> 3;
  //        case 4 -> { int j = 3; break j; }
  //        case 6 -> throw new Exception();
  //        default -> f();
  //      };
  //
  // get desugared here to:
  //
  //    int x = ((JsSupplier) (() -> switch(x) {
  //        case 1:
  //        case 3:
  //          return 3;
  //        case 4:
  //          { int j = 3; return j; }
  //        case 6 :
  //          throw new Exception();
  //        default:
  //          return f();
  //      })).get();
  //
  // taking advantage the functions in JavaScript can modify the values that are captured.

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {

          @Override
          public Node rewriteSwitchExpression(SwitchExpression switchExpression) {
            DeclaredTypeDescriptor supplierJsFunctionTypeDescriptor =
                getSupplierJsFunction(switchExpression.getTypeDescriptor());

            return MethodCall.Builder.from(
                    supplierJsFunctionTypeDescriptor.getJsFunctionMethodDescriptor())
                .setQualifier(
                    // Enclose the switch expression as a switch statement inside a function.
                    FunctionExpression.newBuilder()
                        .setTypeDescriptor(supplierJsFunctionTypeDescriptor)
                        .setStatements(SwitchStatement.Builder.from(switchExpression).build())
                        .setSourcePosition(switchExpression.getSourcePosition())
                        .build())
                .build();
          }

          @Override
          public ReturnStatement rewriteYieldStatement(YieldStatement yieldStatement) {
            return ReturnStatement.newBuilder()
                .setExpression(yieldStatement.getExpression())
                .setSourcePosition(yieldStatement.getSourcePosition())
                .build();
          }
        });
  }

  /** Returns the JsFunction supplier with the appropriate type for {@code typeDescriptor}. */
  private DeclaredTypeDescriptor getSupplierJsFunction(TypeDescriptor typeDescriptor) {
    // TODO(b/112308901): remove this hacky code when function types are modeled better.
    return LambdaAdaptorTypeDescriptors.createJsFunctionTypeDescriptor(
        getSupplierType(typeDescriptor));
  }

  /** Returns the supplier that is most suitable to be used for {@code typeDescriptor}. */
  private DeclaredTypeDescriptor getSupplierType(TypeDescriptor typeDescriptor) {
    if (!typeDescriptor.isPrimitive()) {
      return TypeDescriptors.get()
          .javaUtilFunctionSupplier
          .specializeTypeVariables(typeVariable -> typeDescriptor);
    } else if (TypeDescriptors.isPrimitiveBoolean(typeDescriptor)) {
      return TypeDescriptors.get().javaUtilFunctionBooleanSupplier;
    } else if (TypeDescriptors.isPrimitiveFloatOrDouble(typeDescriptor)) {
      return TypeDescriptors.get().javaUtilFunctionDoubleSupplier;
    } else if (TypeDescriptors.isPrimitiveLong(typeDescriptor)) {
      return TypeDescriptors.get().javaUtilFunctionLongSupplier;
    }
    return TypeDescriptors.get().javaUtilFunctionIntSupplier;
  }
}
