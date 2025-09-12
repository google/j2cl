// Copyright 2023 Google Inc.
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

goog.module('j2wasm.ExceptionUtils');

/**
 * @param {!Error} e
 */
function throwException(e) {
  throw e;
}

/**
 * @param {?} error
 * @param {?} throwable
 * @public
 */
function setJavaThrowable(error, throwable) {
  if (error instanceof Object) {
    try {
      // This may throw exception (e.g. frozen object) in strict mode.
      error["__j2wasm$exception"] = throwable;
    } catch (ignored) {}
  }
}

/**
 * @param {?} error
 * @return {?} throwable
 * @public
 */
function getJavaThrowable(error) {
  // Note that we use a different name for the property to avoid collisions with
  // the property used by the J2CL/Closure runtime.
  return error["__j2wasm$exception"] ?? null;
}

/**
 * @param {?} obj
 * @return {boolean}
 * @public
 */
function isError(obj) {
  return obj instanceof Error;
}

exports = {
  throwException,
  setJavaThrowable,
  getJavaThrowable,
  isError,
};
