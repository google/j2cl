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
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager

internal object BazelJarAutoFriends {

  internal fun ModuleVisibilityManager.addEligibleFriends(
    currentTarget: String?,
    classPath: List<String>,
  ) {
    if (currentTarget == null) return
    val currentLabel = BzlLabel.parseOrThrow(currentTarget)
    loadLabels(classPath.map(Path::of)).forEach { (depPath, depLabel) ->
      if (depLabel != null && AutoFriends.isEligibleFriend(currentLabel, depLabel)) {
        addFriendPath(depPath.toString())
      }
    }
  }

  private fun loadLabels(classpath: List<Path>): Map<Path, BzlLabel?> {
    return classpath.associateWith { path -> useManifestFast(path) { it.targetLabel } }
  }

  private fun <T> useManifestFast(path: Path, block: (KtManifest) -> T): T? {
    return JarInputStream(BufferedInputStream(Files.newInputStream(path)), /* verify= */ false)
      .use { jar -> jar.manifest?.let { block(KtManifest(it)) } }
  }
}
