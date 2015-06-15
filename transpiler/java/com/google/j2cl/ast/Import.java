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
package com.google.j2cl.ast;

/**
 * A Node class that represents the goog.require statement
 * var ClassName = goog.require('moduleName').ClassName
 */
public class Import extends Node implements Comparable<Import> {

  public static final Import IMPORT_CLASS = new Import("Class", "gen.java.lang.ClassModule");
  public static final Import IMPORT_BOOTSTRAP_UTIL =
      new Import("Util", "nativebootstrap.UtilModule");

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
    this(
        typeReference.getSimpleName(),
        "gen." + typeReference.getCompilationUnitSourceName() + "Module");
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
  public int compareTo(Import other) {
    int compareModuleName = this.moduleName.compareTo(other.moduleName);
    if (compareModuleName == 0) {
      return this.className.compareTo(other.className);
    } else {
      return compareModuleName;
    }
  }
}
