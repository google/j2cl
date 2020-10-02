/*
 * Copyright 2019 Google Inc.
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
package com.google.j2cl.transpiler.frontend;

import com.google.j2cl.common.Problems;
import com.google.j2cl.common.SourceUtils.FileInfo;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache;
import com.google.j2cl.transpiler.frontend.javac.JavacParser;
import com.google.j2cl.transpiler.frontend.jdt.CompilationUnitBuilder;
import com.google.j2cl.transpiler.frontend.jdt.CompilationUnitsAndTypeBindings;
import com.google.j2cl.transpiler.frontend.jdt.JdtParser;
import java.util.List;

/** Drives the frontend to parse, type check and resolve Java source code. */
public enum Frontend {
  JDT {
    @Override
    public List<CompilationUnit> getCompilationUnits(
        List<String> classPath,
        List<FileInfo> sources,
        boolean useTargetClassPath,
        Problems problems) {
      CompilationUnitsAndTypeBindings jdtUnitsAndResolvedBindings =
          createJdtUnitsAndResolveBindings(classPath, sources, useTargetClassPath, problems);
      return convertUnits(jdtUnitsAndResolvedBindings, classPath, problems);
    }

    private List<CompilationUnit> convertUnits(
        CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings,
        List<String> classPath,
        Problems problems) {
      init(classPath, problems);
      return CompilationUnitBuilder.build(compilationUnitsAndTypeBindings);
    }

    private CompilationUnitsAndTypeBindings createJdtUnitsAndResolveBindings(
        List<String> classPath,
        List<FileInfo> sources,
        boolean useTargetClassPath,
        Problems problems) {
      JdtParser parser = new JdtParser(classPath, problems);
      CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings =
          parser.parseFiles(sources, useTargetClassPath);
      problems.abortIfHasErrors();
      return compilationUnitsAndTypeBindings;
    }
  },
  JAVAC {
    @Override
    public List<CompilationUnit> getCompilationUnits(
        List<String> classPath,
        List<FileInfo> sources,
        boolean useTargetClassPath,
        Problems problems) {
      init(classPath, problems);
      return new JavacParser(classPath, problems).parseFiles(sources, useTargetClassPath);
    }
  };

  public abstract List<CompilationUnit> getCompilationUnits(
      List<String> classPath,
      List<FileInfo> sources,
      boolean useTargetClassPath,
      Problems problems);

  private static void init(List<String> classPath, Problems problems) {
    // Records information about package-info files supplied as byte code.
    PackageInfoCache.init(classPath, problems);
  }
}
