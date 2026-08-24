/*
 * Copyright 2021 Google Inc.
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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin

import com.google.j2cl.common.Problems
import com.google.j2cl.transpiler.ast.Library
import com.google.j2cl.transpiler.frontend.common.FrontendOptions

/** A parser for Kotlin sources that builds {@code CompilationtUnit}s. */
class KotlinParser(private val problems: Problems) {
  /** Returns a list of compilation units after Kotlinc parsing. */
  fun parseFiles(options: FrontendOptions): Library {
    check(options.enableKlibs) { "Legacy pipeline is not supported anymore." }

    return KlibsKotlinParser(problems).parseFiles(options)
  }
}
