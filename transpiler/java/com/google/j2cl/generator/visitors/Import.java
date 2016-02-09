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
package com.google.j2cl.generator.visitors;

import com.google.j2cl.ast.TypeDescriptor;

/**
 * A Node class that represents the goog.require statement
 * var ClassNameAlias = goog.require('gen.class.File.Name').
 */
public class Import implements Comparable<Import> {

  private String implModulePath;
  private String headerModulePath;
  private String alias;
  private TypeDescriptor typeDescriptor;

  public Import(String alias, TypeDescriptor typeDescriptor) {
    String baseModulePath = computeModulePath(typeDescriptor);

    this.headerModulePath = baseModulePath;
    this.implModulePath =
        typeDescriptor.isNative() || typeDescriptor.isExtern()
            ? baseModulePath
            : baseModulePath + "$impl";
    this.alias = alias;
    this.typeDescriptor = typeDescriptor;
  }

  /**
   * Returns the alias.
   */
  public String getAlias() {
    return alias;
  }

  /**
   * Returns the importable module path for the class impl this may be different from the file path
   * in the case of JsTypes with a customized namespace.
   */
  public String getImplModulePath() {
    return implModulePath;
  }

  /**
   * Returns the importable module path for the class header this may be different from the file
   * path in the case of JsTypes with a customized namespace.
   */
  public String getHeaderModulePath() {
    return headerModulePath;
  }

  /**
   * Returns the associated type descriptor.
   */
  public TypeDescriptor getTypeDescriptor() {
    return typeDescriptor;
  }

  /**
   * Imported items should be sorted by module name first, and then class name.
   */
  @Override
  public int compareTo(Import that) {
    return this.implModulePath.compareTo(that.implModulePath);
  }

  private static String computeModulePath(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return "vmbootstrap.primitives.$" + typeDescriptor.getBinaryName();
    }
    if (typeDescriptor.isExtern()) {
      // TODO: use "self.Foo" (it will work in more environments) when JSCompiler understands it.
      return "window." + typeDescriptor.getQualifiedName();
    }
    if (typeDescriptor.isRaw()) {
      return typeDescriptor.getBinaryName();
    }
    if (typeDescriptor.isJsType()) {
      return typeDescriptor.getQualifiedName();
    }
    return "gen." + typeDescriptor.getBinaryName();
  }
}
