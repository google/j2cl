/*
 * Copyright 2025 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchExpression;

/**
 * Instruments exhaustive switch expressions with no default to handle unexpected values.
 *
 * <p>Exhaustive switch expressions that don't explicitly handle the default case are instrumented
 * to check for unexpected values. In the JVM this is done to generate correct code in the presence
 * of library skew, which is not an issue in J2CL. However a similar situation could happen with
 * JsEnums where it is quite easy to receive an unexpected value.
 *
 * <p>The same reasoning could be applied to switch statements; i.e. if we determine that the switch
 * statement is exhaustive and lacks default handling it could be instrumented. But the nuance in
 * switch statements is that the default handling might be present outside the switch construct as
 * in the following code.
 *
 * <pre>{@code
 * enum X { A, B;}
 *
 * ....
 * switch (e) {
 *    case A:
 *      return 1;
 *    case B:
 *      return 2;
 * }
 * // Handle unexpected value here
 * throw new AssertionError();
 * }</pre>
 */
public class AddSwitchExpressionsExhaustivenessCheck extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitSwitchExpression(SwitchExpression switchExpression) {
            if (switchExpression.hasDefaultCase()) {
              return;
            }

            // The switch expression does not have a default case; that means that the frontend
            // deemed it exhaustive, hence add a default case with a runtime check.
            // TODO(b/395953418): Reject switch expressions with no default on native jsenums.
            var expressionType = switchExpression.getTypeDescriptor().toRawTypeDescriptor();
            var isCritical = expressionType.isNative() && expressionType.isJsEnum();

            var sourcePosition = switchExpression.getSourcePosition();
            var checkMethodCall =
                RuntimeMethods.createCheckCriticalExhaustiveCall(isCritical)
                    .makeStatement(sourcePosition);
            switchExpression
                .getCases()
                .add(SwitchCase.newBuilder().setStatements(checkMethodCall).build());
          }
        });
  }
}
