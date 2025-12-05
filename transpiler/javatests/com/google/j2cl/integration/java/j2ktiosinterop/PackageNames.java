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
package j2ktiosinterop;

import j2ktiosinterop.dep.CustomFromDep;
import j2ktiosinterop.dep.DefaultFromDep;
import j2ktiosinterop.src.CustomFromSrc;
import j2ktiosinterop.src.DefaultFromSrc;

public final class PackageNames {
  public void method(DefaultFromSrc c) {}

  public void method(CustomFromSrc c) {}

  public void method(DefaultFromDep c) {}

  public void method(CustomFromDep c) {}

  public static void staticMethod(DefaultFromSrc c) {}

  public static void staticMethod(CustomFromSrc c) {}

  public static void staticMethod(DefaultFromDep c) {}

  public static void staticMethod(CustomFromDep c) {}
}
