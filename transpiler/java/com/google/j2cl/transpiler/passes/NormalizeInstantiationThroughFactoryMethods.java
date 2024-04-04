/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.ArrayList;
import java.util.List;

/** Creates factory methods for each constructor to encapsulate instantiations. */
public class NormalizeInstantiationThroughFactoryMethods extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    replaceNewInstancesWithFactoryMethodCalls(type);
    rewriteConstructors(type);
  }

  /** Rewrite NewInstance to be a MethodCall to the $create factory method. */
  private void replaceNewInstancesWithFactoryMethodCalls(Type type) {
    // Replace all the NewInstances in the library AST tree.
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewInstance(NewInstance constructorInvocation) {
            if (constructorInvocation.getTarget().getEnclosingTypeDescriptor().isNative()) {
              return constructorInvocation;
            }
            return MethodCall.Builder.from(
                    getFactoryDescriptorForConstructor(constructorInvocation.getTarget()))
                .setArguments(constructorInvocation.getArguments())
                .build();
          }
        });
  }

  private void rewriteConstructors(Type type) {
    if (type.isNative() || type.getConstructors().isEmpty()) {
      return;
    }
    insertFactoryMethods(type);
    rewriteConstructorsAsCtorMethods(type);
  }

  /** Inserts $create methods for each constructor. */
  private void insertFactoryMethods(Type type) {
    if (type.isAbstract()) {
      return;
    }
    List<Member> members = type.getMembers();
    for (int i = 0; i < members.size(); i++) {
      if (!members.get(i).isConstructor()) {
        continue;
      }
      Method method = (Method) members.get(i);

      // Insert the factory method just before the corresponding constructor, and advance.
      members.add(i++, synthesizeFactoryMethod(method, type.getTypeDescriptor()));
    }
  }

  private static void rewriteConstructorsAsCtorMethods(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            if (!method.isConstructor()) {
              return method;
            }

            return Method.newBuilder()
                .setMethodDescriptor(getCtorMethodDescriptorForConstructor(method.getDescriptor()))
                .setParameters(method.getParameters())
                .addStatements(method.getBody().getStatements())
                .setSourcePosition(method.getSourcePosition())
                .build();
          }

          // Rewrite super() constructor calls.
          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getTarget().isConstructor()) {
              return methodCall;
            }

            return MethodCall.Builder.from(
                    getCtorMethodDescriptorForConstructor(methodCall.getTarget()))
                .setQualifier(
                    methodCall.getQualifier() == null
                        ? new ThisReference(methodCall.getTarget().getEnclosingTypeDescriptor())
                        : methodCall.getQualifier())
                .setArguments(methodCall.getArguments())
                .build();
          }
        });
  }

  /**
   * Generates code of the form:
   *
   * <pre>{@code
   * static $create(args)
   *   Type $instance = new Type(args);
   *   $instance.$ctor...(args);
   *   return $instance;
   * }</pre>
   */
  private Method synthesizeFactoryMethod(Method constructor, DeclaredTypeDescriptor enclosingType) {

    List<Statement> statements = new ArrayList<>();

    List<Variable> factoryMethodParameters = AstUtils.clone(constructor.getParameters());

    Variable newInstance =
        Variable.newBuilder().setName("$instance").setTypeDescriptor(enclosingType).build();

    SourcePosition constructorSourcePosition = constructor.getSourcePosition();

    // Type $instance = new Type();
    Statement newInstanceStatement =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                newInstance,
                NewInstance.Builder.from(constructor.getDescriptor())
                    .setArguments(
                        factoryMethodParameters.stream()
                            .map(Variable::createReference)
                            .collect(toImmutableList()))
                    .build())
            .build()
            .makeStatement(constructorSourcePosition);
    statements.add(newInstanceStatement);

    // $instance.$ctor...();
    Statement ctorCallStatement =
        MethodCall.Builder.from(constructor.getDescriptor())
            .setQualifier(newInstance.createReference())
            .setArguments(AstUtils.getReferences(factoryMethodParameters))
            .build()
            .makeStatement(constructorSourcePosition);
    statements.add(ctorCallStatement);

    if (enclosingType.isAssignableTo(TypeDescriptors.get().javaLangThrowable)) {
      // $instance.privateInitError(Exceptions.createJsError);
      statements.add(
          createThrowableInit(newInstance.createReference())
              .makeStatement(constructorSourcePosition));
    }

    // return $instance
    Statement returnStatement =
        ReturnStatement.newBuilder()
            .setExpression(newInstance.createReference())
            .setSourcePosition(constructorSourcePosition)
            .build();
    statements.add(returnStatement);

    return Method.newBuilder()
        .setMethodDescriptor(getFactoryDescriptorForConstructor(constructor.getDescriptor()))
        .setParameters(factoryMethodParameters)
        .addStatements(statements)
        .setSourcePosition(constructorSourcePosition)
        .build();
  }

  /** Method descriptor for $ctor methods. */
  private static MethodDescriptor getCtorMethodDescriptorForConstructor(
      MethodDescriptor constructor) {
    checkArgument(constructor.isConstructor());
    return constructor.transform(
        builder ->
            builder
                .setReturnTypeDescriptor(PrimitiveTypes.VOID)
                .setName(MethodDescriptor.CTOR_METHOD_PREFIX)
                .setConstructor(false)
                .setStatic(false)
                .setOrigin(MethodOrigin.SYNTHETIC_CTOR_FOR_CONSTRUCTOR)
                // Set to private to avoid putting the method in the vtable. (These methods might
                // be called by a subclass though)
                .setVisibility(Visibility.PRIVATE)
                .setOriginalJsInfo(JsInfo.NONE)
                // Clear side effect free flag since the ctor method does not return a value and
                // would be pruned incorrectly.
                // The factory method still preserves the side effect free flags if it was present.
                .setSideEffectFree(false));
  }

  /** Method descriptor for $create methods. */
  private static MethodDescriptor getFactoryDescriptorForConstructor(MethodDescriptor constructor) {
    checkArgument(constructor.isConstructor());
    return constructor.transform(
        builder ->
            builder
                .setStatic(true)
                .setName(MethodDescriptor.CREATE_METHOD_NAME)
                .setConstructor(false)
                .addTypeParameterTypeDescriptors(
                    0,
                    builder
                        .getEnclosingTypeDescriptor()
                        .getTypeDeclaration()
                        .getTypeParameterDescriptors())
                .setOrigin(MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR)
                .setOriginalJsInfo(JsInfo.NONE)
                .setVisibility(constructor.getVisibility()));
  }

  private static Expression createThrowableInit(Expression newInstanceRef) {
    return RuntimeMethods.createThrowableInitMethodCall(
        newInstanceRef, newInstanceOfError(newInstanceRef.clone()));
  }

  /** Emits {@code Exceptions.createJsError(instance.toString())} */
  private static Expression newInstanceOfError(Expression thisRef) {
    return RuntimeMethods.createExceptionsMethodCall(
        "createJsError",
        MethodCall.Builder.from(
                TypeDescriptors.get().javaLangObject.getMethodDescriptorByName("toString"))
            .setQualifier(thisRef)
            .build());
  }
}
