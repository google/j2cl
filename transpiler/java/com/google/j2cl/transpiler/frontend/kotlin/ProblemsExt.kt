/*
 * Copyright 2025 Google Inc.
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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus

internal fun Problems.registerForCancellation() = globalProblems.set(this)

// Track problems on a thread local so a cancelation doesn't effect other compilation threads.
private val globalProblems: ThreadLocal<Problems> =
  ThreadLocal<Problems>().also {
    // Register a compilation canceled status to transfer cancellation from the "problems" object.
    ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(
      object : CompilationCanceledStatus {
        override fun checkCanceled() {
          // throw CompilationCanceledException instead of our own which is properly handled by
          // kotlinc to gracefully exit from the compilation.
          if (globalProblems.get().isCancelled) {
            throw CompilationCanceledException().initCause(Problems.Exit())
          }
        }
      }
    )
  }

internal fun Problems.createMessageCollector(): MessageCollector =
  object : MessageCollector {
    override fun clear() {
      // This implementation does not support clearing error messages.
    }

    override fun hasErrors(): Boolean {
      return this@createMessageCollector.hasErrors()
    }

    override fun report(
      severity: CompilerMessageSeverity,
      message: String,
      location: CompilerMessageSourceLocation?,
    ) {
      if (!severity.isError) {
        return
      }

      if (location != null) {
        error(location.line, location.path, "%s", message)
      } else {
        error("%s", message)
      }
    }
  }
