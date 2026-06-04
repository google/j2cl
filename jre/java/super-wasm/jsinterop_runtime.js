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
 * @return {T} The proxy.
 * @template T
 */
function constructorProxy(id) {
  const proxy = new Proxy(
      function() {},
      {
        construct(target, args, newTarget) {
          if (newTarget !== proxy) {
            throw new TypeError('WASM types cannot be subtyped');
          }
          return new globalThis.j2wasmJsConstructors[id](...args);
        },
        get(target, property, receiver) {
          return Reflect.get(globalThis.j2wasmJsConstructors[id], property, receiver);
        },
        set(target, property, value, receiver) {
          return Reflect.set(globalThis.j2wasmJsConstructors[id], property, value, receiver);
        },
      });
  return proxy;
}

/**
 * To be called in Wasm to invoke a JS function, for example when receiving
 * a function from JS.
 *
 * @param {function(...?): ?} fn
 * @param {...?} args
 * @return {?}
 */
function invokeJsFunction(fn, ...args) {
  return fn(...args);
}

/**
 * To be called by Wasm to expose a function to JS.
 *
 * @param {function(!Object, ...?): ?} fn
 * @param {!Object} adapter
 * @return {function(...?): ?}
 */
function bindJsFunction(fn, adapter) {
  const f = fn.bind(adapter);
  f.adapter = adapter;
  return f;
}

/**
 * To be called by Wasm when receiving a function from JS.
 *
 * @param {function(...?): ?} fn
 * @param {function(function(...?): ?): !Object} createAdapter
 * @return {!Object}
 */
function adaptJsFunction(fn, createAdapter) {
  if (!fn.adapter) {
    fn.adapter = createAdapter(fn);
  }
  return fn.adapter;
}

exports = {
  adaptJsFunction,
  bindJsFunction,
  constructorProxy,
  invokeJsFunction,
};
