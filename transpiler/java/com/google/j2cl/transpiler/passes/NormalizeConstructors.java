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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.JsInfo;
import com.google.j2cl.transpiler.ast.JsMemberType;
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
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.ThisReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.Visibility;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Transforms classes so that each has only (at most) one constructor to match the one constructor
 * restriction imposed by JavaScript.
 *
 * <p>Creates $create factory methods corresponding to each constructor and transforms newInstances
 * into calls to these factory methods.
 */
public class NormalizeConstructors extends NormalizationPass {
  /**
   * This pass transforms Java constructors into methods with the $ctor prefix, and synthesizes a
   * single constructor per class (which will end up being the actual Javascript ES6 constructor).
   * The process is done in three stages:
   *
   * <ul>
   *   <li>1) Synthesize the primary (and only) constructor that will be emitted in the output.
   *   <li>2) Remove calls to super to from these constructors (which will be transformed into ctor
   *       methods) since super will be called by the primary constructor instead.
   *   <li>3) Rewrite Java constructors as simple methods with the $ctor prefix and update
   *       references to constructor calls such as "super(...)" and "this(...)" to point to the
   *       synthesize methods.
   * </ul>
   *
   * <p>Note that before this pass, constructors are Java constructors, whereas after this pass
   * constructors are actually Javascript constructors.
   *
   * <p>This pass also performs @JsConstructors normalizations. Note that there are 3 forms of
   * constructors:
   *
   * <ul>
   *   <li>1) Normal Java classes where the Javascript constructor simply defines the class fields.
   *   <li>2) @JsConstructor classes that subclass a regular constructor. These classes expose
   *       'real' Javascript constructors that can be used to make an instance of the class.
   *       However, to call super we cannot call the es6 super(args) since the super class is a
   *       regular Java class, it is expected that the $ctor_super(args) is called. Hence the
   *       constructors look like this:
   *       <pre>{@code
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
   * }</pre>
   *   <li>3) All subclasses of @JsConstructor (somewhere in the hierarchy). All @JsConstructor
   *       subclasses must use the real Javascript constructor to create an instance since calling
   *       super is only possible from the Javascript constructor. Think about a direct subclass
   *       then realize it must apply recursively to all subclasses. Since super is called in the
   *       real Javascript constructor, it must be removed from the $ctor method.
   *       <pre>{@code
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
   * }</pre>
   * </ul>
   */
  @Override
  public void applyTo(Type type) {
    rewriteNewInstances(type);
    rewriteConstructors(type);
  }

  private static void rewriteConstructors(Type type) {
    if (type.getConstructors().isEmpty()) {
      // No constructors => no normalization.
      return;
    }
    // Synthesize the es6 constructor BEFORE altering any of the constructor's code, but do not
    // add it to the type yet, the code for the es6 constructor should not be transformed.
    Method es6Constructor =
        type.getDeclaration().hasJsConstructor()
            ? synthesizeJsConstructor(type)
            : synthesizePrivateConstructor(type);

    insertFactoryMethods(type);
    // Since JsConstructors include ctor logic as part of ES6 constructor and will be implicitly
    // executed when the ES6 constructor is executed, we should remove explicit calls to the ctor
    // functions via 'super()' and 'this()'.
    removeUnneededCtorCalls(type);
    rewriteConstructorsAsCtorMethods(type);

    type.addMember(0, es6Constructor);
  }

  private static void removeUnneededCtorCalls(Type type) {
    for (Method ctor : type.getMethods()) {
      ExpressionStatement ctorCall = AstUtils.getConstructorInvocationStatement(ctor);
      if (ctorCall == null) {
        continue;
      }
      if (TypeDescriptors.isBoxedTypeAsJsPrimitives(type.getTypeDescriptor())
          || ((MethodCall) ctorCall.getExpression()).getTarget().isJsConstructor()) {
        // this() call should be replaced by a call to the es6 constructor in the $create
        // method so we remove these from $ctor methods.
        ctor.getBody().getStatements().remove(ctorCall);
      }
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
                .setMethodDescriptor(
                    ctorMethodDescriptorFromJavaConstructor(method.getDescriptor()))
                .setParameters(method.getParameters())
                .addStatements(method.getBody().getStatements())
                .setSourcePosition(method.getSourcePosition())
                .setJsDocDescription(
                    getConstructorRelatedJsDocDescription(
                        "Initialization from constructor", method, type))
                .build();
          }

          @Override
          public Expression rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.getTarget().isConstructor()) {
              return methodCall;
            }

