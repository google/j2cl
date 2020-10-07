/*
 * Copyright 2019 Google Inc.
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
package typeannotations;

import java.util.List;
import java.util.Map;
import jsinterop.annotations.JsNonNull;

/** A parametric type with @JsNonNull annotations. */
interface ParametricType<T> {
  T m(@JsNonNull String s, T t);

  List<T> m(List<@JsNonNull String> l, T t);

  default @JsNonNull List<T> m(
      @JsNonNull
          List<
                  @JsNonNull Map<
                      @JsNonNull String @JsNonNull [],
                      Map<@JsNonNull List<@JsNonNull String>[], String @JsNonNull []>>>
              l) {
    return null;
  }
}
