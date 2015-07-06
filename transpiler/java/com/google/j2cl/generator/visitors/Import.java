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
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeReference;

/**
 * A Node class that represents the goog.require statement
 * var ClassName = goog.require('moduleName').ClassName
 */
public class Import extends Node implements Comparable<Import> {

  public static final Import IMPORT_CLASS = new Import("Class", "gen.java.lang.ClassModule");
  public static final Import IMPORT_NATIVE_UTIL = new Import("Util", "nativebootstrap.UtilModule");
  public static final Import IMPORT_VM_ASSERTS = new Import("Asserts", "vmbootstrap.AssertsModule");
  public static final Import IMPORT_VM_ARRAYS = new Import("Arrays", "vmbootstrap.ArraysModule");
  public static final Import IMPORT_VM_CASTS = new Import("Casts", "vmbootstrap.CastsModule");
  public static final Import IMPORT_VM_OBJECTS = new Import("Objects", "vmbootstrap.ObjectsModule");

  private String className;
  private String moduleName;

  public Import(String className, String moduleName) {
    // TODO: remove hack when Closure compiler supports circular references.
    if (moduleName.equals("gen.java.lang.ClassModule")
        || moduleName.equals("gen.java.lang.ObjectModule")) {
      moduleName = "gen.java.lang.CoreModule";
    }
    this.className = className;
    this.moduleName = moduleName;
  }

  public Import(TypeReference typeReference) {
    this(typeReference.getClassName(), typeReference.computeModuleName());
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getModuleName() {
    return moduleName;
  }

  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
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
}
