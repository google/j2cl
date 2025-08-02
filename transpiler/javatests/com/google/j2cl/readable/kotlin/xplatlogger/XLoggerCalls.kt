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
package xplatlogger

import com.google.apps.xplat.logging.LoggingApi
import com.google.apps.xplat.logging.XLogLevel
import com.google.apps.xplat.logging.XLogger

fun log() {
  XLogger.getLogger(Any::class.java).atInfo().log("X")
}

fun log(logger: XLogger) {
  logger.atInfo().log("X")
  logger.atInfo().log("X", 1)
  // varargs overload
  logger.atInfo().withCause(Throwable()).log("X")
}

fun isEnabled(logger: XLogger) {
  logger.atInfo().isEnabled()
}

fun nonTerminating(logger: XLogger) {
  logger.atInfo()
  logger.atInfo().withCause(Throwable()).log("X")
}

fun nonOptimizable(logger: XLogger, level: XLogLevel) {
  // Not known fluent chain.
  atInfo().log("X")

  // Dynamic level
  logger.loggingAt(level).log("X")

  // withCause call on isEnabled with potential side effects.
  logger.atInfo().withCause(hasSideEffect()).isEnabled()

  // Multiple withCause calls, with potential side effects.
  logger.atInfo().withCause(hasSideEffect()).withCause(Throwable()).log("X")
}

private fun atInfo(): LoggingApi = null as LoggingApi

private fun hasSideEffect() = null as Throwable
