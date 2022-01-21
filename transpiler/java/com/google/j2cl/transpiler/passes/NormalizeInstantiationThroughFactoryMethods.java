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

import com.google.common.collect.Iterables;
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
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Creates factory methods for each constructor to encapsulate instantiations. */
public class NormalizeInstantiationThroughFactoryMethods extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    rewriteNewInstances(type);
    rewriteConstructors(type);
  }

  /** Rewrite NewInstance to be a MethodCall to the $create factory method. */
  private static void rewriteNewInstances(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewInstance(NewInstance constructorInvocation) {
            MethodDescriptor originalConstructor = constructorInvocation.getTarget();
            return MethodCall.Builder.from(getFactoryDescriptorForConstructor(originalConstructor))
                .setArguments(AstUtils.clone(constructorInvocation.getArguments()))
                .build();
          }
        });
  }

  private static void rewriteConstructors(Type type) {
    if (type.getConstructors().isEmpty()) {
      // No constructors => no normalization.
      return;
    }

    insertFactoryMethods(type);
    rewriteConstructorsAsCtorMethods(type);
  }

  /** Inserts $create methods for each constructor. */
  private static void insertFactoryMethods(Type type) {
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
   *   Type $instance = new Type(<original arguments>);
   *   $instance.$ctor...(args);
   *   return $instance;
   * }</pre>
   */
  private static Method synthesizeFactoryMethod(
      Method constructor, DeclaredTypeDescriptor enclosingType) {

    List<Statement> statements = new ArrayList<>();

    List<Variable> factoryMethodParameters = AstUtils.clone(constructor.getParameters());
    // Type $instance = new Type(<original constructor arguments>);
    // TODO(b/200874018): The arguments are not used in the backend, and they will be rewritten and
    // reused to express the values for the immutable fields.
    Variable newInstance =
        Variable.newBuilder().setName("$instance").setTypeDescriptor(enclosingType).build();

    SourcePosition constructorSourcePosition = constructor.getSourcePosition();

    Statement newInstanceStatement =
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(
                newInstance,
                NewInstance.Builder.from(constructor.getDescriptor())
                    .setArguments(
                        factoryMethodParameters.stream()
                            .map(Variable::createReference)
                            .collect(Collectors.toList()))
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
    return MethodDescriptor.Builder.from(constructor)
        .setDeclarationDescriptor(
            constructor.isDeclaration()
                ? null
                : getCtorMethodDescriptorForConstructor(constructor.getDeclarationDescriptor()))
        .setReturnTypeDescriptor(PrimitiveTypes.VOID)
        .setName(MethodDescriptor.CTOR_METHOD_PREFIX)
        .setConstructor(false)
        .setStatic(false)
        .setOrigin(MethodOrigin.SYNTHETIC_CTOR_FOR_CONSTRUCTOR)
        // Set to private to avoid putting the method in the vtable. (These methods might
        // be called by a subclass though)
        .setVisibility(Visibility.PRIVATE)
        .setJsInfo(JsInfo.NONE)
        // Clear side effect free flag since the ctor method does not return a value and would be
        // pruned incorrectly.
        // The factory method still preserves the side effect free flags if it was present.
        .setSideEffectFree(false)
        .build();
  }

  /** Method descriptor for $create methods. */
  private static MethodDescriptor getFactoryDescriptorForConstructor(MethodDescriptor constructor) {
    checkArgument(constructor.isConstructor());
    return MethodDescriptor.Builder.from(constructor)
        .setDeclarationDescriptor(
            constructor.isDeclaration()
                ? null
                : getFactoryDescriptorForConstructor(constructor.getDeclarationDescriptor()))
        .setStatic(true)
        .setName(MethodDescriptor.CREATE_METHOD_NAME)
        .setConstructor(false)
        .setTypeParameterTypeDescriptors(
            Iterables.concat(
                constructor
                    .getEnclosingTypeDescriptor()
                    .getTypeDeclaration()
                    .getTypeParameterDescriptors(),
                constructor.getTypeParameterTypeDescriptors()))
        .setOrigin(MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR)
        .setJsInfo(JsInfo.NONE)
        .setVisibility(Visibility.PUBLIC)
        .build();
  }
}
