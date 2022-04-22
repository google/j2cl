/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.transpiler.frontend.kotlin

import com.google.j2cl.common.Problems
import com.google.j2cl.common.SourceUtils
import com.google.j2cl.transpiler.ast.CompilationUnit

/** Kotlin frontend is not supported yet in open-source due to build related issues. */
class KotlinParser(private val classpathEntries: List<String>, private val kotlincopts: List<String>, private val problems: Problems) {

  fun parseFiles(filePaths: List<SourceUtils.FileInfo>): List<CompilationUnit> {
    return emptyList()
  }
}
