/*
 * Copyright 2024 Google Inc.
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

import com.google.devtools.kotlin.common.AutoFriends
import com.google.devtools.kotlin.common.BzlLabel
import com.google.devtools.kotlin.common.KtManifest
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache
import java.util.jar.Manifest
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

internal fun K2JVMCompilerArguments.setEligibleFriends(
  packageInfoCache: PackageInfoCache,
  currentTarget: String?,
) {
  if (currentTarget == null) return

  val currentLabel = BzlLabel.parseOrThrow(currentTarget)
  this.friendPaths =
    this.classpath
      .orEmpty()
      .split(":")
      .filter {
        val depLabel = packageInfoCache.getManifest(it)?.targetLabel ?: return@filter false
        AutoFriends.isEligibleFriend(currentLabel, depLabel)
      }
      .toTypedArray()
}

private val Manifest.targetLabel: BzlLabel?
  get() = KtManifest(this).targetLabel
