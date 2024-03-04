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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.HasSourcePosition;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.Map;

/** Rewrite System.getProperty() calls based on values passed to the transpiler */
public class ImplementSystemGetProperty extends NormalizationPass {
  private final Map<String, String> properties;

  public ImplementSystemGetProperty(Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget();

            if (!target.getOrigin().isSystemGetPropertyGetter()) {
              return methodCall;
            }

            // JsInteropRestrictionChecker enforces the first parameter is a StringLiteral.
            String propertyKey = target.getName();
            String value = properties.get(propertyKey);

            if (value == null) {
              if (target.getOrigin().isRequiredSystemGetPropertyGetter()) {
                SourcePosition sourcePosition =
                    (((HasSourcePosition) getParent(HasSourcePosition.class::isInstance)))
                        .getSourcePosition();
                getProblems()
                    .error(sourcePosition, "No value found for required property %s", propertyKey);
              }
              return TypeDescriptors.get().javaLangString.getNullValue();
            }

            return new StringLiteral(value);
          }
        });
  }
}
