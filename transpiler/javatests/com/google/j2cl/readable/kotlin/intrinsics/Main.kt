/*
 * Copyright 2023 Google Inc.
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
package intrinsics

// For Kotlin/JVM these two super types are mapped to the same type. This is not typically possible
// with typealiases as kotlinc will enforce that you don't have duplicate supertypes after aliases
// are resolved.
// See: b/308776304
interface IFoo<T> : MutableCollection<T>, Collection<T>

abstract class Foo<T> : MutableCollection<T>, Collection<T>
