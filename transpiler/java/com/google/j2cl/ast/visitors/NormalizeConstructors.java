/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.ast.visitors;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.Visibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * For each constructor we insert a $create factory method.
 * For each NewInstance node we convert it to a call to the $create factory method.
 */
public class NormalizeConstructors {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new RewriteNewInstance());
    compilationUnit.accept(new InsertFactoryMethods());
  }

  /**
   * Rewrite NewInstance nodes to MethodCall nodes to the $create factory method.
   */
  static class RewriteNewInstance extends AbstractRewriter {
    @Override
    public Node rewriteNewInstance(NewInstance constructorInvocation) {
      MethodDescriptor originalConstructor = constructorInvocation.getTarget();
      if (originalConstructor.isJsConstructor()) {
        return constructorInvocation;
      }

      MethodDescriptor staticFactoryMethod = factoryDescriptorForConstructor(originalConstructor);
      return MethodCall.createRegularMethodCall(
          null, staticFactoryMethod, constructorInvocation.getArguments());
    }
  }

  /**
   * Inserts $create methods for each constructor.
   */
  static class InsertFactoryMethods extends AbstractVisitor {
    @Override
    public boolean enterJavaType(JavaType javaType) {
      List<Method> allMethods = new ArrayList<>();
      for (Method method : javaType.getMethods()) {
        if (shouldOutputStaticFactoryCreateMethod(javaType, method)) {
          Method create = factoryMethodForConstructor(method, javaType);
          allMethods.add(create);
        }
        allMethods.add(method);
      }
      javaType.getMethods().clear();
      javaType.addMethods(allMethods);
      return false;
    }
  }

  static boolean shouldOutputStaticFactoryCreateMethod(JavaType javaType, Method method) {
    if (!method.isConstructor()) {
      return false;
    }
    if (method.getDescriptor().isJsConstructor()) {
      // Keep the $create method for inner types
      if (AstUtils.getEnclosingInstanceField(javaType) != null) {
        return false;
      }
    }
    String mangledNameOfCreate =
        ManglingNameUtils.getFactoryMethodMangledName(method.getDescriptor());
    if (javaType.containsJsMethod(mangledNameOfCreate)) {
      return false;
    }
    return true;
  }

  private static MethodDescriptor factoryDescriptorForConstructor(MethodDescriptor constructor) {
    Preconditions.checkArgument(constructor.isConstructor());
    List<TypeDescriptor> allParameterTypes = new ArrayList<>();
    allParameterTypes.addAll(
        constructor.getEnclosingClassTypeDescriptor().getTypeArgumentDescriptors());
    allParameterTypes.addAll(constructor.getTypeParameterTypeDescriptors());
    return MethodDescriptor.Builder.from(constructor)
        .isStatic(true)
        .methodName(MethodDescriptor.CREATE_METHOD_NAME)
        .visibility(Visibility.PUBLIC)
        .isConstructor(false)
        .returnTypeDescriptor(
            TypeDescriptors.toNonNullable(constructor.getEnclosingClassTypeDescriptor()))
        .typeParameterDescriptors(allParameterTypes)
        .build();
  }

  /**
   * Generates code of the form:
   *
   * <pre> {@code
   * static $create(args)
   *   let $instance = new Type();
   *   $instance.$ctor...(args);
   *   return $instance;
   * }
   * </pre>
   */
  private static Method factoryMethodForConstructor(Method constructor, JavaType javaType) {
    TypeDescriptor enclosingType = javaType.getDescriptor();
    MethodDescriptor javascriptConstructor =
        MethodDescriptor.create(
            false,
            Visibility.PUBLIC,
            enclosingType,
            "",
            true,
            false,
            false,
            null,
            TypeDescriptors.get().primitiveVoid,
            new ArrayList<TypeDescriptor>(),
            new ArrayList<TypeDescriptor>(),
            JsInfo.NONE);

    List<Expression> arguments = Lists.newArrayList();
    if (enclosingType.subclassesJsConstructorClass()) {
      // No need for a factory method if we are calling a @JsConstructor
      if (constructor == AstUtils.getPrimaryConstructor(javaType)) {
        return originalContructorBodyMethod(constructor);
      }
      MethodCall constructorInvocation = AstUtils.getConstructorInvocation(constructor);
      Preconditions.checkNotNull(
          constructorInvocation, "this() call was null!" + constructor.toString());

      arguments = constructorInvocation.getArguments();
      MethodDescriptor javascriptConstructorDeclaration =
          MethodDescriptor.Builder.from(javascriptConstructor)
             .parameterTypeDescriptors(
                 constructorInvocation.getTarget().getDeclarationMethodDescriptor()
                     .getParameterTypeDescriptors())
             .build();
      javascriptConstructor =
          MethodDescriptor.Builder.from(javascriptConstructor)
              .declarationMethodDescriptor(javascriptConstructorDeclaration)
              .parameterTypeDescriptors(
                  constructorInvocation.getTarget().getParameterTypeDescriptors())
              .build();
    }

    // let $instance = new Class;
    Variable newInstance = new Variable("$instance", enclosingType, false, false);
    VariableDeclarationFragment frag =
        new VariableDeclarationFragment(
            newInstance, new NewInstance(null, javascriptConstructor, arguments));
    VariableDeclarationExpression expression =
        new VariableDeclarationExpression(Arrays.asList(frag));
    Statement newInstanceStatement = new ExpressionStatement(expression);

    // $instance.$ctor...();
    List<Expression> relayArguments =
        Lists.transform(
            constructor.getParameters(),
            new Function<Variable, Expression>() {
              @Override
              public Expression apply(Variable input) {
                return input.getReference();
              }
            });
    MethodCall ctorCall =
        MethodCall.createRegularMethodCall(
            newInstance.getReference(), constructor.getDescriptor(), relayArguments);
    Statement ctorCallStatement = new ExpressionStatement(ctorCall);

    Expression newInstanceReference = newInstance.getReference();
    if (enclosingType.isJsFunctionImplementation()) {
      newInstanceReference =
          AstUtils.createLambdaInstance(enclosingType, newInstance.getReference());
    }

    // return $instance
    Statement returnStatement =
        new ReturnStatement(
            newInstanceReference, constructor.getDescriptor().getEnclosingClassTypeDescriptor());

    String factoryMethodDescription = "A particular Java constructor as a factory method.";
    return new Method(
        factoryDescriptorForConstructor(constructor.getDescriptor()),
        constructor.getParameters(),
        new Block(newInstanceStatement, ctorCallStatement, returnStatement),
        false,
        false,
        true,
        factoryMethodDescription);
  }

  /**
   * We can assume here that method is the primary constructor.
   */
  private static Method originalContructorBodyMethod(Method primaryConstructor) {
    TypeDescriptor enclosingType =
        primaryConstructor.getDescriptor().getEnclosingClassTypeDescriptor();
    MethodDescriptor javascriptConstructor =
        MethodDescriptor.create(
            false,
            Visibility.PRIVATE,
            enclosingType,
            "",
            true,
            false,
            false,
            null,
            TypeDescriptors.get().primitiveVoid,
            new ArrayList<TypeDescriptor>(),
            new ArrayList<TypeDescriptor>(),
            JsInfo.NONE);
    // $instance.$ctor...();
    List<Expression> relayArguments =
        Lists.transform(
            primaryConstructor.getParameters(),
            new Function<Variable, Expression>() {
              @Override
              public Expression apply(Variable input) {
                return input.getReference();
              }
            });

    MethodDescriptor javascriptConstructorDeclaration =
        MethodDescriptor.Builder.from(javascriptConstructor)
        .parameterTypeDescriptors(
            primaryConstructor.getDescriptor().getDeclarationMethodDescriptor()
                .getParameterTypeDescriptors())
        .build();

    javascriptConstructor =
        MethodDescriptor.Builder.from(javascriptConstructor)
            .declarationMethodDescriptor(javascriptConstructorDeclaration)
            .parameterTypeDescriptors(
                primaryConstructor.getDescriptor().getParameterTypeDescriptors())
            .build();

    // return $instance
    Statement returnStatement =
        new ReturnStatement(
            new NewInstance(null, javascriptConstructor, relayArguments),
            primaryConstructor.getDescriptor().getEnclosingClassTypeDescriptor());

    String factoryMethodDescription = "A particular Java constructor as a factory method.";
    return new Method(
        factoryDescriptorForConstructor(primaryConstructor.getDescriptor()),
        primaryConstructor.getParameters(),
        new Block(returnStatement),
        false,
        false,
        true,
        factoryMethodDescription);
  }
}
