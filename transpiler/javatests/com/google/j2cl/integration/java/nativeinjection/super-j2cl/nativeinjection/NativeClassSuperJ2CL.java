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
package nativeinjection;

import jsinterop.annotations.JsMethod;

/**
 * The class has several special properties; The directory has 'super-j2cl' in directory name and
 * and rest of the path follows package name. 'super-j2cl' should be considered as the root
 * directory and the class and the native file should match.
 */
public class NativeClassSuperJ2CL {
  @JsMethod
  public static native String nativeStaticMethod();
}
