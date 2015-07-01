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
import com.google.j2cl.ast.processors.Visitable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A model class that represents a Java Compilation Unit.
 */
@Visitable
public class CompilationUnit extends Node {

  private String filePath;
  private String packageName;
  @Visitable List<JavaType> types = new ArrayList<>();

  public CompilationUnit(String filePath, String packageName) {
    Preconditions.checkArgument(filePath != null);
    Preconditions.checkArgument(packageName != null);
    this.filePath = filePath;
    this.packageName = packageName;
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

  public void addTypes(Collection<JavaType> types) {
    this.types.addAll(types);
  }

  public List<JavaType> getTypes() {
    return types;
  }

  public String getName() {
    int endIndex = filePath.lastIndexOf(".java");
    Preconditions.checkState(endIndex != -1);

    return filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, endIndex);
  }

  @Override
  public CompilationUnit accept(Processor processor) {
    return Visitor_CompilationUnit.visit(processor, this);
  }
}
