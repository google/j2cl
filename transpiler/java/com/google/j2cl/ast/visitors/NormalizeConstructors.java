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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.AstUtils;
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
 * Transforms all classes so that each has only (at most) one constructor to match the one
 * constructor restriction in JavaScript.
 * <p>
 * Creates $create factory methods corresponding to each constructor and transforms newInstances
 * into calls to these factory methods.
 */
public class NormalizeConstructors {

  /**
   * This pass transforms Java constructors into methods with the $ctor prefix, and synthesizes a
   * single constructor per class (which will end up being the actual Javascript ES6 constructor).
   * The process is done in three stages:
   * <p>1) Synthesize the primary (and only) constructor that will be emitted in the output.
   * <p>2) Remove calls to super to from these constructors (which will be transformed into ctor
   * methods) since super will be called by the primary constructor instead.
   * <p>3) Rewrite Java constructors as simple methods with the $ctor prefix and update references
   * to constructor calls such as "super(...)" and "this(...)" to point to the synthesize methods.
   * <p> Note that before this pass, constructors are Java constructors, whereas after this pass
   * constructors are actually Javascript constructors.
   *
   * This pass also performs @JsConstructors normalizations. Note that there are 3 forms
   * of constructors:
   * </p> 1) Normal Java classes where the Javascript constructor simply defines the class fields.
   *
   * </p> 2) @JsConstructor classes that subclass a regular constructor.  This class exposes a
   * 'real' Javascript constructor that can be used to make an instance of the class.  However, to
   * call super we cannot call the es6 super(args) since the super class is a regular Java class, it
   * is expected that the $ctor_super(args) is called.  Hence the constructors look like this:
   * <pre> {@code
   * class JsConstructorClass extends RegularClass
   *   constructor(args) {
   *     super();
   *     // field inits
   *     $ctor(args);
   *   }
   *
   *   $ctor(args) {
   *     $ctorSuper(args);
   *     ...
   *   }
   * }
   * </pre>
   *
   * <p> 3) All subclasses of @JsConstructor (somewhere in the hierarchy). All @JsConstructor
   * subclasses must use the real Javascript constructor to create an instance since calling super
   * is only possible from the Javascript constructor. Think about a direct subclass then realize
   * it must apply recursively to all subclasses.  Since super is called in the real Javascript
   * constructor, it must be removed from the $ctor method.
   * <pre> {@code
   * class JsConstructorClass extends JsConstructorClassOrSubclass
   *   constructor(args) {
   *     super(args);
   *     // field inits
   *     $ctor(args);
   *   }
   *
   *   $ctor(args) {
   *     // no $ctorSuper call!
   *     ...
   *   }
   * }
   * </pre>
   */
  public static void applyTo(CompilationUnit compilationUnit) {
    for (JavaType type : compilationUnit.getTypes()) {
      Method resultingConstructor =
          type.getDescriptor().isOrSubclassesJsConstructorClass()
              ? synthesizeJsConstructor(type)
              : maybeSynthesizePrivateConstructor(type);

      type.accept(new RewriteNewInstance());
      type.accept(new InsertFactoryMethods());
      type.accept(new RemoveSuperCallsFromConstructor());
      type.accept(new RewriteCtorsAsMethods());

      if (resultingConstructor != null) {
        type.getMethods().add(0, resultingConstructor);
      }
    }
  }

  private static MethodDescriptor ctorMethodDescriptorFromJavaConstructor(
      MethodDescriptor constructor) {
    checkArgument(constructor.isConstructor());
    return MethodDescriptor.Builder.from(constructor)
        .setMethodName(ManglingNameUtils.getCtorMangledName(constructor))
        .setIsConstructor(false)
        .setIsStatic(false)
        .setJsInfo(JsInfo.NONE)
        .setVisibility(Visibility.PUBLIC)
        .build();
  }

  private static class RemoveSuperCallsFromConstructor extends AbstractVisitor {
    @Override
    public boolean enterMethod(Method method) {
      if (!method.isConstructor()) {
        return false;
      }
      TypeDescriptor currentType = getCurrentJavaType().getDescriptor();
      if (!currentType.isOrSubclassesJsConstructorClass()
          || !AstUtils.hasConstructorInvocation(method)) {
        return false;
      }

      // Here we remove the "this" call since this is already taken care of by the
      // $create method.
      final MethodCall constructorInvocation = AstUtils.getConstructorInvocation(method);
      if (constructorInvocation.getTarget().getEnclosingClassTypeDescriptor()
          == getCurrentJavaType().getDescriptor().getSuperTypeDescriptor()) {
        // super() call should be called with the es6 "super(args)" in the es6 constructor
        // if the super class is a @JsConstructor or subclass of @JsConstructor.
        // If the super class is just a normal Java class then we should rely on the
        // $ctor method to call the super constructor (which is $ctor_superclass).
        if (!currentType.getSuperTypeDescriptor().isOrSubclassesJsConstructorClass()) {
          return false; // Don't remove the super call from $ctor below.
        }
      }
      // this() call should be replaced by a call to the es6 constructor in the $create
      // method so we remove these from $ctor methods.
      Statement statement = AstUtils.getConstructorInvocationStatement(method);
      method.getBody().getStatements().remove(statement);
      return false;
    }
  }

