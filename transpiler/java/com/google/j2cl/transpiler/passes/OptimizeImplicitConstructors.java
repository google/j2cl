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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Type;

/** Remove constructors which can be implicit. */
public class OptimizeImplicitConstructors extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteType(Type type) {
            if (type.isInterface()) {
              return type;
            }

            ImmutableList<Method> constructors = type.getConstructors();
            if (constructors.size() != 1) {
              return type;
            }

            Method constructor = constructors.get(0);
            MethodDescriptor descriptor = constructor.getDescriptor();

            if (!descriptor.getParameterDescriptors().isEmpty()) {
              return type;
            }

            if (!constructor.getBody().getStatements().isEmpty()) {
              return type;
            }

            if (descriptor.getVisibility() != type.getDeclaration().getVisibility()) {
              if (!type.isEnum()) {
                return type;
              }
            }

            if (descriptor.isJsConstructor()
                && descriptor.getOriginalJsInfo().getHasJsMemberAnnotation()) {
              return type;
            }

            if (descriptor.getObjectiveCName() != null) {
              return type;
            }

            type.getMembers().remove(constructors.get(0));

            return type;
          }
        });
  }
}
