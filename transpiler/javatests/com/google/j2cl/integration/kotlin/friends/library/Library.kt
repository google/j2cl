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
@file:Suppress("DiscourageTopLevelFunctions", "NOTHING_TO_INLINE", "ExplicitApiType")

package friends.library

import friends.library.friend.friendFunction
import friends.nonfriend.nonFriendFunction

// Main.kt (calling inlineCallToFriend("ignored")) should expect "public-internal":
// - "public-" because Library.kt is not a friend of Main.kt during J2CL transpilation.
// - "internal" because LibraryFriend.kt is a friend of Library.kt during header KLIB compilation.
inline fun inlineCallToFriend(p: Any): String = "public-" + friendFunction("ignored")

// Verifies J2CL transpilation's friend check from Main.kt; should not be selected by Main.kt.
internal inline fun inlineCallToFriend(p: String): String = "internal-" + friendFunction("ignored")

inline fun inlineCallToNonFriend(): String = "public-" + nonFriendFunction("ignored")

fun inferredReturnTypeFromFriend() = friendFunction("ignored")

fun inferredReturnTypeFromNonFriend() = nonFriendFunction("ignored")
