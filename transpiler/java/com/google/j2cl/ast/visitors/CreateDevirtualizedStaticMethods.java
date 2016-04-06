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

import com.google.common.base.Preconditions;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptors;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates instance methods to static methods, and makes the instance methods empty.
 */
public class CreateDevirtualizedStaticMethods extends AbstractRewriter {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new CreateDevirtualizedStaticMethods());
  }

  @Override
  public boolean shouldProcessJavaType(JavaType type) {
    // Creates devirtualized static methods for the unboxed types (Boolean, Double, String), and
    // for JsOverlay methods in native JS types, and JsFunction method.
    return TypeDescriptors.isBoxedTypeAsJsPrimitives(type.getDescriptor())
        || type.getDescriptor().isNative()
        || type.getDescriptor().isJsFunctionImplementation();
  }

  @Override
  public Node rewriteMethod(Method method) {
    if (!shouldDevirtualize(method, getCurrentJavaType())) {
      return method;
    }
    Variable thisArg = new Variable("$thisArg", getCurrentJavaType().getDescriptor(), false, true);

    // Replace all 'this' references in the method with parameter references.
    method.accept(new ReplaceThisReferencesVisitor(thisArg));

    // MethodDescriptor for the devirtualized static method.
    MethodDescriptor newMethodDescriptor =
        MethodDescriptors.makeStaticMethodDescriptor(method.getDescriptor());

    // Parameters for the devirtualized static method.
    List<Variable> newParameters = new ArrayList<>();
    newParameters.add(thisArg); // added extra parameter.
    newParameters.addAll(method.getParameters()); // original parameters in the instance method.

    // Add the static method to current type.
    Method staticMethod = new Method(newMethodDescriptor, newParameters, method.getBody());
    getCurrentJavaType().addMethod(staticMethod);

    // Turn the instance method to an empty method since it should not be called. But we should not
    // delete it otherwise it may lead to JSCompiler errors that complains that the class does not
    // implement all the methods in its super interfaces.
    return Method.Builder.from(method).clearStatements().build();
  }

  private boolean shouldDevirtualize(Method method, JavaType javaType) {
    if (method.getDescriptor().isStaticDispatch()) {
      return false;
    }
    // do not devirtualize non-JsOverlay method.
    if (javaType.getDescriptor().isNative() && !method.getDescriptor().isJsOverlay()) {
      return false;
    }
    if (javaType.getDescriptor().isJsFunctionImplementation()) {
      if (method.getDescriptor().isJsFunction()) {
        // If the JsFunction method has different method signature from the SAM method, it should be
        // devirtualized.
        MethodDescriptor samMethodDescriptor =
            method
                .getDescriptor()
                .getEnclosingClassTypeDescriptor()
                .getJsFunctionMethodDescriptor();
        return !AstUtils.areParameterErasureEqual(method.getDescriptor(), samMethodDescriptor);
      }
    }
    Preconditions.checkArgument(
        !method.getDescriptor().isJsProperty(), "JsProperty should never be devirtualized");
    // TODO: remove once init() function is synthesized in AST.
    Preconditions.checkArgument(!method.getDescriptor().isInit(), "Do not devirtualize init().");
    return true;
  }

  /**
   * Replaces 'this' reference with parameter reference.
   */
  private class ReplaceThisReferencesVisitor extends AbstractRewriter {
    private final Variable thisArg;

    public ReplaceThisReferencesVisitor(Variable thisArg) {
      this.thisArg = thisArg;
    }

    @Override
    public Node rewriteThisReference(ThisReference thisReference) {
      return thisArg.getReference();
    }
  }
}
