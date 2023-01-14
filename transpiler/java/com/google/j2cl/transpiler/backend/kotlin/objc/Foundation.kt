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
package com.google.j2cl.transpiler.backend.kotlin.objc

import com.google.j2cl.transpiler.backend.kotlin.source.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.inCurlyBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inNewLines
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.infix
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.plusComma
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

const val foundationPath = "Foundation/Foundation.h"
val foundationImport = systemImport(foundationPath)
val foundationDependency = dependency(foundationImport)

fun foundationRendering(string: String) = source(string) renderingWith foundationDependency

val nullRendering = foundationRendering("NULL")
val noRendering = foundationRendering("NO")
val yesRendering = foundationRendering("YES")

val nsEnumRendering = foundationRendering("NS_ENUM")
val nsInlineRendering = foundationRendering("NS_INLINE")

val idRendering = foundationRendering("id")
val nsUIntegerRendering = foundationRendering("NSUInteger")
val nsCopyingRendering = foundationRendering("NSCopying")
val nsObjectRendering = foundationRendering("NSObject")
val nsNumberRendering = foundationRendering("NSNumber")
val nsStringRendering = foundationRendering("NSString")

val nsMutableArrayRendering = foundationRendering("NSMutableArray")
val nsMutableSetRendering = foundationRendering("NSMutableSet")
val nsMutableDictionaryRendering = foundationRendering("NSMutableDictionary")

val classRendering = foundationRendering("Class")

fun nsEnumTypedef(name: String, type: Rendering, values: List<String>) =
  nsEnumRendering.bindSource { nsEnumSource ->
    type.bindSource { typeSource ->
      rendering(
        semicolonEnded(
          spaceSeparated(
            source("typedef"),
            join(nsEnumSource, inRoundBrackets(commaSeparated(typeSource, source(name)))),
            inCurlyBrackets(
              inNewLines(
                values.mapIndexed { index, name ->
                  infix(source(name), "=", source("$index")).plusComma
                }
              )
            )
          )
        )
      )
    }
  }
