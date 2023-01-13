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

import com.google.j2cl.transpiler.backend.kotlin.source.source

val objcImport
  get() = systemImport("objc/objc.h")

val nsObjCRuntimeImport
  get() = systemImport("Foundation/NSObjCRuntime.h")

val nsObjectImport
  get() = systemImport("Foundation/NSObject.h")

val nsValueImport
  get() = systemImport("Foundation/NSValue.h")

val nsStringImport
  get() = systemImport("Foundation/NSString.h")

val nsEnumRendering
  get() = source("NS_ENUM") renderingWith dependency(nsObjCRuntimeImport)

val foundationExportRendering
  get() = source("FOUNDATION_EXPORT") renderingWith dependency(nsObjCRuntimeImport)

val idRendering
  get() = source("id") renderingWith dependency(objcImport)

val nsUIntegerRendering
  get() = source("NSUInteger") renderingWith dependency(nsObjCRuntimeImport)

val nsCopyingRendering
  get() = source("NSCopying") renderingWith dependency(nsObjectImport)

val nsObjectRendering
  get() = source("NSObject") renderingWith dependency(nsObjectImport)

val nsNumberRendering
  get() = source("NSNumber") renderingWith dependency(nsValueImport)

val nsStringRendering
  get() = source("NSString") renderingWith dependency(nsStringImport)

val classRendering
  get() = source("Class") renderingWith dependency(objcImport)
