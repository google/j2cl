/*
 * Copyright 2026 Google Inc.
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
package kotlinjavainterop;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
public record JsInteropRecord(
    @JsProperty(name = "customX") int x, String y, @JsIgnore String ignored) {}

@JsType
final record JsInteropRecordExplicitMembers(
    @JsProperty(name = "customX") int x, String y, @JsIgnore String ignored) {

  public JsInteropRecordExplicitMembers(int x, String y, String ignored) {
    this.x = x;
    this.y = y;
    this.ignored = ignored;
  }

  @Override
  public int x() {
    return x;
  }

  @Override
  public String y() {
    return y;
  }

  @Override
  public String ignored() {
    return ignored;
  }
}
