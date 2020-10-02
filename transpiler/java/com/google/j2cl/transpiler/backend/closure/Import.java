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
package com.google.j2cl.transpiler.backend.closure;

import com.google.j2cl.transpiler.ast.TypeDeclaration;

/**
 * A Node class that represents the goog.require statement:
 *
 * <pre>
 * {
 *   var ClassNameAlias = goog.require('gen.class.File.Name').
 * }
 * </pre>
 */
class Import implements Comparable<Import> {

  /**
   * Describes the category of an import.
   *
   * <p>Note that tne enum values are sorted by strength, with strongest first, to simplify the
   * collection of imports.
   */
  public enum ImportCategory {
    /** Not emitted and only exists to ensure that the name is preserved. */
    EXTERN,
    /** Not emitted and only exists to ensure that an alias is created for the current type. */
    SELF,
    /** Used in load-time during initial load of the classes. */
    LOADTIME,
    /** Used in run-time during method execution. */
    RUNTIME,
    /** Used for JsDoc purposes. */
    JSDOC,
    /**
     * Used for circumventing AJD pruning.
     *
     * <p>JavaScript types that are exposed as Java native JsTypes might be used by libraries
     * upstream. Libraries that access those types using the Java native JsType might not have them
     * in their dependencies (and that is ok). But if the exposing Java native JsType is not in the
     * same library as the original type it exposes, AJD will prune the type if there are no
     * goog.requires of it. For that reason those types are tracked as AJD_DEPENDENCIES so that the
     * proper goog.require is issued for them in the corresponding overlay class.
     */
    AJD_DEPENDENCY,
    ;

    public boolean needsGoogRequireInHeader() {
      return this == LOADTIME || this == RUNTIME || this == JSDOC || this == AJD_DEPENDENCY;
    }

    public boolean needsGoogRequireInImpl() {
      return this == LOADTIME;
    }

    public boolean needsGoogForwardDeclare() {
      return this == RUNTIME || this == JSDOC;
    }

    public boolean needsGoogModuleGet() {
      return this == RUNTIME;
    }

    boolean strongerThan(ImportCategory other) {
      return compareTo(other) < 0;
    }
  }

  private final String alias;
  private final TypeDeclaration typeDeclaration;
  private final ImportCategory importCategory;

  Import(String alias, TypeDeclaration typeDeclaration, ImportCategory importCategory) {
    this.alias = alias;
    this.typeDeclaration = typeDeclaration;
    this.importCategory = importCategory;
  }

  /** Returns the alias. */
  public String getAlias() {
    return alias;
  }

  /** Returns the importCategory. */
  public ImportCategory getImportCategory() {
    return importCategory;
  }

  /**
   * Returns the importable module path for the class impl this may be different from the file path
   * in the case of JsTypes with a customized namespace.
   */
  public String getImplModulePath() {
    return typeDeclaration.getImplModuleName();
  }

  /**
   * Returns the importable module path for the class header this may be different from the file
   * path in the case of JsTypes with a customized namespace.
   */
  public String getHeaderModulePath() {
    return typeDeclaration.getModuleName();
  }

  /** Returns the associated type descriptor. */
  public TypeDeclaration getElement() {
    return typeDeclaration;
  }

  /** Imported items should be sorted by module name first, and then class name. */
  @Override
  public int compareTo(Import that) {
    return this.getImplModulePath().compareTo(that.getImplModulePath());
  }

  @Override
  public String toString() {
    return alias + " => " + typeDeclaration + " (" + importCategory + ")";
  }
}
