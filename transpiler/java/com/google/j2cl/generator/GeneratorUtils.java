/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.generator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.Visibility;
import com.google.j2cl.generator.visitors.Import;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility functions related to source generation in the J2CL AST.
 */
public class GeneratorUtils {
  public static String getBinaryName(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getBinaryName();
  }

  /**
   * Returns the relative output path for a given type.
   */
  public static String getRelativePath(JavaType javaType) {
    TypeDescriptor descriptor = javaType.getDescriptor();
    String typeName = descriptor.getClassName();
    String packageName = descriptor.getPackageName();
    return packageName.replace(".", File.separator) + File.separator + typeName;
  }

  /**
   * Returns the absolute binary path for a given type.
   */
  public static String getAbsolutePath(CompilationUnit compilationUnit, JavaType javaType) {
    TypeDescriptor descriptor = javaType.getDescriptor();
    String typeName = descriptor.getClassName();
    return compilationUnit.getDirectoryPath() + File.separator + typeName;
  }

  /**
   * Returns the absolute path for the given output FileSystem and output paths.
   */
  public static Path getAbsolutePath(
      FileSystem outputFileSystem,
      String outputLocationPath,
      String relativeFilePath,
      String suffix) {
    return outputLocationPath != null
        ? outputFileSystem.getPath(outputLocationPath, relativeFilePath + suffix)
        : outputFileSystem.getPath(relativeFilePath + suffix);
  }

