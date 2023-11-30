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
package com.google.j2cl.transpiler.backend.kotlin.common

/** Returns this object if [condition] is true, otherwise the result of applying [fn]. */
inline fun <T> T.letIf(condition: Boolean, fn: (T) -> T): T = if (condition) fn(this) else this

/** Returns this object if [condition] is true, otherwise the result of applying [fn]. */
inline fun <T> T.runIf(condition: Boolean, fn: T.() -> T): T = if (condition) fn() else this

/** Returns this object if it's not null, otherwise the result of [fn]. */
inline fun <T> T?.orIfNull(fn: () -> T): T = this ?: fn()
