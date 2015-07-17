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

import com.google.common.collect.ComparisonChain;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;

/**
 * A Node class that represents the goog.require statement
 * var ClassNameAlias = goog.require('moduleName').ClassName
 */
public class Import implements Comparable<Import> {
  public static final String IMPORT_VM_PRIMITIVES_MODULE = "vmbootstrap.PrimitivesModule";

  private String className;
  private String moduleName;
  private String alias;
  private TypeDescriptor typeDescriptor;

  Import(String alias, TypeDescriptor typeDescriptor) {
    String className = typeDescriptor.getClassName();
    String moduleName = computeModuleName(typeDescriptor);

    // TODO: remove hack when Closure compiler supports circular references.
    if (typeDescriptor == TypeDescriptors.OBJECT_TYPE_DESCRIPTOR
        || typeDescriptor == TypeDescriptors.CLASS_TYPE_DESCRIPTOR) {
      moduleName = computeModuleName("java.lang.Core");
    }
    this.className = className;
    this.moduleName = moduleName;
    this.alias = alias;
    this.typeDescriptor = typeDescriptor;
  }

  /**
   * Returns the class name.
   */
  public String getClassName() {
    return className;
  }

  /**
   * Returns the alias.
   */
  public String getAlias() {
    return alias;
  }

  /**
   * Returns the module name.
   */
  public String getModuleName() {
    return moduleName;
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
    return ComparisonChain.start()
        .compare(this.moduleName, that.moduleName)
        .compare(this.className, that.className)
        .result();
  }

  private static String computeModuleName(TypeDescriptor typeDescriptor) {
    if (typeDescriptor.isPrimitive()) {
      return IMPORT_VM_PRIMITIVES_MODULE;
    }
    if (typeDescriptor.isRaw()) {
      return typeDescriptor.getCompilationUnitSourceName();
    }
    return computeModuleName(typeDescriptor.getCompilationUnitSourceName());
  }

  private static String computeModuleName(String compilationUnitSourceName) {
    return "gen." + compilationUnitSourceName + "Module";
  }
}
