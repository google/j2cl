/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package deprecated;

import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

/** A set of an examples showing how @{@link Deprecated} annotations are generated in JavaScript. */
@JsType
@Deprecated
public class DeprecatedExample {
  @Deprecated public static final String DEPRECATED_STATIC_FIELD = "deprecated field";

  @Deprecated public String deprecatedInstanceField = "deprecated field";

  @Deprecated
  public static void deprecatedStaticMethod(Object someObject) {}

  @Deprecated
  public void deprecatedInstanceMethod(String someArg) {}

  @Deprecated
  public DeprecatedExample() {}

  @JsType
  @Deprecated
  interface DeprecatedInterface {
    @Deprecated
    void doAThing(int anInt);
  }

  @JsFunction
  @Deprecated
  interface DeprecatedJsFunction {
    @Deprecated
    void doAThing(Object aThing);
  }

  @JsType
  @Deprecated
  enum DeprecatedEnum {
    @Deprecated
    A_VALUE,
  }

  @JsEnum
  @Deprecated
  enum DeprecatedJsEnum {
    @Deprecated
    A_VALUE,
  }
}
