/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Transforms certain anonymous inner classes that implement JsFunction interfaces into
 * FunctionExpressions.
 */
public class OptimizeAnonymousInnerClassesToFunctionExpressions {

  public static void applyTo(CompilationUnit compilationUnit) {
    final Map<TypeDescriptor, FunctionExpression> functionExpressionByOptimizableTypeDescriptor =
        new HashMap<>();
    for (Iterator<JavaType> iterator = compilationUnit.getTypes().iterator();
        iterator.hasNext();
        ) {
      JavaType type = iterator.next();
      if (canBeOptimized(type)) {
        functionExpressionByOptimizableTypeDescriptor.put(
            type.getDescriptor(), optimizeToFunctionExpression(type));
        iterator.remove();
      }
    }

    // Replace each instantiation with the corresponding functional expression and all references
    // to the type with that of its JsFunction interface.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            TypeDescriptor targetTypeDescriptor =
                newInstance.getTarget().getEnclosingClassTypeDescriptor();
            if (functionExpressionByOptimizableTypeDescriptor.containsKey(targetTypeDescriptor)) {
              // Rewrites
              //
              //  new JsFunctionInterface() {
              //    @Override
              //    String apply(E e) {
              //      return e.toString();
              //    }
              //  }
              //
              //  to
              //
              //  (E e) -> { return e.toString(); }
              //
              return functionExpressionByOptimizableTypeDescriptor.get(targetTypeDescriptor);
            }
            return newInstance;
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            TypeDescriptor targetTypeDescriptor =
                methodCall.getTarget().getEnclosingClassTypeDescriptor();
            if (functionExpressionByOptimizableTypeDescriptor.containsKey(targetTypeDescriptor)) {
              // The calls that are typed as directly to the anonymous inner class are redirected
              // to be calls though the interface type, e.g.
              //
              //  new JsFunctionInterface() {...}.apply(...)  // apply here is AnonymousClass.apply.
              //
              //  gets transformed so that it is JsFunctionInterface.apply instead.
              //
              return MethodCall.Builder.from(methodCall)
                  .setEnclosingClass(targetTypeDescriptor.getInterfacesTypeDescriptors().get(0))
                  .build();
            }
            return methodCall;
          }
        });
  }

  private static FunctionExpression optimizeToFunctionExpression(final JavaType type) {
    Method jsFunctionMethodImplementation = getSingleDeclaredMethod(type);
    FunctionExpression lambdaMethodImplementaion =
        new FunctionExpression(
            type.getSuperInterfaceTypeDescriptors().get(0),
            jsFunctionMethodImplementation.getParameters(),
            jsFunctionMethodImplementation.getBody().getStatements());

    final Map<FieldDescriptor, Variable> capturesByFieldDescriptor = new HashMap<>();
    for (Field field : type.getFields()) {
      capturesByFieldDescriptor.put(field.getDescriptor(), field.getCapturedVariable());
    }
    lambdaMethodImplementaion.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            // Turn references to captured fields (which are represented as instance fields) back
            // into references to the captured variable.
            if (capturesByFieldDescriptor.containsKey(fieldAccess.getTarget())
                && fieldAccess.getQualifier() instanceof ThisReference) {
              Variable capturedVariable = capturesByFieldDescriptor.get(fieldAccess.getTarget());
              if (capturedVariable != null) {
                return capturedVariable.getReference();
              } else {
                checkState(fieldAccess.getTarget().getFieldName().equals("$outer_this"));
                return new ThisReference(type.getEnclosingTypeDescriptor());
              }
            }
            return fieldAccess;
          }
        });
    return lambdaMethodImplementaion;
  }

  /**
   * Determines whether an inner class that implements a JsFunction interface can be optimized into
   * a function expression (lambda).
   */
  private static boolean canBeOptimized(JavaType type) {
    if (!type.isAnonymous() || !type.getDescriptor().isJsFunctionImplementation()) {
      return false;
    }

    for (Field field : type.getFields()) {
      if (field.getCapturedVariable() == null
          && !field.getDescriptor().getFieldName().equals("$outer_this")) {
        // if there are any fields other than captured variables, bail out.
        return false;
      }
    }

    if (getSingleDeclaredMethod(type) == null) {
      // Can only override a single method.
      return false;
    }

    if (hasThisOrSuperReference(type)) {
      // Can not refer to an itself.
      return false;
    }
    return true;
  }

  private static Method getSingleDeclaredMethod(JavaType type) {
    Method singleDeclaredMethod = null;
    for (Method method : type.getMethods()) {
      if (method.isBridge()) {
        continue;
      }
      if (singleDeclaredMethod != null) {
        return null;
      }
      singleDeclaredMethod = method;
    }
    return singleDeclaredMethod;
  }

  public static boolean hasThisOrSuperReference(final JavaType type) {

    final Set<FieldDescriptor> captures = new HashSet<>();
    for (Field field : type.getFields()) {
      captures.add(field.getDescriptor());
    }

    final boolean[] hasThisOrSuperReference = new boolean[] {false};

    getSingleDeclaredMethod(type)
        .accept(
            new AbstractVisitor() {

              @Override
              public boolean enterFieldAccess(FieldAccess fieldAccess) {
                if (captures.contains(fieldAccess.getTarget())
                    && fieldAccess.getQualifier() instanceof ThisReference) {
                  return false;
                }
                return true;
              }

              @Override
              public boolean enterThisReference(ThisReference expression) {
                hasThisOrSuperReference[0] = true;
                return false;
              }

              @Override
              public boolean enterSuperReference(SuperReference expression) {
                hasThisOrSuperReference[0] = true;
                return false;
              }
            });
    return hasThisOrSuperReference[0];
  }
}
