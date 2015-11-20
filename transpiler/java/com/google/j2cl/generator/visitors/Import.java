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

  private String implFileName;
  private String headerFileName;
  private String alias;
  private TypeDescriptor typeDescriptor;

  public Import(String alias, TypeDescriptor typeDescriptor) {
    String baseFileName = computeBaseFileName(typeDescriptor);

    this.headerFileName = baseFileName;
    this.implFileName =
        typeDescriptor.isNative() || typeDescriptor.isExtern()
            ? baseFileName
            : baseFileName + "$impl";
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
   * Returns the class file name.
   */
  public String getImplFileName() {
    return implFileName;
  }

  /**
   * Returns the header file name.
   */
  public String getHeaderFileName() {
    return headerFileName;
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
    return this.implFileName.compareTo(that.implFileName);
  }

  private static String computeBaseFileName(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return "vmbootstrap.primitives.$" + typeDescriptor.getBinaryName();
    }
    if (typeDescriptor.isExtern()) {
      return "global." + typeDescriptor.getQualifiedName();
    }
    if (typeDescriptor.isRaw()) {
      return typeDescriptor.getBinaryName();
    }
    if (typeDescriptor.isNative()) {
      return typeDescriptor.getQualifiedName();
    }
    return "gen." + typeDescriptor.getBinaryName();
  }
}
