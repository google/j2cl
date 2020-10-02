/*
 * Copyright 2015 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;

/**
 * Verifies that the method call argument counts match the method descriptor parameter counts and
 * method declaration parameter counts match the method descriptor.
 */
public class VerifyParamAndArgCounts extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            checkState(
                method.getParameters().size()
                    == method.getDescriptor().getParameterTypeDescriptors().size());
          }

          @Override
          public void exitInvocation(Invocation invocation) {
            MethodDescriptor methodDescriptor = invocation.getTarget();
            int paramCount = methodDescriptor.getParameterTypeDescriptors().size();
            int argumentCount = invocation.getArguments().size();
            if (methodDescriptor.isJsMethodVarargs()) {
              checkState(argumentCount >= paramCount - 1, "Invalid call argument count.");
            } else {
              checkState(argumentCount == paramCount, "Invalid call argument count.");
            }
          }
        });
  }
}
