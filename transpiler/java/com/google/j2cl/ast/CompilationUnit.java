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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import com.google.j2cl.ast.annotations.Context;
import com.google.j2cl.ast.annotations.Visitable;
import com.google.j2cl.common.J2clUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * A model class that represents a Java Compilation Unit.
 */
@Visitable
@Context
public class CompilationUnit extends Node {

  private static final String COMPILATION_UNIT_FILENAME_SUFFIX = ".java";

  private final String filePath;
  private final String packageName;
  @Visitable List<Type> types = new ArrayList<>();

  public CompilationUnit(String filePath, String packageName) {
    this.filePath = checkNotNull(filePath);
    this.packageName = checkNotNull(packageName);
    checkState(filePath.endsWith(COMPILATION_UNIT_FILENAME_SUFFIX));
  }

  public String getFilePath() {
    return filePath;
  }

  public String getDirectoryPath() {
    if (!filePath.contains(J2clUtils.FILEPATH_SEPARATOR)) {
      return "";
    }
    return filePath.substring(0, filePath.lastIndexOf(J2clUtils.FILEPATH_SEPARATOR));
  }

  public String getFileName() {
    String[] pathComponents = filePath.split(J2clUtils.FILEPATH_SEPARATOR);
    return pathComponents[pathComponents.length - 1];
  }

  public String getPackageName() {
    return packageName;
  }

  public void addType(Type type) {
    this.types.add(checkNotNull(type));
  }

  public List<Type> getTypes() {
    return types;
  }

  public Type getType(final TypeDescriptor typeDescriptor) {
    return Optional.fromJavaUtil(
            types
                .stream()
                .filter(
                    type ->
                        type.getDescriptor()
                            .getRawTypeDescriptor()
                            .equalsIgnoreNullability(typeDescriptor.getRawTypeDescriptor()))
                .findFirst())
        .orNull();
  }

  public String getName() {
    return filePath.substring(
        filePath.lastIndexOf(J2clUtils.FILEPATH_SEPARATOR_CHAR) + 1,
        filePath.length() - COMPILATION_UNIT_FILENAME_SUFFIX.length());
  }

  @Override
  public Node accept(Processor processor) {
    return Visitor_CompilationUnit.visit(processor, this);
  }
}
