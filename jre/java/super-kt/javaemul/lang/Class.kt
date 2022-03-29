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
package javaemul.lang

import kotlin.reflect.KClass

// TODO(b/227166206): Add an implementation that's closer to JLS.

// Placeholder implementations of java.lang.Class methods used in j2cl integration tests. Note that
// the name methods do not match JLS except in some trivial cases.

val <T : Any> KClass<T>.java
  get() = this

fun <T : Any> KClass<T>.getName() = qualifiedName

fun <T : Any> KClass<T>.getCanonicalName() = qualifiedName