  private static class RewriteCtorsAsMethods extends AbstractRewriter {
    @Override
    public Node rewriteMethod(Method method) {
      if (!method.isConstructor()) {
        return method;
      }
      Method.Builder methodBuilder = Method.Builder.fromDefault()
          .setMethodDescriptor(
              ctorMethodDescriptorFromJavaConstructor(method.getDescriptor()))
          .setParameters(method.getParameters())
          .addStatements(method.getBody().getStatements())
          .setJsDocDescription(
              "Initializes instance fields for a particular Java constructor.");
      for (int i = 0; i < method.getParameters().size(); i++) {
        methodBuilder.setParameterOptional(i, method.isParameterOptional(i));
      }
      return methodBuilder.build();
    }

    @Override
    public Node rewriteMethodCall(MethodCall methodCall) {
      if (!methodCall.getTarget().isConstructor()) {
        return methodCall;
      }

      return MethodCall.createMethodCall(
          methodCall.getQualifier(),
          ctorMethodDescriptorFromJavaConstructor(methodCall.getTarget()),
          methodCall.getArguments());
    }
  }

  private static Method synthesizeJsConstructor(JavaType javaType) {
    Method primaryConstructor = checkNotNull(AstUtils.getPrimaryConstructor(javaType));
    MethodCall superConstructorInvocation = AstUtils.getConstructorInvocation(primaryConstructor);
    checkArgument(
        superConstructorInvocation == null
            || superConstructorInvocation.getTarget().getEnclosingClassTypeDescriptor()
                == javaType.getSuperTypeDescriptor());

    List<Statement> body = AstUtils.generateFieldDeclarations(javaType);

    // Must call the corresponding the $ctor method.
    MethodDescriptor ctorDescriptor =
        ctorMethodDescriptorFromJavaConstructor(primaryConstructor.getDescriptor());
    List<Expression> arguments =
        Lists.transform(
            primaryConstructor.getParameters(),
            new Function<Variable, Expression>() {
              @Override
              public Expression apply(Variable var) {
                return var.getReference();
              }
            });
    MethodCall ctorCall = MethodCall.createMethodCall(null, ctorDescriptor, arguments);
    body.add(new ExpressionStatement(ctorCall));

    // Note that the super call arguments are empty if this @JsConstructor class is a subclass of a
    // regular Java class.  Otherwise we get the arguments from the primary constructor.  Also
    // note that the super call may be null if the super constructor was native.
    // TODO: We should verify that these nodes are not being referenced multiple times in the AST.
    if (superConstructorInvocation == null
        || !javaType.getSuperTypeDescriptor().isOrSubclassesJsConstructorClass()) {
      superConstructorInvocation = synthesizeEmptySuperCall(javaType.getSuperTypeDescriptor());
    }
    body.add(0, new ExpressionStatement(superConstructorInvocation));

    MethodDescriptor.Builder builder =
        MethodDescriptor.Builder.from(primaryConstructor.getDescriptor())
            .setVisibility(Visibility.PUBLIC);
    for (Variable typeParameter : primaryConstructor.getParameters()) {
      builder.addParameter(typeParameter.getTypeDescriptor());
    }
    MethodDescriptor constructorDescriptor = builder.build();

    List<Variable> constructorParameters = primaryConstructor.getParameters();
    Method.Builder constructorBuilder =
        Method.Builder.fromDefault()
            .setMethodDescriptor(constructorDescriptor)
            .setParameters(constructorParameters)
            .addStatements(body)
            .setJsDocDescription("Real constructor.");
    for (int i = 0; i < constructorParameters.size(); i++) {
      constructorBuilder.setParameterOptional(i, primaryConstructor.isParameterOptional(i));
    }
    return constructorBuilder.build();
  }


  private static Method maybeSynthesizePrivateConstructor(JavaType javaType) {
    if (javaType.isJsOverlayImpl() || javaType.isInterface()) {
      return null;
    }

    List<Statement> body = AstUtils.generateFieldDeclarations(javaType);

    if (javaType.getDescriptor().getSuperTypeDescriptor() != null) {
      body.add(
          0, new ExpressionStatement(synthesizeEmptySuperCall(javaType.getSuperTypeDescriptor())));
    }

    MethodDescriptor constructorDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setIsConstructor(true)
            .setEnclosingClassTypeDescriptor(javaType.getDescriptor())
            .setVisibility(Visibility.PUBLIC)
            .build();

    return Method.Builder.fromDefault()
        .setMethodDescriptor(constructorDescriptor)
        .addStatements(body)
        .setJsDocDescription("Defines instance fields.")
        .build();
  }

