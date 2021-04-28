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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.LambdaImplementorTypeDescriptors;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableReference;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    List<Type> newLambdaImplementors = new ArrayList<>();

    // (1) Replace each functional expression with an instantiation of the corresponding lambda
    // implementor class; passing the captures as parameters to the constructor.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            TypeDescriptor typeDescriptor = functionExpression.getTypeDescriptor();

            DeclaredTypeDescriptor implementorTypeDescriptor =
                LambdaImplementorTypeDescriptors.createLambdaImplementorTypeDescriptor(
                    typeDescriptor,
                    getCurrentType().getTypeDescriptor(),
                    lambdaCounterPerCompilationUnit++);

            newLambdaImplementors.add(
                createNewLambdaImplementorType(functionExpression, implementorTypeDescriptor));

            ImmutableList.Builder<Expression> argumentsBuilder = new ImmutableList.Builder<>();
            if (functionExpression.isReferencingEnclosingInstance()) {
              argumentsBuilder.add(new ThisReference(getCurrentType().getTypeDescriptor()));
            }
            argumentsBuilder.addAll(
                functionExpression.getCapturedVariables().stream()
                    .map(Variable::createReference)
                    .collect(toImmutableList()));

            // new A$$LambdaImplementor( captures... )
            return NewInstance.newBuilder()
                .setMethodDescriptor(
                    getLambdaImplementorConstructorDescriptor(
                        implementorTypeDescriptor,
                        argumentsBuilder.build().stream()
                            .map(Expression::getTypeDescriptor)
                            .collect(toImmutableList())))
                .setArguments(argumentsBuilder.build())
                .build();
          }
        });

    // (2) Add all the newly created implementors to the compilation unit.
    compilationUnit.addTypes(newLambdaImplementors);
  }

  /**
   * Creates the type that implements the functional interface.
   *
   * <p><code>
   *   class LambdaImplementor implements FunctionalInterface {
   *     private T1 capturedVariable1;
   *         ...
   *     private TN capturedVariablen;
   *
   *     public LambdaImplementor(T1 capturedVariable1, ..., TN capturedVariablen) {
   *       this.capturedVariable1 = capturedVariable1;
   *       ....
   *       this.capturedVariablen = capturedVariablen;
   *     }
   *
   *     public t method(t1 p1, t2 p2, .....) {
   *       ... body of the function expression with captures replaced.
   *     }
   *
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
    //       LambdaImplementor( ...captures...);
    //       ... fields for captures ...
    //       public void accept(String s) {
    //         .. body of function expression with captures replaced...
    //       }
    //   }
    //

    SourcePosition sourcePosition = functionExpression.getSourcePosition();
    implementorTypeDescriptor = implementorTypeDescriptor.toUnparameterizedTypeDescriptor();

    Type lambdaImplementorType =
        new Type(sourcePosition, Visibility.PUBLIC, implementorTypeDescriptor.getTypeDeclaration());

    Map<Node, Field> fieldsByCapture =
        createFieldsForCaptures(functionExpression, implementorTypeDescriptor);

    lambdaImplementorType.addMembers(fieldsByCapture.values());

    lambdaImplementorType.addMember(
        createLambdaImplementorConstructor(
            sourcePosition, implementorTypeDescriptor, fieldsByCapture.values()));

    // public t method(t1 p1, t2 p2, .....) {
    //   ... code from function expression....;
    // }
    lambdaImplementorType.addMember(
        createLambdaMethod(
            sourcePosition, functionExpression, implementorTypeDescriptor, fieldsByCapture));

    return lambdaImplementorType;
  }

  /** Dummy node to use as a key for the capture mapping of the enclosing instance. */
  private static final Node THIS_REFERENCE = new Node() {};

  private static Map<Node, Field> createFieldsForCaptures(
      FunctionExpression functionExpression, DeclaredTypeDescriptor implementorTypeDescriptor) {
    SourcePosition sourcePosition = functionExpression.getSourcePosition();
    Map<Node, Field> fieldsByCapture = new LinkedHashMap<>();
    if (functionExpression.isReferencingEnclosingInstance()) {
      fieldsByCapture.put(
          THIS_REFERENCE,
          Field.Builder.from(
                  FieldDescriptor.newBuilder()
                      .setName("$captured.this")
                      .setTypeDescriptor(implementorTypeDescriptor.getEnclosingTypeDescriptor())
                      .setEnclosingTypeDescriptor(implementorTypeDescriptor)
                      .setVisibility(Visibility.PRIVATE)
                      .setFinal(true)
                      .build())
              .setSourcePosition(sourcePosition)
              .build());
    }

    for (Variable capturedVariable : functionExpression.getCapturedVariables()) {
      fieldsByCapture.put(
          capturedVariable,
          Field.Builder.from(
                  FieldDescriptor.newBuilder()
                      .setName(capturedVariable.getName())
                      .setTypeDescriptor(capturedVariable.getTypeDescriptor())
                      .setEnclosingTypeDescriptor(implementorTypeDescriptor)
                      .setVisibility(Visibility.PRIVATE)
                      .setFinal(true)
                      .build())
              .setSourcePosition(sourcePosition)
              .build());
    }
    return fieldsByCapture;
  }

  /** Creates the constructor for the LambdaImplementor class. */
  private static Method createLambdaImplementorConstructor(
      SourcePosition sourcePosition,
      DeclaredTypeDescriptor implementorTypeDescriptor,
      Iterable<Field> captures) {

    MethodDescriptor implementorConstructorDescriptor =
        getLambdaImplementorConstructorDescriptor(
            implementorTypeDescriptor,
            stream(captures)
                .map(Field::getDescriptor)
                .map(FieldDescriptor::getTypeDescriptor)
                .collect(toImmutableList()));

    List<Variable> parameters = new ArrayList<>();
    List<Statement> captureInitializers = new ArrayList<>();

    for (Field capture : captures) {
      Variable parameter =
          Variable.newBuilder()
              .setName(capture.getDescriptor().getName())
              .setTypeDescriptor(capture.getDescriptor().getTypeDescriptor())
              .setParameter(true)
              .setFinal(true)
              .build();
      parameters.add(parameter);
      captureInitializers.add(
          BinaryExpression.Builder.asAssignmentTo(capture)
              .setRightOperand(parameter)
              .build()
              .makeStatement(sourcePosition));
    }

    return Method.newBuilder()
        .setMethodDescriptor(implementorConstructorDescriptor)
        .setParameters(parameters)
        .addStatements(captureInitializers)
        .setSourcePosition(sourcePosition)
        .build();
  }

  /**
   * Creates the method that implements this lambda.
   *
   * <p>The body of the method is the code in {@code functionExpression} with the references to
   * captured variables replaced by accesses to the corresponding fields.
   */
  private static Method createLambdaMethod(
      SourcePosition sourcePosition,
      FunctionExpression functionExpression,
      DeclaredTypeDescriptor implementorTypeDescriptor,
      Map<Node, Field> fieldsByCapture) {
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

    // The body of the lambda implementation method is just the body of the function with
    // the captures replaced by the corresponding field accesses.
    body.addAll(
        rewriteCaputredReferences(
                sourcePosition,
                implementorTypeDescriptor,
                fieldsByCapture,
                functionExpression.getBody().clone())
            .getStatements());

    return Method.newBuilder()
        .setMethodDescriptor(lambdaMethodDescriptor)
        .setParameters(parameters)
        .addStatements(body)
        .setSourcePosition(sourcePosition)
        .build();
  }

  private static Block rewriteCaputredReferences(
      SourcePosition sourcePosition,
      DeclaredTypeDescriptor implementorTypeDescriptor,
      Map<Node, Field> fieldsByCapture,
      Block body) {
    body.accept(
        new AbstractRewriter() {

          // TODO(rluble): move the computation of captures from CompilationUnitBuilder to a pass.
          @Override
          public boolean shouldProcessNewInstance(NewInstance newInstance) {
            TypeDeclaration targetClass =
                newInstance.getTarget().getEnclosingTypeDescriptor().getTypeDeclaration();
            if (targetClass.isAnonymous()
                && targetClass.getDeclaredFieldDescriptors().stream()
                    .anyMatch(FieldDescriptor::isCapture)) {
              // TODO(b/186272309): Found an anonymous innerclass that has captures inside of a la
              // lambda, throw an exception with a clear message. Not throwing here will result in
              // a NPE later in the compiler which might be difficult for users to diagnose.
              throw new IllegalStateException(
                  "J2WASM does not support anonymous classes in lambdas");
            }
            return true;
          }

          @Override
          public Expression rewriteVariableReference(VariableReference variableReference) {
            Field captureField = fieldsByCapture.get(variableReference.getTarget());
            if (captureField != null) {
              return createCaptureFieldAccess(captureField);
            }
            return variableReference;
          }

          @Override
          public Expression rewriteThisReference(ThisReference thisReference) {
            return createCaptureFieldAccess(fieldsByCapture.get(THIS_REFERENCE));
          }

          @Override
          public Expression rewriteSuperReference(SuperReference superReference) {
            return createCaptureFieldAccess(fieldsByCapture.get(THIS_REFERENCE));
          }

          private FieldAccess createCaptureFieldAccess(Field captureField) {
            return FieldAccess.newBuilder()
                .setTargetFieldDescriptor(captureField.getDescriptor())
                .setQualifier(new ThisReference(implementorTypeDescriptor))
                .setSourcePosition(sourcePosition)
                .build();
          }
        });
    return body;
  }

  /** Returns the MethodDescriptor for the functional method of the LambdaImplementor class. */
  private static MethodDescriptor getLambdaMethodDescriptor(
      DeclaredTypeDescriptor implementorTypeDescriptor) {
    return Iterables.getOnlyElement(implementorTypeDescriptor.getDeclaredMethodDescriptors());
  }

  /** Returns the MethodDescriptor for the constructor of the LambdaImplementor class. */
  private static MethodDescriptor getLambdaImplementorConstructorDescriptor(
      DeclaredTypeDescriptor implementorTypeDescriptor, List<TypeDescriptor> captures) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(implementorTypeDescriptor)
        .setConstructor(true)
        .setParameterTypeDescriptors(captures)
        .build();
  }
}
