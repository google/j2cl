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

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A model class that represents a Java Compilation Unit.
 */
public class CompilationUnit extends Node {

  private String filePath;
  private String packageName;
  private Set<Imports> requiredModules = new TreeSet<>();
  @Visitable private List<JavaType> types = new ArrayList<>();

  public CompilationUnit(String filePath, String packageName) {
    Preconditions.checkArgument(filePath != null);
    Preconditions.checkArgument(packageName != null);
    this.filePath = filePath;
    this.packageName = packageName;
    requiredModules.add(Imports.IMPORT_CLASS);
    requiredModules.add(Imports.IMPORT_UTIL);
    // TODO: remove when imports are correclty computed.
    requiredModules.add(Imports.IMPORT_OBJECT);
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getPackageName() {
    return packageName;
  }

  public void addType(JavaType type) {
    types.add(type);
  }

  public List<JavaType> getTypes() {
    return types;
  }

  public void addRequiredModule(Imports module) {
    // only the class that is not in current module can be added.
    if (module.getModuleName().equals(getName())) {
      return;
    }
    requiredModules.add(module);
  }

  public Set<Imports> getRequiredModules() {
    return requiredModules;
  }

  public String getName() {
    int endIndex = filePath.lastIndexOf(".java");
    Preconditions.checkState(endIndex != -1);

    return filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, endIndex);
  }
}
