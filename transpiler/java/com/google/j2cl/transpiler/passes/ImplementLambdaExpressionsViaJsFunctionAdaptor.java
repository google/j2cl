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

import static com.google.j2cl.transpiler.ast.LambdaAdaptorTypeDescriptors.createJsFunctionTypeDescriptor;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.LambdaAdaptorTypeDescriptors;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implements lambdas by treating the lambda as a JavaScript function that is wrapped in a generic
 * adaptor that implements the required Java interfaces.
 */
public class ImplementLambdaExpressionsViaJsFunctionAdaptor extends NormalizationPass {

  private int lambdaCounterPerCompilationUnit = 1;

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    List<Type> newLambdaAdaptors = new ArrayList<>();

    // (1) Create LambdaAdaptor classes for each qualifying functional interface in the current
    // compilation unit and add the corresponding $adapt method to the functional interface.
    // Qualifying functional interfaces are functional interfaces that are not
    // @JsFunction nor @JsType(isNative=true)
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitType(Type type) {
            DeclaredTypeDescriptor typeDescriptor = type.getTypeDescriptor();
            if (hasSharedLambdaAdaptor(typeDescriptor)) {

              DeclaredTypeDescriptor adaptorTypeDescriptor =
                  LambdaAdaptorTypeDescriptors.createLambdaAdaptorTypeDescriptor(typeDescriptor);
              DeclaredTypeDescriptor jsFunctionTypeDescriptor =
                  LambdaAdaptorTypeDescriptors.createJsFunctionTypeDescriptor(
                      typeDescriptor.getFunctionalInterface());

              // Create and add the LambdaAdaptor type for the functional interface.
              newLambdaAdaptors.add(
                  createLambdaAdaptorType(
                      type.getSourcePosition(), typeDescriptor, adaptorTypeDescriptor));

              // Add the $adapt method to the functional interface to hide the adaptor class..
              addAdaptMethod(type, jsFunctionTypeDescriptor, adaptorTypeDescriptor);
            }
          }
        });

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

            if (!hasSharedLambdaAdaptor(typeDescriptor)) {
              // For these cases create a per instance adaptor.

              DeclaredTypeDescriptor adaptorTypeDescriptor =
                  LambdaAdaptorTypeDescriptors.createLambdaAdaptorTypeDescriptor(
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
                  .setTarget(adaptorTypeDescriptor.getSingleConstructor())
                  .setArguments(
                      FunctionExpression.Builder.from(functionExpression)
                          // Change the function expression type from the functional interface to
                          // the corresponding synthetic @JsFunction interface.
                          .setTypeDescriptor(
                              createJsFunctionTypeDescriptor(
                                  functionExpression.getTypeDescriptor().getFunctionalInterface()))
                          .build())
                  .build();
            }

            DeclaredTypeDescriptor functionalInterfaceTypeDescriptor =
                typeDescriptor.getFunctionalInterface();
            // A.$adapt((...) -> {...})
            return MethodCall.Builder.from(
                    getAdaptMethodDescriptor(
                        functionalInterfaceTypeDescriptor,
                        LambdaAdaptorTypeDescriptors.createJsFunctionTypeDescriptor(
                            functionalInterfaceTypeDescriptor)))
                .setArguments(
                    FunctionExpression.Builder.from(functionExpression)
                        .setTypeDescriptor(
                            createJsFunctionTypeDescriptor(functionalInterfaceTypeDescriptor))
                        .build())
                .build();
          }
        });

    // (3) Add all the newly created adaptors to the compilation unit.
    compilationUnit.addTypes(newLambdaAdaptors);
  }

  private static boolean hasSharedLambdaAdaptor(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isJsFunctionInterface()) {
      // JsFunctions are plain JavaScript functions there is no Java class abstraction hence there
      // is no need for an adaptor.
      return false;
    }

    if (typeDescriptor.isNative() && !typeDescriptor.isAnnotatedWithFunctionalInterface()) {
      // A native contract might be defined with a single method and an interface but that doesn't
      // guarantee that its JavaScript definition is an interface nor that it has single method. So
      // J2CL cannot generate lambda adaptor blindly in such cases as these well end up being
      // type check errors in JsCompiler.
      // J2CL requires the developer to mark the intent to use such interfaces with
      // @FunctionalInterface to use a shared lambda adaptor. They can still be used with lambdas
      // even if they are not marked with @FunctionalInterface where J2CL will produce a specific
      // lambda adaptor per usage site.
      return false;
    }

    return typeDescriptor.isFunctionalInterface();
  }

  /** Adds the $adapt method to the functional interface. */
  private void addAdaptMethod(
      Type functionalInterfaceType,
      DeclaredTypeDescriptor jsFunctionTypeDescriptor,
      DeclaredTypeDescriptor adaptorTypeDescriptor) {

    DeclaredTypeDescriptor functionalInterfaceTypeTypeDescriptor =
        functionalInterfaceType.getTypeDescriptor();

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
    // FunctionalInterface$$LambdaAdaptor $adapt(
    //     FunctionalInterface$$LambdaAdaptor$$JsFunctionInterface fn) {
    //   return new FunctionalInterface$$LambdaAadaptor(fn);
    // }
    functionalInterfaceType.addMember(
        Method.newBuilder()
            .setMethodDescriptor(adaptMethodDescriptor)
            .setParameters(jsFunctionParameter)
            .addStatements(
                ReturnStatement.newBuilder()
                    .setExpression(
                        NewInstance.newBuilder()
                            .setTarget(adaptorConstructor)
                            .setArguments(jsFunctionParameter.createReference())
                            .build())
                    .setSourcePosition(functionalInterfaceType.getSourcePosition())
                    .build())
            .setSourcePosition(functionalInterfaceType.getSourcePosition())
            .build());
  }

  private MethodDescriptor getAdaptMethodDescriptor(
      DeclaredTypeDescriptor enclosingTypeDescriptor, TypeDescriptor jsFunctionTypeDescriptor) {
    return MethodDescriptor.newBuilder()
        .setName("$adapt")
        .setOriginalJsInfo(enclosingTypeDescriptor.isNative() ? JsInfo.RAW_OVERLAY : JsInfo.RAW)
        .setStatic(true)
        .setParameterTypeDescriptors(jsFunctionTypeDescriptor)
        .setTypeParameterTypeDescriptors(jsFunctionTypeDescriptor.getAllTypeVariables())
        .setReturnTypeDescriptor(enclosingTypeDescriptor.toNonNullable())
        .setEnclosingTypeDescriptor(enclosingTypeDescriptor)
        .setOrigin(MethodOrigin.SYNTHETIC_ADAPT_LAMBDA)
        .build();
  }

  /**
   * Creates the type that adapts the functional interface to a JsFunction interface.
   *
   * <p><code>
   *   class FunctionalInterface$$LambdaAdaptor implements FunctionalInterface {
   *     private FunctionalInterface$$JsFunction fn;
   *
   *     public FunctionalInterface$$LambdaAdaptor(FunctionalInterface$$JsFunction fn) {
   *       this.fn = fn;
   *     }
   *
   *     public t method(t1 p1, t2 p2, .....) {
   *       return this.fn.method(p1, p2, ....);
   *     }
   *
   *   }
   * </code>
   */
  private Type createLambdaAdaptorType(
      SourcePosition sourcePosition,
      TypeDescriptor typeDescriptor,
      DeclaredTypeDescriptor adaptorTypeDescriptor) {

    // In most cases, e.g. when the adaptor type is shared, adaptorTypeDescriptor is already
    // an the declaration version of the type descriptor since the creation of the adaptor is driven
    // from the declaration of the functional interface and not from a usage.
    // In the cases where the adaptor is not shared and driven from a usage (e.g. intersection
    // types) the adaptor class could either use the paramterization found in the usage
    // or use the more general parameterization from the declaration (e.g. if it is an intersection
    // that should come from the declarations of all types in the intersection).
    // The choice made here is to have the more general adaptor and that results in an inference
    // by jscompiler in the call site, mimicking the case for the shared adaptors.
    //
    // Example:
    //   interface Consumer<T> {
    //      void accept(T t);
    //   }
    //
    //   Callsite:      Object fn = (Consumer<String> and Serializable)  t -> {};
    //
    //   Adaptor declaration: (this is the one generated below),
    //
    //   class Consumer$Adaptor<T> implements Consumer<T>, Serializable {
    //       Consumer$Adaptor( /** function(T):void */ fn);
    //   }
    //
    //   Parameterized adaptor:
    //
    //   class Consumer$Adaptor implements Consumer<String>, Serializable {
    //       Consumer$Adaptor( /** function(String):void */ fn);
    //   }
    //
    adaptorTypeDescriptor = adaptorTypeDescriptor.getDeclarationDescriptor();

    DeclaredTypeDescriptor jsFunctionTypeDescriptor =
        LambdaAdaptorTypeDescriptors.createJsFunctionTypeDescriptor(
            typeDescriptor.getFunctionalInterface().getDeclarationDescriptor());
    Type adaptorType = new Type(sourcePosition, adaptorTypeDescriptor.getTypeDeclaration());

    // Create the field to contain the lambda function as a JsFunction.
    FieldDescriptor jsFunctionFieldDescriptor =
        FieldDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(adaptorTypeDescriptor)
            .setName("fn")
            .setTypeDescriptor(jsFunctionTypeDescriptor)
            .build();
    adaptorType.addMember(
        Field.Builder.from(jsFunctionFieldDescriptor).setSourcePosition(sourcePosition).build());

    // Create the forwarding method that forwards calls to the functional interface method to
    // the JsFunction lambda.
    MethodDescriptor adaptorForwarderMethodDescriptor =
        LambdaAdaptorTypeDescriptors.getAdaptorForwardingMethod(adaptorTypeDescriptor);

    MethodDescriptor jsFunctionMethodDescriptor =
        MethodDescriptor.Builder.from(jsFunctionTypeDescriptor.getSingleAbstractMethodDescriptor())
            .setEnclosingTypeDescriptor(jsFunctionTypeDescriptor)
            .build();

    //  @JsConstructor
    //  FunctionalInterface$$LambdaAdaptor(FunctionalInterface$$JsFunction fn) {
    //    this.fn = fn;
    //  }
    adaptorType.addMember(
        createLambdaAdaptorConstructor(
            sourcePosition,
            adaptorTypeDescriptor,
            jsFunctionTypeDescriptor,
            jsFunctionFieldDescriptor));
    // public t method(t1 p1, t2 p2, .....) {
    //   return this.fn.method(p1, p2, ....);
    // }
    adaptorType.addMember(
        AstUtils.createForwardingMethod(
            sourcePosition,
            FieldAccess.Builder.from(jsFunctionFieldDescriptor)
                .setDefaultInstanceQualifier()
                .build(),
            adaptorForwarderMethodDescriptor,
            jsFunctionMethodDescriptor,
            /* jsDocDescription */ null));

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
            // this.fn = fn;
            BinaryExpression.Builder.asAssignmentTo(jsFunctionFieldDescriptor)
                .setRightOperand(jsFunctionParameter)
                .build()
                .makeStatement(sourcePosition))
        .setSourcePosition(sourcePosition)
        .build();
  }
}