            Expression qualifier =
                methodCall.getQualifier() != null
                    ? methodCall.getQualifier()
                    : new ThisReference(methodCall.getTarget().getEnclosingTypeDescriptor());
            return MethodCall.Builder.from(
                    ctorMethodDescriptorFromJavaConstructor(methodCall.getTarget()))
                .setQualifier(qualifier)
                .setArguments(methodCall.getArguments())
                .build();
          }
        });
  }

  /**
   * Returns a JsDoc description for the constructor related method.
   *
   * <p>The description makes it easier to understand which of the Java constructors corresponds to
   * the JavaScript method.
   */
  @Nullable
  private static String getConstructorRelatedJsDocDescription(
      String message, Method constructor, Type type) {
    // The constructors in Type might have rewritten before this method is invoked so we are getting
    // the original number of constructors from the declaration.
    boolean hasMultipleConstructors =
        type.getDeclaration().getDeclaredMethodDescriptors().stream()
                .filter(MethodDescriptor::isConstructor)
                .count()
            > 1;
    return hasMultipleConstructors
        ? String.format(message + " '%s'.", constructor.getDescriptor().getReadableDescription())
        : null;
  }

  private static Method synthesizeJsConstructor(Type type) {
    Method jsConstructor = checkNotNull(getJsConstructor(type));
    MethodCall superConstructorInvocation = AstUtils.getConstructorInvocation(jsConstructor);
    checkArgument(superConstructorInvocation.getTarget().isMemberOf(type.getSuperTypeDescriptor()));

    List<Variable> jsConstructorParameters = AstUtils.clone(jsConstructor.getParameters());
    List<Expression> arguments = AstUtils.getReferences(jsConstructorParameters);

    SourcePosition jsConstructorSourcePosition = jsConstructor.getSourcePosition();
    List<Statement> body =
        generateInstanceFieldDeclarationStatements(type, jsConstructorSourcePosition);
    // Must call the corresponding the $ctor method.
    MethodDescriptor ctorMethodDescriptor =
        ctorMethodDescriptorFromJavaConstructor(jsConstructor.getDescriptor());

    MethodCall ctorCall =
        MethodCall.Builder.from(ctorMethodDescriptor)
            .setDefaultInstanceQualifier()
            .setArguments(arguments)
            .build();
    body.add(ctorCall.makeStatement(jsConstructorSourcePosition));

    // Note that the super call arguments are empty if this @JsConstructor class is a subclass of a
    // regular Java class.  Otherwise we get the arguments from the primary constructor.  Also
    // note that the super call may be null if the super constructor was native.
    if (!type.getSuperTypeDescriptor().hasJsConstructor()) {
      superConstructorInvocation = synthesizeEmptySuperCall(type.getSuperTypeDescriptor());
    }
    body.add(
        0,
        AstUtils.replaceDeclarations(
            jsConstructor.getParameters(),
            jsConstructorParameters,
            superConstructorInvocation.makeStatement(jsConstructorSourcePosition)));

    if (type.getSuperTypeDescriptor().hasJsConstructor()) {
      // Move any statements that occurred before the super() call into the synthesized constructor.
      // These are generally expressions that were originally in the super() call that may have been
      // lowered into preceding statements. This is generally allowable so long as they never
      // reference the instance before the super() call occurs.
      int superConstructorStatementIndex =
          jsConstructor
              .getBody()
              .getStatements()
              .indexOf(AstUtils.getConstructorInvocationStatement(jsConstructor));
      var preSuperCallStatements =
          jsConstructor.getBody().getStatements().subList(0, superConstructorStatementIndex);
      body.addAll(
          0,
          AstUtils.replaceDeclarations(
              jsConstructor.getParameters(), jsConstructorParameters, preSuperCallStatements));
      preSuperCallStatements.clear();
    }

    if (type.getTypeDescriptor().isAssignableTo(TypeDescriptors.get().javaLangThrowable)) {
      // $instance.privateInitError(new Error);
      body.add(
          createThrowableInit(
                  new ThisReference(type.getTypeDescriptor()), TypeDescriptors.get().nativeError)
              .makeStatement(jsConstructorSourcePosition));
    }

    return Method.newBuilder()
        .setMethodDescriptor(jsConstructor.getDescriptor())
        .setParameters(jsConstructorParameters)
        .addStatements(body)
        .setJsDocDescription(
            getConstructorRelatedJsDocDescription("JsConstructor", jsConstructor, type))
        .setSourcePosition(jsConstructorSourcePosition)
        .build();
  }

  private static Method synthesizePrivateConstructor(Type type) {
    checkState(!type.isInterface());

    SourcePosition sourcePosition = type.getSourcePosition();

    List<Statement> body = generateInstanceFieldDeclarationStatements(type, sourcePosition);

    if (type.getSuperTypeDescriptor() != null) {
      body.add(
          0, synthesizeEmptySuperCall(type.getSuperTypeDescriptor()).makeStatement(sourcePosition));
    } else {
      body.add(0, synthesizeAssertClinit(type).makeStatement(sourcePosition));
    }

    MethodDescriptor constructorDescriptor =
        getImplicitJavascriptConstructorDescriptor(type.getTypeDescriptor());

    return Method.newBuilder()
        .setMethodDescriptor(constructorDescriptor)
        .addStatements(body)
        .setSourcePosition(sourcePosition)
        .build();
  }

  /** Synthesizes a "super" call to the constructor. */
  private static MethodCall synthesizeEmptySuperCall(DeclaredTypeDescriptor superType) {
    return MethodCall.Builder.from(getImplicitJavascriptConstructorDescriptor(superType)).build();
  }

  private static MethodCall synthesizeAssertClinit(Type type) {
    return RuntimeMethods.createUtilMethodCall(
        "$assertClinit", new ThisReference(type.getTypeDescriptor()));
  }

  /** Rewrite NewInstance nodes to MethodCall nodes to the $create factory method. */
  private static void rewriteNewInstances(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteNewInstance(NewInstance constructorInvocation) {
            MethodDescriptor originalConstructor = constructorInvocation.getTarget();
            if (originalConstructor.isJsConstructor()) {
              return constructorInvocation;
            }

            return MethodCall.Builder.from(factoryDescriptorForConstructor(originalConstructor))
                .setArguments(AstUtils.clone(constructorInvocation.getArguments()))
                .build();
          }
        });
  }

  /** Inserts $create methods for each constructor. */
  private static void insertFactoryMethods(Type type) {
    if (type.isAbstract() || type.isNative()) {
      return;
    }
    List<Member> members = type.getMembers();
    for (int i = 0; i < members.size(); i++) {
      if (!members.get(i).isMethod()) {
        continue;
      }
      Method method = (Method) members.get(i);
      MethodDescriptor methodDescriptor = method.getDescriptor();

      if (!methodDescriptor.isConstructor() || methodDescriptor.isJsConstructor()) {
        continue;
      }

      if (TypeDescriptors.isBoxedTypeAsJsPrimitives(type.getTypeDescriptor())) {
        // Replace constructor with the factory method and advance.
        members.set(i, synthesizeBoxedJsPrimitiveFactoryMethod(type, method));
      } else {
        // Insert the factory method just before the corresponding constructor, and advance.
        members.add(i++, synthesizeFactoryMethod(type, method));
      }
    }
  }

  /**
   * Generates code of the form:
   *
   * <pre>{@code
   * static $create(args)
   *   let $this;
   *   <devirt instructions with assignment to this>
   *   return $this;
   * }</pre>
   */
  private static Method synthesizeBoxedJsPrimitiveFactoryMethod(Type type, Method constructor) {
    Method factory =
        Method.newBuilder()
            .setMethodDescriptor(factoryDescriptorForConstructor(constructor.getDescriptor()))
            .setParameters(constructor.getParameters())
            .setSourcePosition(constructor.getSourcePosition())
            .build();

    List<Statement> factoryStatements = factory.getBody().getStatements();
    Variable thisArg =
        Variable.newBuilder()
            .setName("$thisArg")
            .setTypeDescriptor(type.getTypeDescriptor())
            .build();
    factoryStatements.add(
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclarations(thisArg)
            .build()
            .makeStatement(constructor.getSourcePosition()));
    factoryStatements.addAll(constructor.getBody().getStatements());
    factoryStatements.add(
        ReturnStatement.newBuilder()
            .setExpression(thisArg.createReference())
            .setSourcePosition(constructor.getSourcePosition())
            .build());
    factory.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteThisOrSuperReference(ThisOrSuperReference receiverReference) {
            return thisArg.createReference();
          }
        });

    return factory;
  }

  private static Method synthesizeFactoryMethod(Type type, Method constructor) {
    MethodDescriptor javascriptConstructor =
        getImplicitJavascriptConstructorDescriptor(type.getTypeDescriptor());
    List<Expression> javascriptConstructorArguments = ImmutableList.of();
    List<Statement> preConstructorCallStatements = ImmutableList.of();

    if (type.getDeclaration().hasJsConstructor()) {
      // Use JsConstructor instead.

      // Verify that we are not emitting factory methods for JsConstructors.
      checkState(constructor != getJsConstructor(type));

      MethodCall primaryConstructorInvocation = AstUtils.getConstructorInvocation(constructor);
      javascriptConstructor =
          getPrimaryConstructorDescriptor(primaryConstructorInvocation.getTarget());
      javascriptConstructorArguments = AstUtils.clone(primaryConstructorInvocation.getArguments());

      // Move any statements that occurred before the this() call into the factory method. These are
      // generally expressions that were originally in the this() call that may have been lowered
      // into preceding statements. This is generally allowable so long as they never reference the
      // instance before the this() call occurs.
      var constructorStatements = constructor.getBody().getStatements();
      var originalPreConstructorCallStatements =
          constructorStatements.subList(
              0,
              constructorStatements.indexOf(
                  AstUtils.getConstructorInvocationStatement(constructor)));
      preConstructorCallStatements = ImmutableList.copyOf(originalPreConstructorCallStatements);
      originalPreConstructorCallStatements.clear();
    }

    return synthesizeFactoryMethod(
        constructor,
        getConstructorRelatedJsDocDescription(
            "Factory method corresponding to constructor", constructor, type),
        type.getTypeDescriptor(),
        javascriptConstructor,
        javascriptConstructorArguments,
        preConstructorCallStatements);
  }

  /**
   * Generates code of the form:
   *
   * <pre>{@code
   * static $create(args)
   *   let $instance = new Type(<javascriptConstructorArguments>);
   *   $instance.$ctor...(args);
   *   return $instance;
   * }</pre>
   */
  private static Method synthesizeFactoryMethod(
      Method constructor,
      String jsDocDescription,
      DeclaredTypeDescriptor enclosingType,
      MethodDescriptor javascriptConstructor,
      List<Expression> javascriptConstructorArguments,
      List<Statement> preCtorCallStatements) {

    List<Statement> statements = new ArrayList<>();

    List<Variable> factoryMethodParameters = AstUtils.clone(constructor.getParameters());
    List<Expression> relayArguments = AstUtils.getReferences(factoryMethodParameters);
    statements.addAll(
        AstUtils.replaceDeclarations(
            constructor.getParameters(), factoryMethodParameters, preCtorCallStatements));
    javascriptConstructorArguments =
        AstUtils.replaceDeclarations(
            constructor.getParameters(), factoryMethodParameters, javascriptConstructorArguments);
    // let $instance = new Class(<javascriptConstructorArguments>);
    Variable newInstance =
        Variable.newBuilder().setName("$instance").setTypeDescriptor(enclosingType).build();

    SourcePosition constructorSourcePosition = constructor.getSourcePosition();
    Statement newInstanceStatement =
        AstUtils.replaceDeclarations(
                constructor.getParameters(),
                factoryMethodParameters,
                VariableDeclarationExpression.newBuilder()
                    .addVariableDeclaration(
                        newInstance,
                        NewInstance.Builder.from(javascriptConstructor)
                            .setArguments(javascriptConstructorArguments)
                            .build())
                    .build())
            .makeStatement(constructorSourcePosition);
    statements.add(newInstanceStatement);

    // $instance.$ctor...();
    Statement ctorCallStatement =
        MethodCall.Builder.from(constructor.getDescriptor())
            .setQualifier(newInstance.createReference())
            .setArguments(relayArguments)
            .build()
            .makeStatement(constructorSourcePosition);
    statements.add(ctorCallStatement);

    if (enclosingType.isAssignableTo(TypeDescriptors.get().javaLangThrowable)) {
      // $instance.privateInitError(new Error);
      statements.add(
          createThrowableInit(
                  newInstance.createReference(),
                  enclosingType.isAssignableTo(TypeDescriptors.get().javaLangNullPointerException)
                      ? TypeDescriptors.get().nativeTypeError
                      : TypeDescriptors.get().nativeError)
              .makeStatement(constructorSourcePosition));
    }

    // return $instance
    Statement returnStatement =
        ReturnStatement.newBuilder()
            .setExpression(
                enclosingType.isJsFunctionImplementation()
                    ? AstUtils.createLambdaInstance(enclosingType, newInstance.createReference())
                    : newInstance.createReference())
            .setSourcePosition(constructorSourcePosition)
            .build();
    statements.add(returnStatement);

    return Method.newBuilder()
        .setMethodDescriptor(factoryDescriptorForConstructor(constructor.getDescriptor()))
        .setParameters(factoryMethodParameters)
        .addStatements(statements)
        .setJsDocDescription(jsDocDescription)
        .setSourcePosition(constructorSourcePosition)
        .build();
  }

  private static Expression createThrowableInit(
      Expression newInstanceRef, DeclaredTypeDescriptor errorType) {
    return RuntimeMethods.createThrowableInitMethodCall(
        newInstanceRef, newInstanceOfError(errorType, newInstanceRef.clone()));
  }

  private static Expression newInstanceOfError(DeclaredTypeDescriptor type, Expression thisRef) {
    return NewInstance.Builder.from(
            MethodDescriptor.newBuilder()
                .setConstructor(true)
                .setParameterTypeDescriptors(TypeDescriptors.get().javaLangObject)
                .setEnclosingTypeDescriptor(type)
                .build())
        .setArguments(thisRef)
        .build();
  }

  private static Method getJsConstructor(Type type) {
    return type.getConstructors().stream()
        .filter(ctor -> ctor.getDescriptor().isJsConstructor())
        .collect(onlyElement());
  }

  /** Method descriptor for $ctor methods. */
  private static MethodDescriptor ctorMethodDescriptorFromJavaConstructor(
      MethodDescriptor constructor) {
    checkArgument(constructor.isConstructor());
    return constructor.transform(
        builder ->
            builder
                .setReturnTypeDescriptor(PrimitiveTypes.VOID)
                .setName(getCtorName(constructor))
                .setConstructor(false)
                .setStatic(false)
                .setOriginalJsInfo(JsInfo.NONE)
                .removeParameterOptionality()
                .setOrigin(MethodOrigin.SYNTHETIC_CTOR_FOR_CONSTRUCTOR)
                .setVisibility(Visibility.PUBLIC));
  }

  /** Returns the name of $ctor method for a particular constructor. */
  private static String getCtorName(MethodDescriptor methodDescriptor) {
    // Synthesize a name that is unique per class to avoid property clashes in JS.
    return MethodDescriptor.CTOR_METHOD_PREFIX
        + "__"
        + methodDescriptor.getEnclosingTypeDescriptor().getMangledName();
  }

  /** Method descriptor for $create methods. */
  private static MethodDescriptor factoryDescriptorForConstructor(MethodDescriptor constructor) {
    checkArgument(constructor.isConstructor());
    return constructor.transform(
        builder ->
            builder
                .setStatic(true)
                .setName(MethodDescriptor.CREATE_METHOD_NAME)
                .setConstructor(false)
                .setReturnTypeDescriptor(
                    // JsFunction implementation constructors actually return a function, not an
                    // object of the class.
                    builder.getEnclosingTypeDescriptor().isJsFunctionImplementation()
                        ? builder
                            .getEnclosingTypeDescriptor()
                            .getFunctionalInterface()
                            .toNonNullable()
                        : builder.getEnclosingTypeDescriptor().toNonNullable())
                .addTypeParameterTypeDescriptors(
                    0,
                    builder
                        .getEnclosingTypeDescriptor()
                        .getTypeDeclaration()
                        .getTypeParameterDescriptors())
                .setOrigin(MethodOrigin.SYNTHETIC_FACTORY_FOR_CONSTRUCTOR));
  }

  /** Method descriptor for the implicit (parameterless) ES6 constructor */
  private static MethodDescriptor getImplicitJavascriptConstructorDescriptor(
      DeclaredTypeDescriptor enclosingType) {
    return MethodDescriptor.newBuilder()
        .setEnclosingTypeDescriptor(enclosingType)
        .setConstructor(true)
        .setVisibility(Visibility.PUBLIC)
        .setOrigin(MethodOrigin.SYNTHETIC_NOOP_JAVASCRIPT_CONSTRUCTOR)
        .build();
  }

  /** Method descriptor for the ES6 constructor when the class has a primary constructor */
  private static MethodDescriptor getPrimaryConstructorDescriptor(
      MethodDescriptor constructorDescriptor) {

    DeclaredTypeDescriptor enclosingType = constructorDescriptor.getEnclosingTypeDescriptor();
    MethodDescriptor javascriptConstructorDeclaration =
        MethodDescriptor.newBuilder()
            .setEnclosingTypeDescriptor(enclosingType)
            .setConstructor(true)
            .setParameterDescriptors(
                constructorDescriptor.getDeclarationDescriptor().getParameterDescriptors())
            .setOriginalJsInfo(
                JsInfo.newBuilder().setJsMemberType(JsMemberType.CONSTRUCTOR).build())
            .build();

    return MethodDescriptor.Builder.from(javascriptConstructorDeclaration)
        .setDeclarationDescriptor(javascriptConstructorDeclaration)
        .setParameterDescriptors(constructorDescriptor.getParameterDescriptors())
        .build();
  }

  private static List<Statement> generateInstanceFieldDeclarationStatements(
      Type type, SourcePosition sourcePosition) {
    return type.getInstanceFields().stream()
        .map(field -> AstUtils.declarationStatement(field, sourcePosition))
        .collect(toList());
  }
}
