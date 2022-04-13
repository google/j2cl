// Copyright 2021 Google Inc.
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

goog.module('j2wasm');


/**
 * Instantiates a web assembly module passing the necessary imports and any
 * additional import the user might need to provide for their application.
 *
 * @param {string|!Promise<!Response>} urlOrResponse
 * @param {?Object<string, !Function>=} userImports
 * @return {!Promise<!WebAssembly.Instance>}
 */
async function instantiateStreaming(urlOrResponse, userImports) {
  const response =
      typeof urlOrResponse == 'string' ? fetch(urlOrResponse) : urlOrResponse;
  const {instance} = await WebAssembly.instantiateStreaming(
      response, createImportObject(userImports));
  return instance;
}

/**
 * Instantiates a web assembly module passing the necessary imports and any
 * additional import the user might need to provide for their application.
 *
 * Use of this function is discouraged. Many browsers require when calling the
 * WebAssembly constructor that the number of bytes of the module is under a
 * small threshold, mandating the async functions for all non-trivial apps. This
 * function can be used in other contexts, such as the D8 command line.
 *
 * @param {!BufferSource} moduleObject
 * @param {?Object<string, !Function>=} userImports
 * @return {!WebAssembly.Instance}
 */
function instantiateBlocking(moduleObject, userImports) {
  return new WebAssembly.Instance(
      new WebAssembly.Module(moduleObject), createImportObject(userImports));
}

/**
 * @param {?Object<string, !Function>=} userImports
 * @return {!Object<!Object>} Wasm import object
 */
function createImportObject(userImports) {
  const jreImports = {
    'Math.acos': Math.acos,
    'Math.asin': Math.asin,
    'Math.atan': Math.atan,
    'Math.atan2': Math.atan2,
    'Math.cbrt': Math.cbrt,
    'Math.cos': Math.cos,
    'Math.cosh': Math.cosh,
    'Math.exp': Math.exp,
    'Math.expm1': Math.expm1,
    'Math.hypot': Math.hypot,
    'Math.log': Math.log,
    'Math.log10': Math.log10,
    'Math.log1p': Math.log1p,
    'Math.pow': Math.pow,
    'Math.random': Math.random,
    'Math.round': Math.round,
    'Math.sign': Math.sign,
    'Math.sin': Math.sin,
    'Math.sinh': Math.sinh,
    'Math.tan': Math.tan,
    'Math.tanh': Math.tanh,
    'Date.now': Date.now,
    'Character.toLowerCase': charToLowerCase,
    'Character.toUpperCase': charToUpperCase,
    'ConsoleLogger.log': (level, message) => console[level](message),
    'isValidDouble': isValidDouble,
    'parseFloat': parseFloat,
    'performance.now': () => performance.now(),

    // Utilites to interop strings and arrays. From String.java.
    'getLength': s => s.length,
    'getCharAt': (s, i) => s.charAt(i),
    // TODO(b/193532287): These will be removed after Array interop support in
    // WASM is implemented.
    'createBuffer': size => new Array(size),
    'setBufferAt': (buffer, index, value) => buffer[index] = value,
    'getBufferAt': (buffer, index) => buffer[index],
    'bufferToString': buffer => String.fromCharCode.apply(null, buffer),

    // The following are declared in the JRE but unimplemented for now.
    'Date.UTC': unimplemented,
    'Date.parse': unimplemented,
    'Error.captureStackTrace': unimplemented,
    'Object.getPrototypeOf': unimplemented,
    'Object.keys': unimplemented,
    'Object.values': unimplemented,
    'parseInt': unimplemented,
    'typeof': unimplemented,
  };

  return {
    // Pass the imports required by the jre plus the additional provided by the
    // user. The user imports will override the jre imports if they provide
    // the same key.
    'imports': Object.assign({}, jreImports, userImports)
  };
}

/**
 * @param {number} value
 * @return {number}
 */
function charToLowerCase(value) {
  return String.fromCharCode(value).toLowerCase().charCodeAt(0);
}

/**
 * @param {number} value
 * @return {number}
 */
function charToUpperCase(value) {
  return String.fromCharCode(value).toUpperCase().charCodeAt(0);
}

/**
 * @param {string} str
 * @return {boolean} Whether the argument is a valid Java double parsing input.
 */
function isValidDouble(str) {
  return /^\s*[+-]?(NaN|Infinity|((\d+\.?\d*)|(\.\d+))([eE][+-]?\d+)?[dDfF]?)\s*$/
      .test(str);
}

/** @return {void} */
function unimplemented() {
  throw new Error('Unimplemented extern');
}

exports = {
  instantiateStreaming,
  instantiateBlocking,
};
