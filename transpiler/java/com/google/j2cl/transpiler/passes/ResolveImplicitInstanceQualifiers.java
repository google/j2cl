/*
 * Copyright 2021 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;

/** Resolves implicit qualifiers for instance members and constructors. */
public class ResolveImplicitInstanceQualifiers extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {

    fixupAnonymousClassQualifiersInConstructors(compilationUnit);
    resolveImplicitInstanceQualifiers(compilationUnit);
  }

  /**
   * Prevents the resolution of the implicit qualifiers in anonymous inner classes to this if they
   * occur before the super/this constructor call.
   */
  private static void fixupAnonymousClassQualifiersInConstructors(CompilationUnit compilationUnit) {
    // Anonymous classes are considered to capture the enclosing instance unless they
    // appear in a static method. But when declared inside a constructor they might only
    // capture the enclosing instance only if they don't appear before the super/this
    // constructor call invocation.
    // To workaround that we continue to construct anonymous inner classes as if they
    // capture the enclosing instance, but pass `null` as the instance if the instantiation
    // happens before the super/this constructor call.
    compilationUnit
        .streamTypes()
        .flatMap(t -> t.getConstructors().stream())
        .forEach(
            c -> {
              MethodCall delegatedConstructorCall = AstUtils.getConstructorInvocation(c);
              if (delegatedConstructorCall == null
                  || !delegatedConstructorCall.getTarget().isJsConstructor()) {
                return;
              }

              fixDelegatedConstructorCall(c, delegatedConstructorCall);
            });
  }

  private static void fixDelegatedConstructorCall(
      Method constructor, MethodCall delegatedConstructorCall) {
    constructor.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            if (newInstance.getQualifier() != null) {
              return newInstance;
            }

            if (newInstance
                .getTarget()
                .getEnclosingTypeDescriptor()
                .getTypeDeclaration()
                .isAnonymous()) {
              // This is an anonymous class instantiation with an implicit qualifier
              // happening in a constructor before the super/this call.
              // Pass null as the enclosing instance instead of a this reference.
              return NewInstance.Builder.from(newInstance)
                  .setQualifier(
                      constructor.getDescriptor().getEnclosingTypeDescriptor().getNullValue())
                  .build();
            }
            return newInstance;
          }

          @Override
          public boolean shouldProcessType(Type type) {
            // Do not recurse into nested types.
            return false;
          }

          private boolean foundDelegatedConstructorCall = false;

          @Override
          public Node rewriteExpression(Expression expression) {
            if (expression == delegatedConstructorCall) {
              // All the sub nodes have been processed and we found the super constructor
              // call. Anonymous classes instantiated after this point can capture the
              // enclosing instance.
              foundDelegatedConstructorCall = true;
            }
            return expression;
          }

          @Override
          public boolean shouldProcessNode(Node node) {
            // Skip all processing after the constructor call was found.
            return !foundDelegatedConstructorCall;
          }
        });
  }

  private static void resolveImplicitInstanceQualifiers(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public MemberReference rewriteMemberReference(MemberReference memberReference) {
            return AstUtils.resolveImplicitQualifier(
                memberReference, getCurrentType().getTypeDescriptor());
          }
        });
  }
}
