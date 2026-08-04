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

const Long = goog.require('goog.math.Long');

/**
 * @param {string} id
 * @return {!Function}
 */
function getWasmConstructor(id) {
  const ctors = /** @type {!Object<string, !Function>|undefined} */ (globalThis.j2wasmJsConstructors);
  if (goog.DEBUG) {
    if (!ctors || !ctors[id]) {
      throw new Error(
          `Attempted to use WASM type '${id}' before it has been loaded.`);
    }
  }
  return ctors[id];
}

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
          return new (getWasmConstructor(id))(...args);
        },
        get(target, property, receiver) {
          return Reflect.get(getWasmConstructor(id), property, receiver);
        },
        set(target, property, value, receiver) {
          return Reflect.set(getWasmConstructor(id), property, value, receiver);
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
  // `fn` is always an exported Wasm function and does not have a `this`
  // context, so we pass `null` as the `thisArg` and bind `adapter` as its
  // first argument.
  const f = fn.bind(null, adapter);
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

/**
 * @param {*} x
 * @return {number}
 */
function typeOf(x) {
  if (x == null) {
    return 0;
  }

  switch (typeof x) {
    case 'string':
      return 1;
    case 'boolean':
      return 2;
    case 'number':
      return 3;
    default:
      if (x instanceof Long) {
        return 4;
      }
      return 5;
  }
}

exports = {
  adaptJsFunction,
  bindJsFunction,
  constructorProxy,
  invokeJsFunction,
  typeOf,
};
