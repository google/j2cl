/*
 * Copyright 2017 Google Inc.
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


import com.google.common.collect.Iterables;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.LambdaImplementorTypeDescriptors;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements lambdas by creating a class that implements the required interfaces.
 *
 * <p>The function expression body becomes the implementation of the functional method from the
 * interface.
 */
public class ImplementLambdaExpressionsViaImplementorClasses extends NormalizationPass {

  private int lambdaCounterPerCompilationUnit = 1;

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    // (1) Replace each functional expression with an instantiation of the corresponding lambda
    // implementor class.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            TypeDescriptor typeDescriptor = functionExpression.getTypeDescriptor();

            boolean capturesEnclosingInstance = functionExpression.isCapturingEnclosingInstance();
            DeclaredTypeDescriptor enclosingTypeDescriptor = getCurrentType().getTypeDescriptor();
            DeclaredTypeDescriptor implementorTypeDescriptor =
                LambdaImplementorTypeDescriptors.createLambdaImplementorTypeDescriptor(
                    typeDescriptor,
                    enclosingTypeDescriptor,
                    lambdaCounterPerCompilationUnit++,
                    capturesEnclosingInstance);

            // new A$$LambdaImplementor(...)
            return NewInstance.newBuilder()
                // Set the qualifier since it is expected that expressions after the early
                // qualifier resolution be explicitly qualified.
                .setQualifier(
                    capturesEnclosingInstance
                        ? new ThisReference(getCurrentType().getTypeDescriptor())
                        : null)
                .setAnonymousInnerClass(
                    createNewLambdaImplementorType(functionExpression, implementorTypeDescriptor))
                .setTarget(AstUtils.createImplicitConstructorDescriptor(implementorTypeDescriptor))
                .build();
          }
        });
  }

  /**
   * Creates the type that implements the functional interface.
   *
   * <p><code>
   *   class LambdaImplementor implements FunctionalInterface {
   *     public t method(t1 p1, t2 p2, .....) {
   *       ... body of the function expression.
   *     }
   *   }
   * </code>
   */
  private static Type createNewLambdaImplementorType(
      FunctionExpression functionExpression, DeclaredTypeDescriptor implementorTypeDescriptor) {
    //   interface Consumer<T> {
    //      void accept(T t);
    //   }
    //
    //   Call site:      Object fn = (Consumer<String> and Serializable)  t -> {};
    //
    //
    //   class LambdaImplementor implements Consumer<String>, Serializable {
    //       LambdaImplementor();
    //       public void accept(String s) {
    //         .. body of function expression...
    //       }
    //   }
    //

    SourcePosition sourcePosition = functionExpression.getSourcePosition();
    implementorTypeDescriptor = implementorTypeDescriptor.toUnparameterizedTypeDescriptor();

    Type lambdaImplementorType =
        new Type(sourcePosition, Visibility.PUBLIC, implementorTypeDescriptor.getTypeDeclaration());

    // public t method(t1 p1, t2 p2, .....) {
    //   ... code from function expression....;
    // }
    lambdaImplementorType.addMember(
        createLambdaMethod(sourcePosition, functionExpression, implementorTypeDescriptor));

    return lambdaImplementorType;
  }

  /**
   * Creates the method that implements this lambda.
   *
   * <p>The body of the method is the code in {@code functionExpression}.
   */
  private static Method createLambdaMethod(
      SourcePosition sourcePosition,
      FunctionExpression functionExpression,
      DeclaredTypeDescriptor implementorTypeDescriptor) {
    List<Statement> body = new ArrayList<>();
    List<Variable> parameters = new ArrayList<>();

    // Insert type checking casts in the lambda parameters if needed. The lambda implementor class
    // implements the lambda interface directly without specializing it. However the functional
    // expression might be a specialization and its parameters would then be specialized.
    MethodDescriptor lambdaMethodDescriptor = getLambdaMethodDescriptor(implementorTypeDescriptor);
    for (int i = 0; i < functionExpression.getParameters().size(); i++) {
      Variable lambdaParameter = functionExpression.getParameters().get(i);
      TypeDescriptor functionalMethodParameterType =
          lambdaMethodDescriptor.getDeclarationDescriptor().getParameterTypeDescriptors().get(i);
      if (functionalMethodParameterType.isSameBaseType(lambdaParameter.getTypeDescriptor())) {
        // The parameter has not been specialized, use it as is.
        parameters.add(lambdaParameter);
      } else {
        Variable newParameter =
            Variable.Builder.from(lambdaParameter)
                .setTypeDescriptor(functionalMethodParameterType)
                .build();
        parameters.add(newParameter);
        body.add(
            VariableDeclarationExpression.newBuilder()
                .addVariableDeclaration(
                    lambdaParameter,
                    CastExpression.newBuilder()
                        .setExpression(newParameter.createReference())
                        .setCastTypeDescriptor(functionalMethodParameterType)
                        .build())
                .build()
                .makeStatement(sourcePosition));
      }
    }

    body.addAll(functionExpression.getBody().getStatements());

    return Method.newBuilder()
        .setMethodDescriptor(lambdaMethodDescriptor)
        .setParameters(parameters)
        .addStatements(body)
        .setSourcePosition(sourcePosition)
        .build();
  }

  /** Returns the MethodDescriptor for the functional method of the LambdaImplementor class. */
  private static MethodDescriptor getLambdaMethodDescriptor(
      DeclaredTypeDescriptor implementorTypeDescriptor) {
    return Iterables.getOnlyElement(implementorTypeDescriptor.getDeclaredMethodDescriptors());
  }
}
