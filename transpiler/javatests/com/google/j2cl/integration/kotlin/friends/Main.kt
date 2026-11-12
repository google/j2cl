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
@file:Suppress("DiscourageTopLevelFunctions")

package friends

import com.google.j2cl.integration.testing.Asserts.assertEquals
import friends.friend.friendFunction
import friends.library.inferredReturnTypeFromFriend
import friends.library.inferredReturnTypeFromNonFriend
import friends.library.inlineCallToFriend
import friends.library.inlineCallToNonFriend

private fun staticType(unused: Any): String = "Any"

private fun staticType(unused: String): String = "String"

fun main(vararg unused: String) {
  // Friend.kt is a friend of Main.kt, so its internal friendFunction(String) overload is visible.
  assertEquals("internal", friendFunction("ignored"))
  assertEquals("String", staticType(friendFunction("ignored")))

  // Main.kt resolves Library.kt's public inlineCallToFriend(Any) ("public-"). Inside Library.kt,
  // LibraryFriend.kt's internal overload ("internal") is visible, while NonFriend.kt's is not
  // ("public").
  assertEquals("public-internal", inlineCallToFriend("ignored"))
  assertEquals("public-public", inlineCallToNonFriend())

  // Library.kt's inferred return types come from the overload visible to Library.kt:
  // internal friendFunction(String): String in LibraryFriend.kt vs. public nonFriendFunction(Any):
  // Any in NonFriend.kt.
  assertEquals("String", staticType(inferredReturnTypeFromFriend()))
  assertEquals("internal", inferredReturnTypeFromFriend())
  assertEquals("Any", staticType(inferredReturnTypeFromNonFriend()))
  assertEquals("public", inferredReturnTypeFromNonFriend())
}
