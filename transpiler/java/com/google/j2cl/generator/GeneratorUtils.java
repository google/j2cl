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
import com.google.j2cl.ast.MemberReference;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
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
  public static String getMethodHeader(Method method, SourceGenerator sourceGenerator) {
    MethodDescriptor methodDescriptor = method.getDescriptor();
    String staticQualifier = methodDescriptor.isStatic() ? "static" : null;
    String getterSetterPrefix =
        methodDescriptor.isJsPropertyGetter()
            ? "get"
            : methodDescriptor.isJsPropertySetter() ? "set" : null;
    String methodName = ManglingNameUtils.getMangledName(methodDescriptor);
    String parameterList = getParameterList(method, sourceGenerator);
    return Joiner.on(" ")
        .skipNulls()
        .join(staticQualifier, getterSetterPrefix, methodName + "(" + parameterList + ")");
  }

  /**
   * Returns the list of js doc annotations for the parameters in method.
   * They are of the form: {parameterType} parameterName
   */
  public static List<String> getParameterAnnotationsJsDoc(
      final Method method, final SourceGenerator sourceGenerator) {
    final List<Variable> variables = method.getParameters();
    return Lists.transform(
        variables,
        new Function<Variable, String>() {
          @Override
          public String apply(Variable variable) {
            boolean isLast = variable == variables.get(variables.size() - 1);
            TypeDescriptor parameterTypeDescriptor = variable.getTypeDescriptor();
            String name = sourceGenerator.toSource(variable);
            if (method.getDescriptor().isJsMethodVarargs() && isLast) {
              // The parameter is a js var arg so we convert the type to an array
              Preconditions.checkArgument(parameterTypeDescriptor.isArray());
              String typeName =
                  sourceGenerator.getJsDocName(
                      parameterTypeDescriptor.getComponentTypeDescriptor());
              return String.format("{...%s} %s", typeName, name);
            } else {
              String typeName = sourceGenerator.getJsDocName(parameterTypeDescriptor);
              return String.format("{%s} %s", typeName, name);
            }
          }
        });
  }

  public static String getParameterList(Method method, final SourceGenerator sourceGenerator) {
    return getSourceList(method.getParameters(), sourceGenerator);
  }

  public static <T extends Node> String getSourceList(
      List<T> nodes, final SourceGenerator sourceGenerator) {
    List<String> sourceList =
        Lists.transform(
            nodes,
            new Function<T, String>() {
              @Override
              public String apply(T node) {
                return sourceGenerator.toSource(node);
              }
            });
    return Joiner.on(", ").join(sourceList);
  }

  public static boolean isVoid(TypeDescriptor typeDescriptor) {
    return typeDescriptor.getClassName().equals("void");
  }

  /**
   * Returns true if the type has a superclass that is not a native js type.
   */
  public static boolean needCallSuperClinit(JavaType type) {
    return type.getSuperTypeDescriptor() != null && !type.getSuperTypeDescriptor().isNative();
  }

  /**
   * Returns whether the $clinit function should be rewritten as NOP.
   */
  public static boolean needRewriteClinit(JavaType type) {
    for (Field staticField : type.getStaticFields()) {
      if (staticField.hasInitializer()) {
        return true;
      }
    }
    return !type.getStaticInitializerBlocks().isEmpty();
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

  // Returns if it is java type that is unboxed as Javascript primitive types.
  // java.lang.Boolean, java.lang.Double and java.lang.String are unboxed as JS primitives.
  public static boolean isBoxedTypeAsPrimitive(TypeDescriptor typeDescriptor) {
    return TypeDescriptors.isBoxedTypeAsJsPrimitives(typeDescriptor);
  }

  // Returns true if typeDescriptor is unboxed as JS primitive types or it is super class or super
  // interfaces of these types.
  public static boolean superBoxedTypeAsPrimitive(TypeDescriptor typeDescriptor) {
    // TODO: add
    // ... || typeDescriptor.getRawTypeDescriptor()
    //        == TypeDescriptors.get().javaLangComparable.getRawTypeDescriptor()
    // || typeDescriptor == TypeDescriptors.get().javaLangCharSequence
    // We are not having tests for 'DoubleInstance instanceof Comparable', currently just emit
    // regular $isInstance methods for Comparable and CharSequence.
    return isBoxedTypeAsPrimitive(typeDescriptor)
        || typeDescriptor == TypeDescriptors.get().javaLangNumber;
  }

  public static String getExtendsClause(JavaType javaType, SourceGenerator sourceGenerator) {
    TypeDescriptor superTypeDescriptor = javaType.getSuperTypeDescriptor();
    if (superTypeDescriptor == null) {
      return "";
    }
    String superTypeName = sourceGenerator.toSource(superTypeDescriptor);
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
  public static Block getCtorBody(Method method) {
    TypeDescriptor currentTypeDescriptor = method.getDescriptor().getEnclosingClassTypeDescriptor();
    TypeDescriptor superclassTypeDescriptor = currentTypeDescriptor.getSuperTypeDescriptor();
    boolean removeFirstStatement =
        AstUtils.hasThisCall(method)
            ? currentTypeDescriptor.subclassesJsConstructorClass()
            : superclassTypeDescriptor.subclassesJsConstructorClass();
    if (removeFirstStatement) {
      List<Statement> statements = method.getBody().getStatements();
      return new Block(statements.subList(1, statements.size()));
    }
    return method.getBody();
  }

  /**
   * Returns the arguments for 'new A' statement in constructor factory method $create.
   *
   * <p>If the given constructor delegates to the primary constructor, the delegating this() call
   * should be invoked by the 'new A' statement in the $create factory method, thus passing the
   * arguments here.
   */
  public static String getNewInstanceArguments(Method method, SourceGenerator sourceGenerator) {
    TypeDescriptor typeDescriptor = method.getDescriptor().getEnclosingClassTypeDescriptor();
    if (!typeDescriptor.subclassesJsConstructorClass()) {
      return "";
    }
    MethodCall constructorInvocation = AstUtils.getConstructorInvocation(method);
    Preconditions.checkNotNull(
        constructorInvocation,
        "constructor %s should delegate to the delegated constructor",
        ManglingNameUtils.getConstructorMangledName(method.getDescriptor()));
    return getSourceList(constructorInvocation.getArguments(), sourceGenerator);
  }

  /**
   * Returns the arguments for 'super()' call in ES6 constructor.
   *
   * <p>If the given constructor invokes the primary constructor in its super class, pass in the
   * arguments to the super call here.
   */
  public static String getSuperArguments(
      Method primaryConstructor, SourceGenerator sourceGenerator) {
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
        ? GeneratorUtils.getSourceList(constructorInvocation.getArguments(), sourceGenerator)
        : "";
  }
}
