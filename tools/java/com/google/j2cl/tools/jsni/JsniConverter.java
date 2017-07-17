/*
 * Copyright 2015 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package com.google.j2cl.tools.jsni;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.j2cl.common.Problems;
import com.google.j2cl.frontend.CompilationUnitsAndTypeBindings;
import com.google.j2cl.frontend.JdtParser;
import com.google.j2cl.frontend.PackageInfoCache;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Main class for the converter. This class will parse the different flags passed as arguments and
 * instantiate the different Objects needed for the conversion.
 */
public class JsniConverter {
  private final Problems problems = new Problems();
  private final String outputFile;

  public JsniConverter(String outputFile) {
    this.outputFile = outputFile;
  }

  public Problems getProblems() {
    return problems;
  }

  private CompilationUnitsAndTypeBindings getCompilationUnitsAndTypeBindings(
      List<String> javaFileNames, List<String> classPathEntries) {
    // Since this tool is currently for a one-time extraction of GWT's standard library JSNI there
    // is no special care being taken to ensure that the classpath is being properly constructed.
    // This may result in some JDT parse errors, but since we are not checking the resulting Error
    // object they are effectively being ignored.
    JdtParser jdtParser = new JdtParser(classPathEntries, problems);
    jdtParser.setIncludeRunningVMBootclasspath(true);
    CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings =
        jdtParser.parseFiles(javaFileNames);
    problems.abortIfRequested();
    return compilationUnitsAndTypeBindings;
  }

  public void convert(
      List<String> javaFileNames, List<String> classPathEntries, Set<String> excludeFileNames) {
    Multimap<String, JsniMethod> jsniMethodsByType = ArrayListMultimap.create();

    PackageInfoCache.init(classPathEntries, problems);
    problems.abortIfRequested();

    CompilationUnitsAndTypeBindings compilationUnitsAndTypeBindings =
        getCompilationUnitsAndTypeBindings(javaFileNames, classPathEntries);
    for (Entry<String, CompilationUnit> entry :
        compilationUnitsAndTypeBindings.getCompilationUnitsByFilePath().entrySet()) {
      if (excludeFileNames.contains(entry.getKey())) {
        continue;
      }

      jsniMethodsByType.putAll(
          NativeMethodExtractor.getJsniMethodsByType(
              entry.getKey(), entry.getValue(), compilationUnitsAndTypeBindings.getTypeBindings()));
    }

    new NativeJsFilesWriter(outputFile).write(jsniMethodsByType);
  }
}