  /**
   * Returns the method header including (static) (getter/setter) methodName(parametersList).
   */
  public static String getMethodHeader(Method method, GenerationEnvironment environment) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    String staticQualifier = methodDescriptor.isStatic() ? "static" : null;
    String methodName = ManglingNameUtils.getMangledName(methodDescriptor);
    String parameterList = getParameterList(method, environment);
    return Joiner.on(" ").skipNulls().join(staticQualifier, methodName + "(" + parameterList + ")");
  }

  /**
   * Returns the list of js doc annotations for the parameters in method.
   * They are of the form: {parameterType} parameterName
   */
  public static List<String> getParameterAnnotationsJsDoc(
      final Method method, final GenerationEnvironment environment) {
    final List<Variable> variables = method.getParameters();
    return Lists.transform(
        variables,
        new Function<Variable, String>() {
          @Override
          public String apply(Variable variable) {
            boolean isLast = variable == variables.get(variables.size() - 1);
            TypeDescriptor parameterTypeDescriptor = variable.getTypeDescriptor();
            String name = environment.aliasForVariable(variable);
            if (method.getDescriptor().isJsMethodVarargs() && isLast) {
              // The parameter is a js var arg so we convert the type to an array
              Preconditions.checkArgument(parameterTypeDescriptor.isArray());
              String typeName =
                  JsDocNameUtils.getJsDocName(
                      parameterTypeDescriptor.getComponentTypeDescriptor(), environment);
              return String.format("{...%s} %s", typeName, name);
            } else {
              String typeName = JsDocNameUtils.getJsDocName(parameterTypeDescriptor, environment);
              return String.format("{%s} %s", typeName, name);
            }
          }
        });
  }

  public static String getParameterList(Method method, final GenerationEnvironment environment) {
    List<String> parameterNameList =
        Lists.transform(
            method.getParameters(),
            new Function<Variable, String>() {
              @Override
              public String apply(Variable variable) {
                return environment.aliasForVariable(variable);
              }
            });
    return Joiner.on(", ").join(parameterNameList);
  }

  public static String getArgumentList(
      List<Expression> arguments, final GenerationEnvironment environment) {
    List<String> parameterNameList =
        Lists.transform(
            arguments,
            new Function<Expression, String>() {
              @Override
              public String apply(Expression expression) {
                return ExpressionTranspiler.transform(expression, environment);
              }
            });
    return Joiner.on(", ").join(parameterNameList);
  }

  public static boolean isVoid(TypeDescriptor typeDescriptor) {
    return typeDescriptor == TypeDescriptors.get().primitiveVoid;
  }

  /**
   * Returns true if the type has a superclass that is not a native js type.
   */
  public static boolean hasNonNativeSuperClass(JavaType type) {
    return type.getSuperTypeDescriptor() != null && !type.getSuperTypeDescriptor().isNative();
  }

  /**
   * Returns whether the $clinit function should be rewritten as NOP.
   */
  public static boolean needRewriteClinit(JavaType type) {
    for (Object element : type.getStaticFieldsAndInitializerBlocks()) {
      if (element instanceof Field) {
        Field field = (Field) element;
        if (field.hasInitializer() && !field.isCompileTimeConstant()) {
          return true;
        }
      } else if (element instanceof Block) {
        return true;
      } else {
        throw new UnsupportedOperationException("Unsupported element: " + element);
      }
    }
    return false;
  }

  /**
   * JsOverlayImpl type does not need $clinit function if it does not have any lazy imports and it
   * does not have any JsOverlay members.
   */
  public static boolean needClinit(JavaType type, List<Import> lazyImports) {
    if (!type.isJsOverlayImpl()) {
      return true;
    }
    return !lazyImports.isEmpty() || type.containsJsOverlay();
  }

  private GeneratorUtils() {}

  public static String getArrayAssignmentFunctionName(BinaryOperator binaryOperator) {
    switch (binaryOperator) {
      case ASSIGN:
        return "$set";
      case PLUS_ASSIGN:
        return "$addSet";
      case MINUS_ASSIGN:
        return "$subSet";
      case TIMES_ASSIGN:
        return "$mulSet";
      case DIVIDE_ASSIGN:
        return "$divSet";
      case BIT_AND_ASSIGN:
        return "$andSet";
      case BIT_OR_ASSIGN:
        return "$orSet";
      case BIT_XOR_ASSIGN:
        return "$xorSet";
      case REMAINDER_ASSIGN:
        return "$modSet";
      case LEFT_SHIFT_ASSIGN:
        return "$lshiftSet";
      case RIGHT_SHIFT_SIGNED_ASSIGN:
        return "$rshiftSet";
      case RIGHT_SHIFT_UNSIGNED_ASSIGN:
        return "$rshiftUSet";
      default:
        Preconditions.checkState(
            false, "Requested the Arrays function name for a non-assignment operator.");
        return null;
    }
  }

  /**
   * If possible, returns the qualifier of the provided expression, otherwise null.
   */
  public static Expression getQualifier(Expression expression) {
    if (!(expression instanceof MemberReference)) {
      return null;
    }
    return ((MemberReference) expression).getQualifier();
  }

  public static Expression getInitialValue(Field field) {
    if (field.isCompileTimeConstant()) {
      return field.getInitializer();
    }
    return field.getDescriptor().getTypeDescriptor().getDefaultValue();
  }

  public static boolean hasJsDoc(JavaType type) {
    return !type.getSuperInterfaceTypeDescriptors().isEmpty()
        || type.getDescriptor().isParameterizedType()
        || (type.getSuperTypeDescriptor() != null
            && type.getSuperTypeDescriptor().isParameterizedType());
  }

  /**
   * If the method is a native method with a different namespace than the current class, or it is a
   * native JsProperty method, no need to output any code for it.
   */
  public static boolean shouldNotEmitCode(Method method) {
    return method.isNative()
        && (method.getDescriptor().isJsProperty() || method.getDescriptor().isJsMethod());
  }

  public static String getExtendsClause(JavaType javaType, GenerationEnvironment environment) {
    TypeDescriptor superTypeDescriptor = javaType.getSuperTypeDescriptor();
    if (superTypeDescriptor == null) {
      return "";
    }
    String superTypeName = environment.aliasForType(superTypeDescriptor);
    return String.format("extends %s ", superTypeName);
  }

  /**
   * For a constructor of a JsConstructor class or a subclass of a JsConstructor class, as the
   * primary constructor in the class is output as the real ES6 constructor, the first statement
   * (which must either be a super() call or a this() call), has been invoked in ES6 constructor
   * and thus should be removed from the $ctor method. It follows the following rules:
   *
   * <p>If the super class has a primary constructor, which should have been output as ES6
   * constructor, the super() call should have been invoked in constructor and should be removed in
   * $ctor.
   *
   * <p>If the current class has a primary constructor, which should have been output as ES6
   * constructor, the this() call should have been invoked in constructor and should be removed in
   * $ctor.
   */
  public static Method createNonPrimaryConstructor(Method constructor) {
    TypeDescriptor currentTypeDescriptor =
        constructor.getDescriptor().getEnclosingClassTypeDescriptor();
    TypeDescriptor superclassTypeDescriptor = currentTypeDescriptor.getSuperTypeDescriptor();
    boolean removeFirstStatement =
        AstUtils.hasThisCall(constructor)
            ? currentTypeDescriptor.subclassesJsConstructorClass()
            : superclassTypeDescriptor.subclassesJsConstructorClass();
    if (removeFirstStatement) {
      return Method.Builder.from(constructor).removeStatement(0).build();
    }
    return constructor;
  }

  /**
   * Returns the arguments for 'new A' statement in constructor factory method $create.
   *
   * <p>If the given constructor delegates to the primary constructor, the delegating this() call
   * should be invoked by the 'new A' statement in the $create factory method, thus passing the
   * arguments here.
   */
  public static String getNewInstanceArguments(Method method, GenerationEnvironment environment) {
    TypeDescriptor typeDescriptor = method.getDescriptor().getEnclosingClassTypeDescriptor();
    if (!typeDescriptor.subclassesJsConstructorClass()) {
      return "";
    }
    MethodCall constructorInvocation = AstUtils.getConstructorInvocation(method);
    Preconditions.checkNotNull(
        constructorInvocation,
        "constructor %s must delegate to the primary constructor",
        ManglingNameUtils.getConstructorMangledName(method.getDescriptor()));
    return getArgumentList(constructorInvocation.getArguments(), environment);
  }

  /**
   * Returns the arguments for 'super()' call in ES6 constructor.
   *
   * <p>If the given constructor invokes the primary constructor in its super class, pass in the
   * arguments to the super call here.
   */
  public static String getSuperArguments(
      Method primaryConstructor, GenerationEnvironment environment) {
    MethodCall constructorInvocation = AstUtils.getConstructorInvocation(primaryConstructor);
    if (constructorInvocation == null) {
      // This case happens when the super class is a native type, and we do not synthesize the
      // default super() call for the default constructor.
      return "";
    }
    return constructorInvocation
            .getTarget()
            .getEnclosingClassTypeDescriptor()
            .subclassesJsConstructorClass()
        ? getArgumentList(constructorInvocation.getArguments(), environment)
        : "";
  }

  /**
   * The ctor visibility should never be public since it is not intended to be called externally
   * unless a super class calls it as part of the ctor chain.
   */
  public static String visibilityForMethod(Method method) {
    if (method.isConstructor() && method.getDescriptor().getVisibility() == Visibility.PUBLIC) {
      return "protected";
    }
    return method.getDescriptor().getVisibility().jsText;
  }
}
