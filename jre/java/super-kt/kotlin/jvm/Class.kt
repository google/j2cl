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
package kotlin.jvm

import java.lang.Class
import kotlin.reflect.KClass

private val classMap: MutableMap<KClass<*>, Class<*>> = mutableMapOf()

// TODO(b/227166206): Add synchronization to make it thread-safe.
val <T : Any> KClass<T>.java: Class<T>
  get() = classMap.getOrPut(this) { Class<T>(this) } as Class<T>
