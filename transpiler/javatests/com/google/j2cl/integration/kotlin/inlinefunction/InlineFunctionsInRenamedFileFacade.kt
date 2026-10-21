/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("RenamedFileFacade")

package inlinefunction

private val privateTopLevelPropertyInRenamedFileFacade = 10

private fun privateTopLevelFunctionInRenamedFileFacade() = 20

// Once inlined in another module, the body can only reach the private top-level members through
// the synthetic accessors generated for this module, which live in the file facade renamed by
// `@JvmName`.
internal inline fun sumWithPrivateMembersOfRenamedFileFacade(): Int =
  privateTopLevelPropertyInRenamedFileFacade + privateTopLevelFunctionInRenamedFileFacade()
