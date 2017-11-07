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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.j2cl.ast.LambdaTypeDescriptors.createJsFunctionTypeDescriptor;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.LambdaTypeDescriptors;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.common.SourcePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implements lambdas by treating the lambda as a JavaScript function that is wrapped in a generic
 * adaptor that implements the required Java interfaces.
 */
public class ImplementLambdaExpressions extends NormalizationPass {

  private int lambdaCounterPerCompilationUnit = 1;

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    List<Type> newLambdaAdaptors = new ArrayList<>();

    // (1) Create LambdaAdaptor classes for each qualifying functional interface in the current
    // compilation unit and add the corresponding $adapt method to the functional interface.
    // Qualifying functional interfaces are functional interfaces that are not
    // @JsFunction nor @JsType(isNative=true)
    for (Type type : compilationUnit.getTypes()) {
      if (type.getDeclaration().isFunctionalInterface()
          && !type.getDeclaration().isJsFunctionInterface()
          && !type.getDeclaration().isNative()) {

        DeclaredTypeDescriptor adaptorTypeDescriptor =
            LambdaTypeDescriptors.createLambdaAdaptorTypeDescriptor(type.getTypeDescriptor());
        DeclaredTypeDescriptor jsFunctionTypeDescriptor =
            LambdaTypeDescriptors.createJsFunctionTypeDescriptor(type.getTypeDescriptor());

        // Create and add the LambdaAdaptor type for the functional interface.
        newLambdaAdaptors.add(
            createLambdaAdaptorType(
                type.getSourcePosition(), type.getTypeDescriptor(), adaptorTypeDescriptor));

        // Add the $adapt method to the functional interface to hide the adaptor class..
        addAdaptMethod(type, jsFunctionTypeDescriptor, adaptorTypeDescriptor);
      }
    }

    // (2) Replace each instantiation with the corresponding functional expression, and create
    // the LambdaAdaptor classes intersection types and @JsType(isNative=true) lambdas.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
            TypeDescriptor typeDescriptor = functionExpression.getTypeDescriptor();
            if (typeDescriptor.isJsFunctionInterface()) {
              return functionExpression;
            }

            // TODO(b/65977332): Do not generate a per instance adaptor for native functional
            // interfaces.
            if (typeDescriptor.isIntersection() || typeDescriptor.isNative()) {
              // For these cases create a per instance adaptor.

              DeclaredTypeDescriptor adaptorTypeDescriptor =
                  LambdaTypeDescriptors.createLambdaAdaptorTypeDescriptor(
                      typeDescriptor,
                      getCurrentType().getTypeDescriptor(),
                      Optional.of(lambdaCounterPerCompilationUnit++));

              newLambdaAdaptors.add(
                  createLambdaAdaptorType(
                      functionExpression.getSourcePosition(),
                      typeDescriptor,
                      adaptorTypeDescriptor));

              // new A$$LambdaAdaptor( (...) -> {...} )
              return NewInstance.newBuilder()
                  .setMethodDescriptor(adaptorTypeDescriptor.getSingleConstructor())
                  .setArguments(
                      FunctionExpression.Builder.from(functionExpression)
                          // Change the function expression type from the functional interface to
                          // the corresponding synthetic @JsFunction interface.
                          .setTypeDescriptor(
                              createJsFunctionTypeDescriptor(
                                  functionExpression.getTypeDescriptor()))
                          .build())
                  .build();
            }

