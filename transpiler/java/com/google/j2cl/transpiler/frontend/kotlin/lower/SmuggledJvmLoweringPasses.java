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
package com.google.j2cl.transpiler.frontend.kotlin.lower;

import kotlin.jvm.functions.Function1;
import org.jetbrains.kotlin.backend.common.FileLoweringPass;
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext;
import org.jetbrains.kotlin.backend.jvm.lower.ExternalPackageParentPatcherLowering;
import org.jetbrains.kotlin.backend.jvm.lower.FileClassLowering;
import org.jetbrains.kotlin.backend.jvm.lower.JvmInventNamesForLocalClasses;
import org.jetbrains.kotlin.backend.jvm.lower.JvmLateinitLowering;
import org.jetbrains.kotlin.backend.jvm.lower.JvmLocalClassPopupLowering;
import org.jetbrains.kotlin.backend.jvm.lower.JvmPropertiesLowering;
import org.jetbrains.kotlin.backend.jvm.lower.JvmReturnableBlockLowering;
import org.jetbrains.kotlin.backend.jvm.lower.StaticInitializersLowering;

/**
 * Expose factories for JVM lowering passes, side-stepping the internal visibility of them.
 *
 * <p>TODO(b/408198772): Properly expose these from kotlinc and remove this.
 */
@SuppressWarnings("KotlinInternal")
final class SmuggledJvmLoweringPasses {
  static final Function1<JvmBackendContext, FileLoweringPass> jvmLocalClassPopupLoweringFactory =
      JvmLocalClassPopupLowering::new;
  static final Function1<JvmBackendContext, FileLoweringPass>
      externalPackageParentPatcherLoweringFactory = ExternalPackageParentPatcherLowering::new;
  static final Function1<JvmBackendContext, FileLoweringPass> jvmInventNamesForLocalClassesFactory =
      JvmInventNamesForLocalClasses::new;
  static final Function1<JvmBackendContext, FileLoweringPass> staticInitializersLoweringFactory =
      StaticInitializersLowering::new;
  static final Function1<JvmBackendContext, FileLoweringPass> jvmLateinitLoweringFactory =
      JvmLateinitLowering::new;
  static final Function1<JvmBackendContext, FileLoweringPass> jvmPropertiesLoweringFactory =
      JvmPropertiesLowering::new;
  static final Function1<JvmBackendContext, FileLoweringPass> fileClassLoweringFactory =
      FileClassLowering::new;
  static final Function1<JvmBackendContext, FileLoweringPass> jvmReturnableBlockLoweringFactory =
      JvmReturnableBlockLowering::new;

  private SmuggledJvmLoweringPasses() {}
}
