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
package com.google.j2cl.transpiler.frontend.kotlin

import com.google.j2cl.common.Problems
import com.google.j2cl.common.Problems.FatalError
import com.google.j2cl.common.SourceUtils.FileInfo
import com.google.j2cl.transpiler.frontend.common.FrontendOptions
import com.google.j2cl.transpiler.frontend.common.PackageInfoCache
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

/** Populates the cache from the annotations in package-info sources. */
internal fun PackageInfoCache.populateFromSources(options: FrontendOptions, problems: Problems) {
  val packageInfoFiles = options.sources.filter { it.originalPath().endsWith("package-info.java") }
  for (packageInfoFile in packageInfoFiles) {
    val content = readSource(packageInfoFile, problems)
    val jsNamespace = extractJsNamespace(content)
    if (jsNamespace != null) {
      val packageName = extractPackageName(content)
      setPackageProperties(packageName, jsNamespace, null, false)
    }
  }
}

private fun readSource(file: FileInfo, problems: Problems): String {
  try {
    return File(file.sourcePath()).readText()
  } catch (e: IOException) {
    problems.fatal(FatalError.CANNOT_OPEN_FILE, e.message)
    error("impossible")
  }
}

private val JS_PACKAGE_NAMESPACE_PATTERN =
  Pattern.compile(
    """^\s*@((?:jsinterop\.annotations\.)?JsPackage)\s*\(\s*namespace\s*=\s*"([^"]*)"\s*\)""",
    Pattern.MULTILINE,
  )

private fun extractJsNamespace(content: String): String? {
  val matcher = JS_PACKAGE_NAMESPACE_PATTERN.matcher(content)
  if (!matcher.find()) {
    return null
  }
  // If short name is used, make sure that it is properly imported.
  // Note that there could be also wildcard import but that's against to the style guide.
  if (matcher.group(1) == "JsPackage" && "import jsinterop.annotations.JsPackage" !in content) {
    return null
  }

  return matcher.group(2)
}

private val PACKAGE_PATTERN = Pattern.compile("^package ([\\w.]+)", Pattern.MULTILINE)

private fun extractPackageName(content: String): String {
  val matcher = PACKAGE_PATTERN.matcher(content)
  check(matcher.find())
  return matcher.group(1)
}
