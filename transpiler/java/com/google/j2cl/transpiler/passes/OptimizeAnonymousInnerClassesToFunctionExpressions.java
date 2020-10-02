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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Predicates;
import com.google.common.collect.Streams;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.common.SourcePosition;
import java.util.HashMap;
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
                newInstance.getTarget().getEnclosingTypeDescriptor();
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
            DeclaredTypeDescriptor targetTypeDescriptor =
                methodCall.getTarget().getEnclosingTypeDescriptor();
            if (optimizableJsFunctionsByTypeDescriptor.containsKey(targetTypeDescriptor)) {
              // The calls that are typed as directly to the anonymous inner class are redirected
              // to be calls though the interface type, e.g.
              //
              //  new JsFunctionInterface() {...}.apply(...)  // apply here is AnonymousClass.apply.
              //
              //  gets transformed so that it is JsFunctionInterface.apply instead.
              //
              MethodDescriptor methodDescriptor =
                  MethodDescriptor.Builder.from(methodCall.getTarget())
                      .setEnclosingTypeDescriptor(
                          targetTypeDescriptor
                              .getFunctionalInterface()
                              .getJsFunctionMethodDescriptor()
                              .getEnclosingTypeDescriptor())
                      .build();
              return MethodCall.Builder.from(methodCall)
                  .setMethodDescriptor(methodDescriptor)
                  .build();
            }
            return methodCall;
          }

          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            if (optimizableJsFunctionsByTypeDescriptor.containsKey(
                fieldAccess.getTarget().getEnclosingTypeDescriptor())) {
              // Due to the cascading construction for captures in inner class construction,
              // at the end some field references might be incorrectly referring
              // the removed jsfunction class and need to point to the proper enclosing class.
              return FieldAccess.Builder.from(
                      FieldDescriptor.Builder.from(fieldAccess.getTarget())
                          .setEnclosingTypeDescriptor(getCurrentType().getTypeDescriptor())
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
              return new ThisReference(getCurrentType().getTypeDescriptor());
            }
            return thisReference;
          }
        });
  }

  /** Converts an anonymous inner class that implements a JsFunction into an FunctionExpression. */
  private static FunctionExpression optimizeToFunctionExpression(
      final Type type, final Set<Variable> enclosingCaptures) {
    Method jsFunctionMethodImplementation = getSingleDeclaredMethod(type);
    DeclaredTypeDescriptor jsFunctionTypeDescriptor =
        type.getSuperInterfaceTypeDescriptors().get(0);
    checkState(jsFunctionTypeDescriptor.isJsFunctionInterface());
    FunctionExpression lambdaMethodImplementation =
        FunctionExpression.newBuilder()
            .setTypeDescriptor(jsFunctionTypeDescriptor)
            .setParameters(jsFunctionMethodImplementation.getParameters())
            .setStatements(jsFunctionMethodImplementation.getBody().getStatements())
            .setSourcePosition(
                SourcePosition.Builder.from(jsFunctionMethodImplementation.getSourcePosition())
                    .setName(jsFunctionMethodImplementation.getQualifiedBinaryName())
                    .build())
            .build();

    final Map<FieldDescriptor, Variable> capturesByFieldDescriptor = new HashMap<>();
    for (Field field : type.getFields()) {
      capturesByFieldDescriptor.put(field.getDescriptor(), field.getCapturedVariable());
    }
    lambdaMethodImplementation.accept(
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
              } else if (fieldAccess.getTarget().isEnclosingInstanceCapture()) {
                return new ThisReference(
                    type.getEnclosingTypeDeclaration().toUnparameterizedTypeDescriptor());
              }
            }
            return fieldAccess;
          }
        });
    return lambdaMethodImplementation;
  }

  private static Map<TypeDescriptor, Type> collectOptimizableJsFunctionsByTypeDescriptor(
      CompilationUnit compilationUnit) {
    Map<TypeDescriptor, Type> optimizableJsFunctionsByTypeDescriptor = new HashMap<>();
    for (Type type : compilationUnit.getTypes()) {
      if (canBeOptimized(type)) {
        optimizableJsFunctionsByTypeDescriptor.put(type.getTypeDescriptor(), type);
      }
    }
    return optimizableJsFunctionsByTypeDescriptor;
  }

  /**
   * Determines whether an inner class that implements a JsFunction interface can be optimized into
   * a function expression (lambda).
   */
  private static boolean canBeOptimized(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    if (!typeDeclaration.isAnonymous() || !typeDeclaration.isJsFunctionImplementation()) {
      return false;
    }

    // Do not optimize if there are fields that are not captures.
    if (!Streams.stream(type.getFields())
        .map(Field::getDescriptor)
        .allMatch(FieldDescriptor::isCapture)) {
      return false;
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
      if (method.isBridge() || method.isConstructor()) {
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

    final boolean[] hasThisOrSuperReference = new boolean[] {false};

    getSingleDeclaredMethod(type)
        .accept(
            new AbstractVisitor() {
              @Override
              public boolean enterFieldAccess(FieldAccess fieldAccess) {
                if (fieldAccess.getTarget().isMemberOf(type.getTypeDescriptor())
                    && fieldAccess.getQualifier() instanceof ThisReference) {
                  // Skip "this" references when accessing captures.
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
