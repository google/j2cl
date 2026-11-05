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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.backend.closure.ClosureGenerationEnvironment;
import java.util.List;

/**
 * Information about a JavaScript method imported into Wasm.
 *
 * <p>An import is created from the import redeclaration of a native method, and describes the
 * JavaScript member that the native method declares.
 */
@AutoValue
abstract class JsMethodImport {
  /** Creates the import for the import redeclaration of a native method. */
  static JsMethodImport create(
      Method importRedeclaration, ClosureGenerationEnvironment closureEnvironment) {
    MethodDescriptor methodDescriptor = importRedeclaration.getDescriptor();
    checkArgument(
        methodDescriptor.getOrigin() == MethodOrigin.WASM_JS_NATIVE_IMPORT_REDECLARATION,
        "Not an import redeclaration: %s",
        methodDescriptor.getReadableDescription());
    MethodDescriptor nativeMethodDescriptor = methodDescriptor.getBridgeTarget();
    return new AutoValue_JsMethodImport(
        nativeMethodDescriptor,
        importRedeclaration.getSourcePosition(),
        computeJsImportName(nativeMethodDescriptor),
        // The parameters of the redeclaration have the types that cross the boundary.
        computeSignature(methodDescriptor, closureEnvironment));
  }

  /**
   * Gets the name of the JS import for the specified JS method. This is suitable for referencing
   * the function in Wasm.
   */
  private static String computeJsImportName(MethodDescriptor nativeMethodDescriptor) {
    String qualifiedJsName = nativeMethodDescriptor.getQualifiedJsName();
    if (nativeMethodDescriptor.isConstructor()) {
      // TODO(b/235601426): This is required until all tests and client code are migrated to use
      // the generated imports. Remove once complete.
      qualifiedJsName = qualifiedJsName.replace("<init>", "constructor");
    } else if (nativeMethodDescriptor.isPropertyGetter()) {
      qualifiedJsName = "get " + qualifiedJsName;
    } else if (nativeMethodDescriptor.isPropertySetter()) {
      qualifiedJsName = "set " + qualifiedJsName;
    }
    return qualifiedJsName;
  }

  /** Gets a signature for the import using the types of the parameters that cross the boundary. */
  private static String computeSignature(
      MethodDescriptor methodDescriptor, ClosureGenerationEnvironment closureEnvironment) {
    return methodDescriptor.getParameterTypeDescriptors().stream()
        // TODO(b/285407647): Make nullability consistent for parameterized types, etc.
        .map(TypeDescriptor::toNonNullable)
        .map(closureEnvironment::getClosureTypeString)
        .collect(joining(","));
  }

  /** The native method that declares the JavaScript member being imported. */
  public abstract MethodDescriptor getMethodDescriptor();

  /** The source position of the native method, for error reporting. */
  public abstract SourcePosition getSourcePosition();

  abstract String getBaseImportKey();

  /**
   * A parameter signature, used to check if parameters are compatible with other imports of the
   * same method.
   */
  abstract String getSignature();

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
    MethodDescriptor methodDescriptor = getMethodDescriptor();
    if (methodDescriptor.isExtern()) {
      ImmutableList<String> jsNameParts = splitQualifiedName(methodDescriptor.getQualifiedJsName());
      return jsNameParts.getFirst();
    } else if (methodDescriptor.hasJsNamespace()) {
      return WasmJsInteropClosureGenerationEnvironment.computeJsAlias(
          methodDescriptor.getJsNamespace());
    }
    return WasmJsInteropClosureGenerationEnvironment.getJsTypeAlias(
        methodDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration());
  }

  /**
   * Short name for the JS method. For externs, this is the last part of the fully-qualified name,
   * whatever appears after the last dot ('.'). For non-externs, this is the simple JS name of the
   * method.
   */
  @Memoized
  public String getJsName() {
    MethodDescriptor methodDescriptor = getMethodDescriptor();
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
    return getMethodDescriptor().isConstructor();
  }

  public boolean isInstance() {
    return getMethodDescriptor().isInstanceMember();
  }

  public boolean isPropertyGetter() {
    return getMethodDescriptor().isPropertyGetter();
  }

  public boolean isPropertySetter() {
    return getMethodDescriptor().isPropertySetter();
  }

  public List<ParameterDescriptor> getParameters() {
    return getMethodDescriptor().getParameterDescriptors();
  }

  /** Returns the JavaScript modules that need to be imported to reference the member. */
  public ImmutableSet<String> getModuleDependencies() {
    return WasmJsInteropClosureGenerationEnvironment.getJsModuleDependencies(getMethodDescriptor());
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
}
