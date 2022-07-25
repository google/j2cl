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

interface JavaMap<K, V> : MutableMap<K, V> {

  override fun containsKey(key: K): Boolean = java_containsKey(key)

  override fun containsValue(value: V): Boolean = java_containsValue(value)

  override operator fun get(key: K): V? = java_get(key)

  override fun remove(key: K): V? = java_remove(key)

  abstract fun java_containsKey(key: Any?): Boolean

  abstract fun java_containsValue(value: Any?): Boolean

  abstract fun java_get(key: Any?): V?

  abstract fun java_remove(key: Any?): V?
}

// Note: No need to check for the runtime type below. The bridge interface is
// only necessary to allow overriding with the Java signature. Calling with the
// Java signature does not require any trampolines, only manual type erasure.

@Suppress("UNCHECKED_CAST")
fun <K, V> MutableMap<K, V>.java_containsKey(key: Any?): Boolean = containsKey(key as K)

@Suppress("UNCHECKED_CAST")
fun <K, V> MutableMap<K, V>.java_containsValue(value: Any?): Boolean = containsValue(value as V)

@Suppress("UNCHECKED_CAST") fun <K, V> MutableMap<K, V>.java_get(key: Any?): V? = get(key as K)

@Suppress("UNCHECKED_CAST")
fun <K, V> MutableMap<K, V>.java_remove(key: Any?): V? = remove(key as K)
