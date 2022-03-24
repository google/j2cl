/*
 * Copyright 2018 Google Inc.
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
package nativeinjectionapt;

import nativeinjectionapt.apt.RunApt;
import jsinterop.annotations.JsMethod;

/**
 * The class has several special properties; directory doesn't follow package name and also
 * directory has java in directy name. It should still match with java qualified name since APT
 * would only look from that perspective not physical location.
 */
@RunApt
public class SuperMyClass {
  @JsMethod
  public static native String nativeStaticMethod();
}
