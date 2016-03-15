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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodBuilder;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;
import com.google.j2cl.ast.Node;

import java.util.ArrayList;

/**
 * The first line of a static method should be a call to the Class's clinit.
 */
public class InsertClassInitStaticMethods {
  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ClassInitStaticMethodsRewriter());
  }

  private static class ClassInitStaticMethodsRewriter extends AbstractRewriter {
    @Override
    public Node rewriteMethod(Method node) {
      if (node.getDescriptor().isStatic()) {
        MethodDescriptor clinitDescriptor =
            MethodDescriptorBuilder.fromDefault()
                .jsInfo(JsInfo.RAW)
                .isStatic(true)
                .enclosingClassTypeDescriptor(
                    node.getDescriptor().getEnclosingClassTypeDescriptor())
                .methodName("$clinit")
                .build();
        MethodCall call =
            MethodCall.createRegularMethodCall(null, clinitDescriptor, new ArrayList<Expression>());
        return MethodBuilder.from(node).statement(0, new ExpressionStatement(call)).build();
      }
      return node;
    }
  }
}
