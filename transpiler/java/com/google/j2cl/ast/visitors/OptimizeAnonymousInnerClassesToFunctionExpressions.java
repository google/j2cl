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

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Predicates;
import com.google.common.collect.Streams;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transforms certain anonymous inner classes that implement JsFunction interfaces into
 * FunctionExpressions.
 */
public class OptimizeAnonymousInnerClassesToFunctionExpressions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    final Map<TypeDescriptor, Type> optimizableJsFunctionsByTypeDescriptor =
        collectOptimizableJsFunctionsByTypeDescriptor(compilationUnit);

    // Replace each instantiation with the corresponding functional expression.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            TypeDescriptor targetTypeDescriptor =
                newInstance.getTarget().getEnclosingClassTypeDescriptor();
            Type optimizableJsFunctionImplementation =
                optimizableJsFunctionsByTypeDescriptor.get(targetTypeDescriptor);
            if (optimizableJsFunctionImplementation != null) {
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
              Set<Variable> enclosingCaptures =
                  Streams.stream(getCurrentType().getFields())
                      .map(Field::getCapturedVariable)
                      .filter(Predicates.notNull())
                      .collect(toImmutableSet());
              return optimizeToFunctionExpression(
                  optimizableJsFunctionImplementation, enclosingCaptures);
            }
            return newInstance;
          }
        });

    // Revmove the inner classes that where optimized away.
    compilationUnit.getTypes().removeAll(optimizableJsFunctionsByTypeDescriptor.values());

    // Replace all references to the type that was optimized away.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            TypeDescriptor targetTypeDescriptor =
                methodCall.getTarget().getEnclosingClassTypeDescriptor();
            if (optimizableJsFunctionsByTypeDescriptor.containsKey(targetTypeDescriptor)) {
              // The calls that are typed as directly to the anonymous inner class are redirected
              // to be calls though the interface type, e.g.
              //
              //  new JsFunctionInterface() {...}.apply(...)  // apply here is AnonymousClass.apply.
              //
              //  gets transformed so that it is JsFunctionInterface.apply instead.
              //
              return MethodCall.Builder.from(methodCall)
                  .setEnclosingClass(
                      targetTypeDescriptor
                          .getJsFunctionMethodDescriptor()
                          .getEnclosingClassTypeDescriptor())
                  .build();
            }
            return methodCall;
          }

          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            if (optimizableJsFunctionsByTypeDescriptor.containsKey(
                fieldAccess.getTarget().getEnclosingClassTypeDescriptor())) {
              // Due to the cascading construction for captures in inner class construction,
              // at the end some field references might be incorrectly referring
              // the removed jsfunction class and need to point to the proper enclosing class.
              return FieldAccess.Builder.from(
                      FieldDescriptor.Builder.from(fieldAccess.getTarget())
                          .setEnclosingClassTypeDescriptor(getCurrentType().getDescriptor())
                          .build())
                  .setQualifier(fieldAccess.getQualifier())
                  .build();
            }
            return fieldAccess;
          }

          @Override
          public Node rewriteThisReference(ThisReference thisReference) {
            if (optimizableJsFunctionsByTypeDescriptor.containsKey(
                thisReference.getTypeDescriptor())) {
              // Due to the cascading construction for captures in inner class construction,
              // at the end some this references might be incorrectly referring
              // the removed jsfunction class and need to point to the proper enclosing class.
              return new ThisReference(getCurrentType().getDescriptor());
            }
            return thisReference;
          }
        });
  }

  /** Converts an anonymous inner class that implements a JsFunction into an FunctionExpression. */
  private static FunctionExpression optimizeToFunctionExpression(
      final Type type, final Set<Variable> enclosingCaptures) {
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
              if (capturedVariable != null && !enclosingCaptures.contains(capturedVariable)) {
                return capturedVariable.getReference();
              } else if (fieldAccess.getTarget().isFieldDescriptorForEnclosingInstance()) {
                return new ThisReference(type.getEnclosingTypeDescriptor());
              }
            }
            return fieldAccess;
          }
        });
    return lambdaMethodImplementaion;
  }

  private static Map<TypeDescriptor, Type> collectOptimizableJsFunctionsByTypeDescriptor(
      CompilationUnit compilationUnit) {
    Map<TypeDescriptor, Type> optimizableJsFunctionsByTypeDescriptor = new HashMap<>();
    for (Type type : compilationUnit.getTypes()) {
      if (canBeOptimized(type)) {
        optimizableJsFunctionsByTypeDescriptor.put(type.getDescriptor(), type);
      }
    }
    return optimizableJsFunctionsByTypeDescriptor;
  }

  /**
   * Determines whether an inner class that implements a JsFunction interface can be optimized into
   * a function expression (lambda).
   */
  private static boolean canBeOptimized(Type type) {
    if (!type.isAnonymous() || !type.getDescriptor().isJsFunctionImplementation()) {
      return false;
    }

    for (Field field : type.getFields()) {
      if (field.getCapturedVariable() == null
          && !field.getDescriptor().isFieldDescriptorForEnclosingInstance()) {
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

  private static Method getSingleDeclaredMethod(Type type) {
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

  public static boolean hasThisOrSuperReference(final Type type) {

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
