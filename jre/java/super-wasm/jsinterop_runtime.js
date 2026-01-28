// Copyright 2026 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

goog.module('j2wasm.JsInteropRuntime');

/**
 * Creates a proxy for the given exported constructor that intercepts
 * instantiation and static calls.
 *
 * @param {string} id The ID of the exported constructor.
 * @return {!Object} The proxy.
 */
function constructorProxy(id) {
  return new Proxy(
      function() {},
      {
        construct(target, args) {
          return new globalThis.j2wasmJsConstructors[id](...args);
        },
        get(target, property, receiver) {
          return Reflect.get(globalThis.j2wasmJsConstructors[id], property, receiver);
        },
        set(target, property, value, receiver) {
          return Reflect.set(globalThis.j2wasmJsConstructors[id], property, value, receiver);
        },
      });
}

exports = {
  constructorProxy,
};
