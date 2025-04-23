/*
 * Copyright 2025 Google Inc.
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

import com.google.j2cl.transpiler.frontend.common.PackageInfo
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

/**
 * A cache for information on package-info files that are needed for transpilation, like JsInterop
 * annotations.
 */
internal class PackageInfoCache(private val classpath: List<VirtualFile>) {

  private val packageInfoByName: Map<String, PackageInfo?> = buildMap {
    for (cp in classpath) {
      @Suppress("CheckReturnValue")
      VfsUtilCore.iterateChildrenRecursively(cp, null) {
        if (it.name == "package-info.class") {
          val packageInfo = PackageInfo.read(it.inputStream)
          put(packageInfo.packageName, packageInfo)
        }
        true
      }
    }
  }

  internal fun getJsNamespace(packageName: String): String? =
    packageInfoByName[packageName]?.jsNamespace
}
