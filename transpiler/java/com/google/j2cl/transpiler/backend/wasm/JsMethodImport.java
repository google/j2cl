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

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

/**
 * Information about a JavaScript method imported into Wasm.
 *
 * <p>An import is created from the import redeclaration of a native method, and describes the
 * JavaScript member that the native method declares.
 */
@AutoValue
abstract class JsMethodImport {
  /** Creates the import for the import redeclaration of a native method. */
  static JsMethodImport create(Method importRedeclaration) {
    MethodDescriptor methodDescriptor = importRedeclaration.getDescriptor();
    checkArgument(
        methodDescriptor.getOrigin() == MethodOrigin.WASM_JS_NATIVE_IMPORT_REDECLARATION,
        "Not an import redeclaration: %s",
        methodDescriptor.getReadableDescription());
    MethodDescriptor nativeMethodDescriptor = methodDescriptor.getBridgeTarget();
    return new AutoValue_JsMethodImport(
        nativeMethodDescriptor,
        WasmJsInteropClosureGenerationEnvironment.getJsModuleDependency(nativeMethodDescriptor));
  }

  /** The native method that declares the JavaScript member being imported. */
  public abstract MethodDescriptor getMethodDescriptor();

  /** The JavaScript module that needs to be imported at runtime to reference the member, if any. */
  @Nullable
  public abstract String getModuleDependency();

  /**
   * Gets the external Wasm/JS import name for this method.
   *
   * <p>There is a 1-to-1 relationship between the import key and the generated JS implementation
   * snippet, so any two native methods that produce the same key are guaranteed to have identical
   * snippets.
   */
  @Memoized
  public String getImportKey() {
    MethodDescriptor method = getMethodDescriptor();
    String baseName = method.getQualifiedJsName();
    String scopeSuffix = method.isInstanceMember() ? "#i" : "";
    int fixedParameterCount =
        method.getParameterDescriptors().size() - (method.isVarargs() ? 1 : 0);

    if (emitAsMethodReference()) {
      return baseName;
    }
    if (method.isPropertyGetter()) {
      String prefix =
          TypeDescriptors.isPrimitiveVoid(method.getReturnTypeDescriptor()) ? "exec " : "get ";
      return prefix + baseName + scopeSuffix;
    }
    if (method.isPropertySetter()) {
      return "set " + baseName + scopeSuffix;
    }
    return baseName + "#" + fixedParameterCount + (method.isVarargs() ? "+" : "") + scopeSuffix;
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

  public boolean emitAsMethodReference() {
    MethodDescriptor method = getMethodDescriptor();
    // As an optimization, if the method is static we can just assign the method reference EXCEPT
    // if it's a method on the Function prototype (e.g. `call`) or some other known exception.
    return !method.isInstanceMember()
        && !method.isConstructor()
        && !method.isPropertyGetter()
        && !method.isPropertySetter()
        // Varargs are passed from Wasm as a JS array that needs to be spread when calling the JS
        // method, so a wrapper is needed.
        && !method.isVarargs()
        // TODO(b/279080129) Figure out a more permanent solution for special cases here.
        && !getJsName().equals("call")
        && !method.getQualifiedJsName().equals("performance.now");
  }
}
