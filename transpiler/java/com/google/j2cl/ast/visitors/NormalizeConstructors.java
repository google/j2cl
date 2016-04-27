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
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.JsMemberType;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For each constructor we insert a $create factory method.
 * For each NewInstance node we convert it to a call to the $create factory method.
 */
public class NormalizeConstructors {
  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new RewriteNewInstance());
    compilationUnit.accept(new InsertFactoryMethods());
    RewriteConstructorsAsCtorMethods.applyTo(compilationUnit);
  }

  /**
   * This pass transforms Java constructors into methods with the $ctor prefix, then synthesizes a
   * single constructor per class that represents the actual Javascript ES6 constructor. There are
   * 3 stages:
   * <p>1) Gather primary constructors which is needed to generate correct Javascript constructors
   * for classes annotated with @JsConstructor.  Remove calls to super to from these constructors
   * since super will be called by the Javascript constructor instead.
   * <p>2) Rewrite Java constructors as simple methods with the $ctor prefix and update references
   * to constructor calls such as "super(...)" and "this(...)" to point to the synthesize methods.
   * <p>3) Synthesize Javascript constructors.
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
  private static class RewriteConstructorsAsCtorMethods {
    private static class PrimaryConstructorInfo {
      Method primaryConstructorReference;
      MethodCall constructorInvocation;

      PrimaryConstructorInfo(Method primaryConstructorReference, MethodCall constructorInvocation) {
        this.primaryConstructorReference = primaryConstructorReference;
        this.constructorInvocation = constructorInvocation;
      }
    }

    private Map<JavaType, PrimaryConstructorInfo> primaryConstructorByJavaType = new HashMap<>();

    public static void applyTo(CompilationUnit compilationUnit) {
      new RewriteConstructorsAsCtorMethods().run(compilationUnit);
    }

    public void run(CompilationUnit compilationUnit) {
      compilationUnit.accept(new GatherPrimaryConstructors());
      compilationUnit.accept(new RemoveSuperCallsFromConstructor());
      compilationUnit.accept(new RewriteCtorsAsMethods());
      compilationUnit.accept(new InsertJavascriptConstructors());
    }

    private static MethodDescriptor ctorMethodDescritorFromJavaConstructor(
        MethodDescriptor constructor) {
      checkArgument(constructor.isConstructor());
      String ctorName = ManglingNameUtils.getCtorMangledName(constructor);
      return MethodDescriptor.Builder.from(constructor)
          .methodName(ctorName)
          .isConstructor(false)
          .isStatic(false)
          .jsInfo(JsInfo.NONE)
          .visibility(Visibility.PUBLIC)
          .build();
    }

    private class GatherPrimaryConstructors extends AbstractVisitor {
      @Override
      public boolean enterJavaType(JavaType javaType) {
        if (javaType.getDescriptor().subclassesJsConstructorClass()) {
          Method primaryConstructor = checkNotNull(AstUtils.getPrimaryConstructor(javaType));
          MethodCall constructorInvocation = AstUtils.getConstructorInvocation(primaryConstructor);

          primaryConstructorByJavaType.put(
              javaType, new PrimaryConstructorInfo(primaryConstructor, constructorInvocation));
        }
        return true;
      }
    }

    private class RemoveSuperCallsFromConstructor extends AbstractVisitor {
      @Override
      public boolean enterMethod(Method constructor) {
        if (constructor.isConstructor()) {
          TypeDescriptor currentType = getCurrentJavaType().getDescriptor();
          if (currentType.subclassesJsConstructorClass()) {
            // Here we remove the "this" call since this is already taken care of by the
            // $create method.
            if (AstUtils.hasConstructorInvocation(constructor)) {
              final MethodCall constructorInvocation =
                  AstUtils.getConstructorInvocation(constructor);
              if (constructorInvocation.getTarget().getEnclosingClassTypeDescriptor()
                  == getCurrentJavaType().getDescriptor().getSuperTypeDescriptor()) {
                // super() call should be called with the es6 "super(args)" in the es6 constructor
                // if the super class is a @JsConstructor or subclass of @JsConstructor.
                // If the super class is just a normal Java class then we should rely on the
                // $ctor method to call the super constructor (which is $ctor_superclass).
                if (!currentType.getSuperTypeDescriptor().subclassesJsConstructorClass()) {
                  return false; // Don't remove the super call from $ctor below.
                }
              }
              // this() call should be replaced by a call to the es6 constructor in the $create
              // method so we remove these from $ctor methods.
              Statement statement = AstUtils.getConstructorInvocationStatement(constructor);
              constructor.getBody().getStatements().remove(statement);
            }
          }
        }
        return false;
      }
    }

    private class RewriteCtorsAsMethods extends AbstractRewriter {
      @Override
      public Node rewriteMethod(Method constructor) {
        if (constructor.isConstructor()) {

          return new Method(
              ctorMethodDescritorFromJavaConstructor(constructor.getDescriptor()),
              constructor.getParameters(),
              constructor.getBody(),
              false,
              false,
              false,
              "Initializes instance fields for a particular Java constructor.");
        }
        return constructor;
      }

      @Override
      public Node rewriteMethodCall(MethodCall methodCall) {
        if (methodCall.getTarget().isConstructor()) {
          return MethodCall.createRegularMethodCall(
              methodCall.getQualifier(),
              ctorMethodDescritorFromJavaConstructor(methodCall.getTarget()),
              methodCall.getArguments());
        }
        return methodCall;
      }
    }

    private class InsertJavascriptConstructors extends AbstractVisitor {
      private Method synthesizeJsConstructorRealConstructor(JavaType javaType) {
        String comment = "Real constructor.";
        List<Statement> body = AstUtils.generateFieldDeclarations(javaType);

        Method primaryConstructor =
            primaryConstructorByJavaType.get(javaType).primaryConstructorReference;
        MethodCall superCall = primaryConstructorByJavaType.get(javaType).constructorInvocation;

        MethodDescriptor.Builder builder =
            MethodDescriptor.Builder.fromDefault()
                .isConstructor(true)
                .jsInfo(JsInfo.create(JsMemberType.CONSTRUCTOR, "constructor", null, false))
                .visibility(Visibility.PUBLIC)
                .enclosingClassTypeDescriptor(javaType.getDescriptor());
        for (Variable typeParameter : primaryConstructor.getParameters()) {
          builder.addParameter(typeParameter.getTypeDescriptor());
        }
        MethodDescriptor constructorDescriptor = builder.build();
        List<Variable> constructorArguments = primaryConstructor.getParameters();

        // Must call the corresponding the $ctor method.
        MethodDescriptor ctorDescriptor =
            ctorMethodDescritorFromJavaConstructor(primaryConstructor.getDescriptor());
        List<Expression> arguments =
            Lists.transform(
                primaryConstructor.getParameters(),
                new Function<Variable, Expression>() {
                  @Override
                  public Expression apply(Variable var) {
                    return var.getReference();
                  }
                });
        MethodCall ctorCall = MethodCall.createRegularMethodCall(null, ctorDescriptor, arguments);
        body.add(new ExpressionStatement(ctorCall));

        List<Expression> superArguments = Collections.<Expression>emptyList();
        List<TypeDescriptor> superArgumentTypes = Collections.<TypeDescriptor>emptyList();
        // Note that the super call arguments empty if this @JsConstructor class is a sublcass of a
        // regular Java class.  Otherwise we get the arguments from the primary constructor.  Also
        // note that the super call may be null if the super constructor was native.
        if (superCall != null && javaType.getSuperTypeDescriptor().subclassesJsConstructorClass()) {
          superArguments = superCall.getArguments();
          superArgumentTypes = superCall.getTarget().getParameterTypeDescriptors();
        }
        MethodCall jsSuperCall =
            sythesizeEs6SuperCall(
                javaType.getSuperTypeDescriptor(), superArgumentTypes, superArguments);
        body.add(0, new ExpressionStatement(jsSuperCall));

        return new Method(
            constructorDescriptor,
            constructorArguments,
            new Block(body),
            false,
            false,
            false,
            comment);
      }

      private Method synthesizeRegularEs6Constructor(JavaType javaType) {
        List<Statement> body = AstUtils.generateFieldDeclarations(javaType);
        MethodCall emptySuperCall =
            sythesizeEs6SuperCall(
                javaType.getSuperTypeDescriptor(),
                Collections.<TypeDescriptor>emptyList(),
                Collections.<Expression>emptyList());
        body.add(0, new ExpressionStatement(emptySuperCall));

        MethodDescriptor constructorDescriptor =
            MethodDescriptor.Builder.fromDefault()
                .isConstructor(true)
                .enclosingClassTypeDescriptor(javaType.getDescriptor())
                .visibility(Visibility.PUBLIC)
                .build();

        return new Method(
            constructorDescriptor,
            Collections.<Variable>emptyList(),
            new Block(body),
            false,
            false,
            false,
            "Defines instance fields.");
      }

      /**
       * Synthesizes a super method call in es6 style.
       */
      private MethodCall sythesizeEs6SuperCall(
          TypeDescriptor superType,
          List<TypeDescriptor> superArgumentTypes,
          List<Expression> superArguments) {
        MethodDescriptor jsSuperDescriptor =
            MethodDescriptor.Builder.fromDefault()
                .parameterTypeDescriptors(superArgumentTypes)
                .enclosingClassTypeDescriptor(superType)
                .jsInfo(JsInfo.create(JsMemberType.CONSTRUCTOR, "super", null, false))
                .isConstructor(true)
                .build();
        return MethodCall.createRegularMethodCall(null, jsSuperDescriptor, superArguments);
      }

      @Override
      public boolean enterJavaType(JavaType javaType) {
        if (javaType.isJsOverlayImpl() || javaType.isInterface()) {
          return false;
        }
        if (javaType.getDescriptor().getSuperTypeDescriptor() == null) {
          return false;
        }

        if (javaType.getDescriptor().subclassesJsConstructorClass()) {
          javaType.getMethods().add(0, synthesizeJsConstructorRealConstructor(javaType));
        } else {
          javaType.getMethods().add(0, synthesizeRegularEs6Constructor(javaType));
        }
        return false;
      }
    }
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
        .typeParameterTypeDescriptors(allParameterTypes)
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
            .enclosingClassTypeDescriptor(enclosingType)
            .isConstructor(true)
            .returnTypeDescriptor(TypeDescriptors.get().primitiveVoid)
            .build();

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
        MethodDescriptor.Builder.fromDefault()
            .enclosingClassTypeDescriptor(enclosingType)
            .isConstructor(true)
            .returnTypeDescriptor(TypeDescriptors.get().primitiveVoid)
            .visibility(Visibility.PRIVATE)
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
