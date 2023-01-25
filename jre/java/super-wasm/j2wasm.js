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
    'Number.toPrecision': (/** number */ n, /** number */ p) =>
        n.toPrecision(p),
    'Character.codePointToLowerCase': codePointToLowerCase,
    'Character.codePointToUpperCase': codePointToUpperCase,
    'Character.charToLowerCase': charToLowerCase,
    'Character.charToUpperCase': charToUpperCase,
    'ConsoleLogger.log': (level, message) => console[level](message),
    'isValidDouble': isValidDouble,
    'parseFloat': parseFloat,
    'performance.now': () => performance.now(),

    // String
    'String.fromCodePoint': String.fromCodePoint,
    'String.fromCharCode': String.fromCharCode,
    'String.indexOf': (/** string */ s, /** string */ r, /** number */ i) =>
        s.indexOf(r, i),
    'String.lastIndexOf': (/** string */ s, /** string */ r, /** number */ i) =>
        s.lastIndexOf(r, i),
    'String.replace': (/** string */ s, /** !RegExp */ re, /** string */ r) =>
        s.replace(re, r),
    'String.toLowerCase': (/** string */ s) => s.toLowerCase(),
    'String.toUpperCase': (/** string */ s) => s.toUpperCase(),
    'String.toLocaleLowerCase': (/** string */ s) => s.toLocaleLowerCase(),
    'String.toLocaleUpperCase': (/** string */ s) => s.toLocaleUpperCase(),
    'String.compareTo': (/** string */ a, /** string */ b) =>
        a == b ? 0 : (a < b ? -1 : 1),
    'String.equalsIgnoreCase': (/** string */ a, /** string */ b) =>
        a.toLowerCase() == b.toLowerCase(),
    'String.fromNumber': (/** number */ n, /** number */ r) => n.toString(r),
    // Regex
    'RegExp.constructor': (/** string */ p, /** string */ f) =>
        new RegExp(p, f),
    'RegExp.setLastIndex': (/** !RegExp */ r, /** number */ i) => r.lastIndex =
        i,
    'RegExp.exec': (/** !RegExp */ r, /** string */ s) => r.exec(s),
    'RegExp.test': (/** !RegExp */ r, /** string */ s) => r.test(s),
    'RegExpResult.index': (/** !RegExpResult */ r) => r.index,

    // TODO(b/193532287): These will be removed after Array interop support in
    // WASM is implemented.
    'createBuffer': size => new Array(size),
    'setBufferAt': (buffer, index, value) => buffer[index] = value,
    'getBufferAt': (buffer, index) => buffer[index],
    'getLength': s => s.length,

    // Date
    'Date.now': Date.now,
    'Date.UTC':
        (/** number */ year, /** number */ month, /** number */ dayOfMonth,
         /** number */ hours, /** number */ minutes, /** number */ seconds,
         /** number */ millis) =>
            Date.UTC(year, month, dayOfMonth, hours, minutes, seconds, millis),
    'Date.parse': (/** string */ dateString) => Date.parse(dateString),
    'Date.create': (...args) => new Date(...args),
    'Date.getDate': (/** !Date */ date) => date.getDate(),
    'Date.getDay': (/** !Date */ date) => date.getDay(),
    'Date.getFullYear': (/** !Date */ date) => date.getFullYear(),
    'Date.getHours': (/** !Date */ date) => date.getHours(),
    'Date.getMilliseconds': (/** !Date */ date) => date.getMilliseconds(),
    'Date.getMinutes': (/** !Date */ date) => date.getMinutes(),
    'Date.getMonth': (/** !Date */ date) => date.getMonth(),
    'Date.getSeconds': (/** !Date */ date) => date.getSeconds(),
    'Date.getTime': (/** !Date */ date) => date.getTime(),
    'Date.getTimezoneOffset': (/** !Date */ date) => date.getTimezoneOffset(),
    'Date.getUTCDate': (/** !Date */ date) => date.getUTCDate(),
    'Date.getUTCFullYear': (/** !Date */ date) => date.getUTCFullYear(),
    'Date.getUTCHours': (/** !Date */ date) => date.getUTCHours(),
    'Date.getUTCMinutes': (/** !Date */ date) => date.getUTCMinutes(),
    'Date.getUTCMonth': (/** !Date */ date) => date.getUTCMonth(),
    'Date.getUTCSeconds': (/** !Date */ date) => date.getUTCSeconds(),
    'Date.toLocaleString': (/** !Date */ date) => date.toLocaleString(),
    'Date.setDate': (/** !Date */ date, /** number */ dayOfMonth) =>
        void date.setDate(dayOfMonth),
    'Date.setFullYear': dateSetFullYear,
    'Date.setHours': dateSetHours,
    'Date.setMinutes': (/** !Date */ date, /** number */ minutes) =>
        void date.setMinutes(minutes),
    'Date.setMonth': (/** !Date */ date, /** number */ month) =>
        void date.setMonth(month),
    'Date.setSeconds': (/** !Date */ date, /** number */ seconds) =>
        void date.setSeconds(seconds),
    'Date.setTime': (/** !Date */ date, /** number */ milliseconds) =>
        void date.setTime(milliseconds),
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
function codePointToLowerCase(value) {
  return String.fromCodePoint(value).toLowerCase().codePointAt(0);
}

/**
 * @param {number} value
 * @return {number}
 */
function codePointToUpperCase(value) {
  return String.fromCodePoint(value).toUpperCase().codePointAt(0);
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
 * @param {!Date} date
 * @param {...number} dateParts
 */
function dateSetFullYear(date, ...dateParts) {
  date.setFullYear(...dateParts);
}

/**
 * @param {!Date} date
 * @param {...number} timeParts
 */
function dateSetHours(date, ...timeParts) {
  date.setHours(...timeParts);
}

/**
 * @param {string} str
 * @return {boolean} Whether the argument is a valid Java double parsing input.
 */
function isValidDouble(str) {
  return /^\s*[+-]?(NaN|Infinity|((\d+\.?\d*)|(\.\d+))([eE][+-]?\d+)?[dDfF]?)\s*$/
      .test(str);
}

exports = {
  instantiateStreaming,
  instantiateBlocking,
};
