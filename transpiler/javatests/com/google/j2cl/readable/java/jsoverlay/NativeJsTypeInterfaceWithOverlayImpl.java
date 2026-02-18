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
package jsoverlay;

// NOTE: This is super-sourced for Wasm.
public class NativeJsTypeInterfaceWithOverlayImpl implements Main.NativeJsTypeInterfaceWithOverlay {
  public int m() {
    return 0;
  }

  public void testOverlayInterfaceImpl() {
    var foo = new NativeJsTypeInterfaceWithOverlayImpl();
    foo.m();
    foo.callM();
  }
}
