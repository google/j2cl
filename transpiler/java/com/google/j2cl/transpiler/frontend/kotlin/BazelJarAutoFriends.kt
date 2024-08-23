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
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarInputStream
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager

internal fun ModuleVisibilityManager.addEligibleFriends(configuration: CompilerConfiguration) {
  val friendPaths = configuration.getList(JVMConfigurationKeys.FRIEND_PATHS)
  for (path in friendPaths) {
    this.addFriendPath(path)
  }
}

internal fun K2JVMCompilerArguments.setEligibleFriends(currentTarget: String?) {
  if (currentTarget == null) return

  val currentLabel = BzlLabel.parseOrThrow(currentTarget)

  this.friendPaths =
    this.classpath
      .orEmpty()
      .split(":")
      .filter {
        val depPath = Path.of(it)
        val depLabel = useManifestFast(depPath) { it.targetLabel } ?: return@filter false
        AutoFriends.isEligibleFriend(currentLabel, depLabel)
      }
      .toTypedArray()
}

private fun <T> useManifestFast(path: Path, block: (KtManifest) -> T): T? {
  return JarInputStream(BufferedInputStream(Files.newInputStream(path)), /* verify= */ false).use {
    jar ->
    jar.manifest?.let { block(KtManifest(it)) }
  }
}
