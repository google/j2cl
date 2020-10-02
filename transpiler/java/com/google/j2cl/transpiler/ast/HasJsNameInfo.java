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
package com.google.j2cl.transpiler.ast;

/** Abstracts JsInterop name related information for the AST nodes. */
public interface HasJsNameInfo {
  /** The name specified directly on a type, method or field. */
  String getSimpleJsName();

  /** The namespace specified on a package, type, method or field. */
  String getJsNamespace();

  /** Whether it is a native type, method or field. */
  boolean isNative();

  /** Returns the qualified JavaScript name of a type, method or field. */
  default String getQualifiedJsName() {
    if (JsUtils.isGlobal(getJsNamespace())) {
      return getSimpleJsName();
    }
    return AstUtils.buildQualifiedName(getJsNamespace(), getSimpleJsName());
  }
}