            DeclaredTypeDescriptor functionalInterfaceTypeDescriptor =
                (DeclaredTypeDescriptor) typeDescriptor;
            // A.$adapt((...) -> {...})
            return MethodCall.Builder.from(
                    getAdaptMethodDescriptor(
                        functionalInterfaceTypeDescriptor,
                        LambdaTypeDescriptors.createJsFunctionTypeDescriptor(
                            functionalInterfaceTypeDescriptor)))
                .setArguments(
                    FunctionExpression.Builder.from(functionExpression)
                        .setTypeDescriptor(
                            createJsFunctionTypeDescriptor(functionExpression.getTypeDescriptor()))
                        .build())
                .build();
          }
        });

    // (3) Add all the newly created adaptors to the compilation unit.
    compilationUnit.addTypes(newLambdaAdaptors);
  }

  /** Adds the $adapt method to the functional interface. */
  private void addAdaptMethod(
      Type functionalInterfaceType,
      DeclaredTypeDescriptor jsFunctionTypeDescriptor,
      DeclaredTypeDescriptor adaptorTypeDescriptor) {

    DeclaredTypeDescriptor functionalInterfaceTypeTypeDescriptor =
        functionalInterfaceType.getTypeDescriptor();
    checkArgument(
        functionalInterfaceTypeTypeDescriptor.isFunctionalInterface()
            && !functionalInterfaceTypeTypeDescriptor.isNative()
            && !functionalInterfaceTypeTypeDescriptor.isJsFunctionInterface());

    MethodDescriptor adaptMethodDescriptor =
        getAdaptMethodDescriptor(functionalInterfaceTypeTypeDescriptor, jsFunctionTypeDescriptor);

    Variable jsFunctionParameter =
        Variable.newBuilder()
            .setName("fn")
            .setTypeDescriptor(jsFunctionTypeDescriptor)
            .setParameter(true)
            .setFinal(true)
            .build();
    MethodDescriptor adaptorConstructor = adaptorTypeDescriptor.getSingleConstructor();
    functionalInterfaceType.addMethod(
        // FunctionalInterface$$LambdaAdaptor $adapt(
        //     FunctionalInterface$$LambdaAdaptor$$JsFunctionInterface fn) {
        //   return new FunctionalInterface$$LambdaAadaptor(fn);
        // }
        Method.newBuilder()
            .setMethodDescriptor(adaptMethodDescriptor)
            .setParameters(jsFunctionParameter)
            .addStatements(
                ReturnStatement.newBuilder()
                    .setExpression(
                        NewInstance.newBuilder()
                            .setMethodDescriptor(adaptorConstructor)
                            .setArguments(jsFunctionParameter.getReference())
                            .build())
                    .setTypeDescriptor(adaptorConstructor.getReturnTypeDescriptor())
                    .setSourcePosition(functionalInterfaceType.getSourcePosition())
                    .build())
            .setSourcePosition(functionalInterfaceType.getSourcePosition())
            .build());
  }

  private MethodDescriptor getAdaptMethodDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor, TypeDescriptor jsFunctionTypeDescriptor) {
    return MethodDescriptor.newBuilder()
        .setName("$adapt")
        .setJsInfo(JsInfo.RAW)
        .setStatic(true)
        .setParameterTypeDescriptors(jsFunctionTypeDescriptor)
        .setTypeParameterTypeDescriptors(jsFunctionTypeDescriptor.getAllTypeVariables())
        .setReturnTypeDescriptor(enclosingTypeDescriptor)
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setOrigin(MethodOrigin.SYNTHETIC_ADAPT_LAMBDA)
        .build();
  }

  /**
   * Creates the type that adapts the functional interface to a JsFunction interface.
   *
   * <p><code>
   *   class FunctionalInterface$$LambdaAdaptor implements FunctionalInterface {
   *     private FunctionalInterface$$JsFunction $$fn;
   *
   *     public FunctionalInterface$$LambdaAdaptor(FunctionalInterface$$JsFunction fn) {
   *       this.$$fn = fn;
   *     }
   *
   *     public t method(t1 p1, t2 p2, .....) {
   *       return this.$$fn.method(p1, p2, ....);
   *     }
   *
   *   }
   * </code>
   */
  private Type createLambdaAdaptorType(
      SourcePosition sourcePosition,
      TypeDescriptor typeDescriptor,
      DeclaredTypeDescriptor adaptorTypeDescriptor) {

    adaptorTypeDescriptor = adaptorTypeDescriptor.unparameterizedTypeDescriptor();

    DeclaredTypeDescriptor jsFunctionTypeDescriptor =
        LambdaTypeDescriptors.createJsFunctionTypeDescriptor(typeDescriptor);
    Type adaptorType =
        new Type(sourcePosition, Visibility.PUBLIC, adaptorTypeDescriptor.getTypeDeclaration());

    // Create the field to contain the lambda function as a JsFunction.
    FieldDescriptor jsFunctionFieldDescriptor =
        FieldDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(adaptorTypeDescriptor)
            .setName("$$fn")
            .setTypeDescriptor(jsFunctionTypeDescriptor)
            .build();
    adaptorType.addField(
        Field.Builder.from(jsFunctionFieldDescriptor).setSourcePosition(sourcePosition).build());

    // Create the forwarding method that forwards calls to the functional interface method to
    // the JsFunction lambda.
    MethodDescriptor adaptorForwarderMethodDescriptor =
        LambdaTypeDescriptors.getAdaptorForwardingMethod(adaptorTypeDescriptor);

    MethodDescriptor jsFunctionMethodDescriptor =
        MethodDescriptor.newBuilder()
            .from(jsFunctionTypeDescriptor.getSingleAbstractMethodDescriptor())
            .setEnclosingTypeDescriptor(jsFunctionTypeDescriptor)
            .build();

    adaptorType.addMethod(
        //  @JsConstructor
        //  FunctionalInterface$$LambdaAdaptor(FunctionalInterface$$JsFunction fn) {
        //    this.$$fn = fn;
        //  }
        createLambdaAdaptorConstructor(
            sourcePosition,
            adaptorTypeDescriptor,
            jsFunctionTypeDescriptor,
            jsFunctionFieldDescriptor));
    adaptorType.addMethod(
        // public t method(t1 p1, t2 p2, .....) {
        //   return this.$$fn.method(p1, p2, ....);
        // }
        AstUtils.createForwardingMethod(
            sourcePosition,
            FieldAccess.Builder.from(jsFunctionFieldDescriptor).build(),
            adaptorForwarderMethodDescriptor,
            jsFunctionMethodDescriptor,
            null,
            false));

    return adaptorType;
  }

  /** Creates the constructor for the lambda adaptor class. */
  private Method createLambdaAdaptorConstructor(
      SourcePosition sourcePosition,
      DeclaredTypeDescriptor adaptorTypeDescriptor,
      DeclaredTypeDescriptor jsFunctionTypeDescriptor,
      FieldDescriptor jsFunctionFieldDescriptor) {

    MethodDescriptor adaptorConstructor = adaptorTypeDescriptor.getSingleConstructor();

    // The constructor receives the JsFunction lambda as a parameter.
    Variable jsFunctionParameter =
        Variable.newBuilder()
            .setFinal(true)
            .setParameter(true)
            .setName("fn")
            .setTypeDescriptor(jsFunctionTypeDescriptor)
            .build();

    return Method.newBuilder()
        .setMethodDescriptor(adaptorConstructor)
        .setParameters(jsFunctionParameter)
        .addStatements(
            // Store the JsFunction lambda in the corresponding field.
            // this.$$fn = fn;
            BinaryExpression.Builder.asAssignmentTo(jsFunctionFieldDescriptor)
                .setRightOperand(jsFunctionParameter)
                .build()
                .makeStatement(sourcePosition))
        .setSourcePosition(sourcePosition)
        .build();
  }
}
