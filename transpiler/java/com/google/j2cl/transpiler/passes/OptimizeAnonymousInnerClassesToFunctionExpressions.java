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


import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Transforms certain anonymous inner classes that implement JsFunction interfaces into
 * FunctionExpressions.
 */
public class OptimizeAnonymousInnerClassesToFunctionExpressions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    Set<TypeDeclaration> unoptimizeableClasses = new HashSet<>();

    // 1. Collects types that cannot be optimized due to the type having been propagated by
    // inference.
    //
    // There are two ways that the type can be leaked:
    //  - to a variable with inferred type (i.e. declared with var), this case we handle by
    //    changing the type of the variable.
    //  - to a parameter in a method. This case we backoff.
    //  - to an array literal. This case we backoff.
    //  - and potentially to a field. Which we also backoff.
    //
    // The type might end up in a deep parameterization but for that to occur there needs to be
    // a parameter assignment with the type.
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethodDescriptor(MethodDescriptor methodDescriptor) {
            methodDescriptor.getParameterTypeDescriptors().stream()
                .map(TypeDescriptor::toRawTypeDescriptor)
                .forEach(this::backoffType);
          }

          @Override
          public void exitFieldDescriptor(FieldDescriptor fieldDescriptor) {
            backoffType(fieldDescriptor.getTypeDescriptor().toRawTypeDescriptor());
          }

          @Override
          public void exitArrayTypeDescriptor(ArrayTypeDescriptor arrayTypeDescriptor) {
            backoffType(arrayTypeDescriptor.getLeafTypeDescriptor().toRawTypeDescriptor());
          }

          private void backoffType(TypeDescriptor typeDescriptor) {
            if (typeDescriptor instanceof ArrayTypeDescriptor) {
              // Handle varargs and array literals.
              backoffType(((ArrayTypeDescriptor) typeDescriptor).getLeafTypeDescriptor());
            } else if (typeDescriptor instanceof DeclaredTypeDescriptor) {
              TypeDeclaration typeDeclaration =
                  ((DeclaredTypeDescriptor) typeDescriptor).getTypeDeclaration();
              if (typeDeclaration.isAnonymous()) {
                unoptimizeableClasses.add(typeDeclaration);
              }
            }
          }
        });

    // Keeps track of the classes that were optimized away to fix references.
    Set<TypeDeclaration> optimizedClasses = new HashSet<>();

    // 2. Replace each instantiation with the corresponding functional expression.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            Type anonymousInnerClass = newInstance.getAnonymousInnerClass();
            if (anonymousInnerClass != null
                && !unoptimizeableClasses.contains(anonymousInnerClass.getDeclaration())
                && canBeOptimized(anonymousInnerClass)) {
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
              optimizedClasses.add(anonymousInnerClass.getDeclaration());
              return optimizeToFunctionExpression(anonymousInnerClass);
            }
            return newInstance;
          }
        });

    // 3. Replace all references to the type that was optimized away.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor target = methodCall.getTarget();
            DeclaredTypeDescriptor targetTypeDescriptor = target.getEnclosingTypeDescriptor();
            if (optimizedClasses.contains(targetTypeDescriptor.getTypeDeclaration())
                && target.isOverride(
                    targetTypeDescriptor
                        .getFunctionalInterface()
                        .getSingleAbstractMethodDescriptor())) {
              // The calls that are typed as directly to the anonymous inner class are redirected
              // to be calls though the interface type, e.g.
              //
              //  new JsFunctionInterface() {...}.apply(...)  // apply here is AnonymousClass.apply.
              //
              //  gets transformed so that it is JsFunctionInterface.apply instead.
              //
              return MethodCall.Builder.from(methodCall)
                  .setTarget(
                      targetTypeDescriptor
                          .getFunctionalInterface()
                          .getSingleAbstractMethodDescriptor())
                  .build();
            }
            return methodCall;
          }

          @Override
          public TypeDescriptor rewriteDeclaredTypeDescriptor(
              DeclaredTypeDescriptor declaredTypeDescriptor) {
            TypeDeclaration typeDeclaration = declaredTypeDescriptor.getTypeDeclaration();
            if (optimizedClasses.contains(typeDeclaration)) {
              return declaredTypeDescriptor.getFunctionalInterface();
            }
            return declaredTypeDescriptor;
          }

          @Override
          public TypeDeclaration rewriteTypeDeclaration(TypeDeclaration typeDeclaration) {
            if (optimizedClasses.contains(typeDeclaration)) {
              return typeDeclaration.toDescriptor().getFunctionalInterface().getTypeDeclaration();
            }
            return typeDeclaration;
          }
        });
  }

  /**
   * Converts an anonymous inner class that implements a FunctionalInterface into an
   * FunctionExpression.
   */
  private static FunctionExpression optimizeToFunctionExpression(final Type type) {
    Method jsFunctionMethodImplementation = getSingleDeclaredMethod(type);
    DeclaredTypeDescriptor jsFunctionTypeDescriptor =
        type.getTypeDescriptor().getFunctionalInterface();
    return FunctionExpression.newBuilder()
        .setTypeDescriptor(jsFunctionTypeDescriptor)
        .setParameters(jsFunctionMethodImplementation.getParameters())
        .setStatements(jsFunctionMethodImplementation.getBody().getStatements())
        .setJsAsync(
            jsFunctionMethodImplementation.getDescriptor().isJsAsync()
                || jsFunctionTypeDescriptor.getSingleAbstractMethodDescriptor().isJsAsync())
        .setSourcePosition(
            SourcePosition.Builder.from(jsFunctionMethodImplementation.getSourcePosition())
                .setName(jsFunctionMethodImplementation.getQualifiedBinaryName())
                .build())
        .build();
  }

  /**
   * Determines whether an inner class that implements a funcitonal interface can be optimized into
   * a function expression (lambda).
   */
  private static boolean canBeOptimized(Type type) {
    TypeDeclaration typeDeclaration = type.getDeclaration();
    if (!typeDeclaration.isAnonymous()) {
      return false;
    }

    if (!TypeDescriptors.isJavaLangObject(typeDeclaration.getSuperTypeDescriptor())
        || typeDeclaration.getInterfaceTypeDescriptors().size() != 1) {
      return false;
    }

    if (!typeDeclaration.getInterfaceTypeDescriptors().get(0).isFunctionalInterface()) {
      return false;
    }

    // Do not optimize if the class declares fields.
    if (!type.getFields().isEmpty()) {
      return false;
    }
    Method lambdaMethod = getSingleDeclaredMethod(type);
    if (lambdaMethod == null) {
      // Can only override a single method.
      return false;
    }

    if (!lambdaMethod.getDescriptor().getTypeParameterTypeDescriptors().isEmpty()) {
      // Type parameters in the lambda method are not expressible in Closure, avoid
      // optimizing this uncommon case.
      return false;
    }

    if (hasThisOrSuperReference(type)) {
      // Can not refer to itself.
      return false;
    }
    return true;
  }

  @Nullable
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
              public boolean enterThisOrSuperReference(ThisOrSuperReference expression) {
                if (expression
                    .getTypeDescriptor()
                    .getTypeDeclaration()
                    .equals(type.getDeclaration())) {
                  hasThisOrSuperReference[0] = true;
                }
                return false;
              }
            });
    return hasThisOrSuperReference[0];
  }
}
