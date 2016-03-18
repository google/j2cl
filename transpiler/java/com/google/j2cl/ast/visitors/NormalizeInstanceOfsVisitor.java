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
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.TypeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces instanceof expression with corresponding $isInstance method call.
 */
public class NormalizeInstanceOfsVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeInstanceOfsVisitor().normalizeInstanceOfs(compilationUnit);
  }

  private void normalizeInstanceOfs(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteInstanceOfExpression(InstanceOfExpression expression) {
    TypeDescriptor checkTypeDescriptor = expression.getTestTypeDescriptor();
    if (checkTypeDescriptor.isArray()) {
      return rewriteArrayInstanceOfExpression(expression);
    }
    return rewriteRegularInstanceOfExpression(expression);
  }

  private Node rewriteRegularInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
    TypeDescriptor checkTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();
    if (checkTypeDescriptor.isNative()) {
      checkTypeDescriptor = AstUtils.createJsOverlayImplTypeDescriptor(checkTypeDescriptor);
    }

    MethodDescriptor isInstanceMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .jsInfo(JsInfo.RAW)
            .isStatic(true)
            .enclosingClassTypeDescriptor(checkTypeDescriptor)
            .methodName("$isInstance")
            .parameterTypeDescriptors(Lists.newArrayList(TypeDescriptors.get().javaLangObject))
            .returnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(instanceOfExpression.getExpression());

    // TypeName.$isInstance(expr);
    return MethodCall.createRegularMethodCall(null, isInstanceMethodDescriptor, arguments);
  }

  private Node rewriteArrayInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
    TypeDescriptor checkTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();
    Preconditions.checkArgument(checkTypeDescriptor.isArray());

    if (checkTypeDescriptor.getLeafTypeDescriptor().isNative()) {
      return rewriteNativeJsArrayInstanceOfExpression(instanceOfExpression);
    }
    return rewriteJavaArrayInstanceOfExpression(instanceOfExpression);
  }

  private Node rewriteJavaArrayInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
    TypeDescriptor checkTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();
    MethodDescriptor isInstanceMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .jsInfo(JsInfo.RAW)
            .isStatic(true)
            .enclosingClassTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
            .methodName("$instanceIsOfType")
            .parameterTypeDescriptors(
                Lists.newArrayList(
                    TypeDescriptors.get().javaLangObject,
                    TypeDescriptors.get().javaLangObject,
                    TypeDescriptors.get().primitiveInt))
            .returnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(instanceOfExpression.getExpression());
    arguments.add(new TypeReference(checkTypeDescriptor.getLeafTypeDescriptor()));
    arguments.add(
        new NumberLiteral(TypeDescriptors.get().primitiveInt, checkTypeDescriptor.getDimensions()));
    // Arrays.$instanceIsOfType(expr, leafType, dimensions);
    return MethodCall.createRegularMethodCall(null, isInstanceMethodDescriptor, arguments);
  }

  /**
   * Instanceof check on array with leaf type that is a native JsType is equivalent to check if the
   * instance is a raw JS array (i.e. Array.isArray(instance)).
   */
  private Node rewriteNativeJsArrayInstanceOfExpression(InstanceOfExpression instanceOfExpression) {
    TypeDescriptor checkTypeDescriptor = instanceOfExpression.getTestTypeDescriptor();
    Preconditions.checkArgument(checkTypeDescriptor.isArray());
    Preconditions.checkArgument(checkTypeDescriptor.getLeafTypeDescriptor().isNative());

    MethodDescriptor isInstanceMethodDescriptor =
        MethodDescriptorBuilder.fromDefault()
            .jsInfo(JsInfo.RAW)
            .isStatic(true)
            .enclosingClassTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
            .methodName("$instanceIsOfNative")
            .parameterTypeDescriptors(Lists.newArrayList(TypeDescriptors.get().javaLangObject))
            .returnTypeDescriptor(TypeDescriptors.get().primitiveBoolean)
            .build();
    List<Expression> arguments = new ArrayList<>();
    arguments.add(instanceOfExpression.getExpression());
    // Arrays.$isArray(expr);
    return MethodCall.createRegularMethodCall(null, isInstanceMethodDescriptor, arguments);
  }
}