  /**
   * Synthesizes a method descriptor for a "super" call to the constructor.
   */
  private static MethodCall synthesizeEmptySuperCall(TypeDescriptor superType) {
    MethodDescriptor superDescriptor =
        MethodDescriptor.Builder.fromDefault()
            .setEnclosingClassTypeDescriptor(superType)
            .setIsConstructor(true)
            .build();
    return MethodCall.createMethodCall(null, superDescriptor);
  }

  /**
   * Rewrite NewInstance nodes to MethodCall nodes to the $create factory method.
   */
  private static class RewriteNewInstance extends AbstractRewriter {
    @Override
    public Node rewriteNewInstance(NewInstance constructorInvocation) {
      MethodDescriptor originalConstructor = constructorInvocation.getTarget();
      if (originalConstructor.isJsConstructor()) {
        return constructorInvocation;
      }

      MethodDescriptor staticFactoryMethod = factoryDescriptorForConstructor(originalConstructor);
      return MethodCall.createMethodCall(
          null, staticFactoryMethod, constructorInvocation.getArguments());
    }
  }

  /**
   * Inserts $create methods for each constructor.
   */
  private static class InsertFactoryMethods extends AbstractVisitor {
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
    if (!method.isConstructor() || method.getDescriptor().isJsConstructor()) {
      return false;
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
        .setIsStatic(true)
        .setMethodName(MethodDescriptor.CREATE_METHOD_NAME)
        .setVisibility(Visibility.PUBLIC)
        .setIsConstructor(false)
        .setReturnTypeDescriptor(
            TypeDescriptors.toNonNullable(constructor.getEnclosingClassTypeDescriptor()))
        .setTypeParameterTypeDescriptors(allParameterTypes)
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
        MethodDescriptor.Builder.fromDefault()
            .setEnclosingClassTypeDescriptor(enclosingType)
            .setIsConstructor(true)
            .setReturnTypeDescriptor(TypeDescriptors.get().primitiveVoid)
            .build();

    List<Expression> arguments = Lists.newArrayList();
    if (enclosingType.isOrSubclassesJsConstructorClass()) {
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
              .setParameterTypeDescriptors(
                  constructorInvocation
                      .getTarget()
                      .getDeclarationMethodDescriptor()
                      .getParameterTypeDescriptors())
              .build();
      javascriptConstructor =
          MethodDescriptor.Builder.from(javascriptConstructor)
              .setDeclarationMethodDescriptor(javascriptConstructorDeclaration)
              .setParameterTypeDescriptors(
                  constructorInvocation.getTarget().getParameterTypeDescriptors())
              .setIsVarargs(constructorInvocation.getTarget().isVarargs())
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
        MethodCall.createMethodCall(
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

    return Method.Builder.fromDefault()
        .setMethodDescriptor(factoryDescriptorForConstructor(constructor.getDescriptor()))
        .setParameters(constructor.getParameters())
        .addStatements(newInstanceStatement, ctorCallStatement, returnStatement)
        .setIsFinal(true)
        .setJsDocDescription("A particular Java constructor as a factory method.")
        .build();
  }

  /**
   * We can assume here that method is the primary constructor.
   */
  private static Method originalContructorBodyMethod(Method primaryConstructor) {
    TypeDescriptor enclosingType =
        primaryConstructor.getDescriptor().getEnclosingClassTypeDescriptor();
    
    MethodDescriptor javascriptConstructor =
        MethodDescriptor.Builder.fromDefault()
            .setEnclosingClassTypeDescriptor(enclosingType)
            .setIsConstructor(true)
            .setReturnTypeDescriptor(TypeDescriptors.get().primitiveVoid)
            .setVisibility(Visibility.PRIVATE)
            .setIsVarargs(primaryConstructor.getDescriptor().isVarargs())
            .build();

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
            .setParameterTypeDescriptors(
                primaryConstructor
                    .getDescriptor()
                    .getDeclarationMethodDescriptor()
                    .getParameterTypeDescriptors())
            .build();

    javascriptConstructor =
        MethodDescriptor.Builder.from(javascriptConstructor)
            .setDeclarationMethodDescriptor(javascriptConstructorDeclaration)
            .setParameterTypeDescriptors(
                primaryConstructor.getDescriptor().getParameterTypeDescriptors())
            .build();

    // return $instance
    Statement returnStatement =
        new ReturnStatement(
            new NewInstance(null, javascriptConstructor, relayArguments),
            primaryConstructor.getDescriptor().getEnclosingClassTypeDescriptor());

    return Method.Builder.fromDefault()
        .setMethodDescriptor(factoryDescriptorForConstructor(primaryConstructor.getDescriptor()))
        .setParameters(primaryConstructor.getParameters())
        .addStatements(returnStatement)
        .setIsFinal(true)
        .setJsDocDescription("A particular Java constructor as a factory method.")
        .build();
  }
}
