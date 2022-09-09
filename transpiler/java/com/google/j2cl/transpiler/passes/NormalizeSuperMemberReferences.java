/*
 * Copyright 2022 Google Inc.
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
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;

/**
 * Normalize the references to members through super to satisfy the invariants we hold for the
 * passes that follow.
 *
 * <p>References that resolve to member in a super class will preserve the super qualifier.
 * References to super member of an outer class or to a default method implementation will be marked
 * as static dispatch and have ThisReference as their qualifier.
 */
public class NormalizeSuperMemberReferences extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            if (!(methodCall.getQualifier() instanceof SuperReference)) {
              return methodCall;
            }
            SuperReference qualifier = (SuperReference) methodCall.getQualifier();

            if (methodCall.getTarget().isDefaultMethod() || methodCall.isStaticDispatch()) {
              // Treat calls to interface default method as static dispatch (targeting the default
              // method in the interface).
              // Make all static dispatch calls to go through ThisReference instead of super, since
              // in that case the qualifier becomes a parameter of the explicit call and super
              // is only valid as a qualifier.
              return MethodCall.Builder.from(methodCall)
                  .setStaticDispatch(true)
                  .setQualifier(new ThisReference(qualifier.getTypeDescriptor(), false))
                  .build();
            }

            if (getCurrentType()
                .getTypeDescriptor()
                .hasSameRawType(qualifier.getTypeDescriptor())) {
              // Regular super call targeting a method in a superclass. Nothing to rewrite.
              return methodCall;
            }

            // This is a qualified super call, targeting an outer class method; for that reason we
            // use a ThisReference instead of a SuperReference and mark the class explicitly as
            // static dispatch. SuperReferences are only used when targeting methods of an actual
            // superclass of the class that enclosed the code in question.
            return MethodCall.Builder.from(methodCall)
                .setQualifier(new ThisReference(qualifier.getTypeDescriptor(), true))
                .setStaticDispatch(true)
                .build();
          }

          @Override
          public FieldAccess rewriteFieldAccess(FieldAccess fieldAccess) {
            if (!(fieldAccess.getQualifier() instanceof SuperReference)) {
              return fieldAccess;
            }
            SuperReference qualifier = (SuperReference) fieldAccess.getQualifier();

            // Always rewrite super field accesses to go through "this" instead of super, the
            // FieldDescriptor uniquely determines which field to access.
            return FieldAccess.Builder.from(fieldAccess)
                .setQualifier(
                    new ThisReference(qualifier.getTypeDescriptor(), qualifier.isQualified()))
                .build();
          }
        });
  }
}
