/*
 * Copyright 2023 Google Inc.
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
package com.google.j2cl.transpiler.backend.wasm;

import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.backend.closure.ClosureGenerationEnvironment;
import java.util.List;

/** Information about a JavaScript method. */
@AutoValue
abstract class JsMethodImport {
  /**
   * Gets the name of the JS import for the specified JS method. This is suitable for referencing
   * the function in Wasm.
   */
  static String getJsImportName(MethodDescriptor methodDescriptor) {
    String qualifiedJsName = methodDescriptor.getQualifiedJsName();
    if (methodDescriptor.isConstructor()) {
      // TODO(b/235601426): This is required until all tests and client code are migrated to use
      // the generated imports. Remove once complete.
      qualifiedJsName = qualifiedJsName.replace("<init>", "constructor");
    } else if (methodDescriptor.isPropertyGetter()) {
      qualifiedJsName = "get " + qualifiedJsName;
    } else if (methodDescriptor.isPropertySetter()) {
      qualifiedJsName = "set " + qualifiedJsName;
    }
    return qualifiedJsName;
  }

  /** Gets a signature for the native method using the native type of the parameters. */
  static String computeSignature(
      MethodDescriptor methodDescriptor, ClosureGenerationEnvironment closureEnvironment) {
    return methodDescriptor.getParameterTypeDescriptors().stream()
        // TODO(b/285407647): Make nullability consistent for parameterized types, etc.
        .map(TypeDescriptor::toNonNullable)
        .map(closureEnvironment::getClosureTypeString)
        .collect(joining(","));
  }

  abstract String getBaseImportKey();

  /**
   * A parameter signature, used to check if parameters are compatible with other imports of the
   * same method.
   */
  abstract String getSignature();

  public abstract Method getMethod();

  @Memoized
  public String getImportKey() {
    int importNumber =
        emitAsMethodReference() || isPropertyGetter() || isPropertySetter()
            ? 0
            : getParameters().size();
    return importNumber == 0 ? getBaseImportKey() : getBaseImportKey() + "$" + importNumber;
  }

  /**
   * Qualifier for the JS method. For externs, this is the full qualifier--whatever appears before
   * final dot ('.') of the fully-qualified name. For non-externs, this is the alias to the JS type.
   */
  @Memoized
  public String getJsQualifier() {
    MethodDescriptor methodDescriptor = getMethod().getDescriptor();
    if (methodDescriptor.isExtern()) {
      ImmutableList<String> jsNameParts = splitQualifiedName(methodDescriptor.getQualifiedJsName());
      return jsNameParts.get(0);
    } else if (methodDescriptor.hasJsNamespace()) {
      return computeJsAlias(methodDescriptor.getJsNamespace());
    }
    return getJsTypeName(methodDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration());
  }

  /**
   * Short name for the JS method. For externs, this is the last part of the fully-qualified name,
   * whatever appears after the last dot ('.'). For non-externs, this is the simple JS name of the
   * method.
   */
  @Memoized
  public String getJsName() {
    MethodDescriptor methodDescriptor = getMethod().getDescriptor();
    if (methodDescriptor.isExtern()) {
      ImmutableList<String> jsNameParts = splitQualifiedName(methodDescriptor.getQualifiedJsName());
      return jsNameParts.get(1);
    }
    return methodDescriptor.getSimpleJsName();
  }

  /**
   * Splits the specified qualified name by the last dot ('.').
   *
   * @return a pair of (namespace, name).
   */
  private static ImmutableList<String> splitQualifiedName(String value) {
    int lastDot = value.lastIndexOf('.');
    return (lastDot == -1)
        ? ImmutableList.of("", value)
        : ImmutableList.of(value.substring(0, lastDot), value.substring(lastDot + 1));
  }

  public boolean isConstructor() {
    return getMethod().getDescriptor().isConstructor();
  }

  public boolean isInstance() {
    return getMethod().getDescriptor().isInstanceMember();
  }

  public boolean isPropertyGetter() {
    return getMethod().getDescriptor().isPropertyGetter();
  }

  public boolean isPropertySetter() {
    return getMethod().getDescriptor().isPropertySetter();
  }

  public List<Variable> getParameters() {
    return getMethod().getParameters();
  }

  public boolean emitAsMethodReference() {
    // As an optimization, if the method is static we can just assign the method reference EXCEPT
    // if it's a method on the Function prototype (e.g. `call`) or some other known exception.
    return !isInstance()
        && !isConstructor()
        && !isPropertyGetter()
        && !isPropertySetter()
        // TODO(b/279080129) Figure out a more permanent solution for special cases here.
        && !getJsName().equals("call")
        && !getBaseImportKey().equals("performance.now");
  }

  static boolean isCompatible(JsMethodImport existing, JsMethodImport other) {
    return existing.isConstructor() == other.isConstructor()
        && existing.isInstance() == other.isInstance()
        && existing.isPropertyGetter() == other.isPropertyGetter()
        && existing.isPropertySetter() == other.isPropertySetter();
  }

  static String getJsTypeName(TypeDeclaration typeDeclaration) {
    return AstUtils.buildQualifiedName(
        computeJsAlias(typeDeclaration.getEnclosingModule()),
        typeDeclaration.getInnerTypeQualifier());
  }

  private static String computeJsAlias(TypeDeclaration typeDeclaration) {
    if (typeDeclaration.isExtern()) {
      return typeDeclaration.getQualifiedJsName();
    }
    return computeJsAlias(typeDeclaration.getQualifiedJsName());
  }

  static String computeJsAlias(String name) {
    return name.replace('.', '_');
  }

  public static Builder newBuilder() {
    return new AutoValue_JsMethodImport.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setBaseImportKey(String value);

    public abstract Builder setSignature(String value);

    public abstract Builder setMethod(Method value);

    public abstract JsMethodImport build();
  }
}
