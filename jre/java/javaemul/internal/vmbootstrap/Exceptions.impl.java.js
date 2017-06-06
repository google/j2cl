// Copyright 2017 Google Inc.
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

/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Exceptions$impl');

let AutoClosable = goog.require('java.lang.AutoCloseable$impl');
let Throwable = goog.require('java.lang.Throwable$impl');

/**
 * Provides helper methods.
 */
class Exceptions {
  /**
   * A try with resource block uses safeClose to close resources that have been
   * opened.  If an exception occurred during the resource initialization
   * or in the try block, it is passed in here so that we can add suppressed
   * exceptions to it if the resource fails to close.
   *
   * @param {AutoClosable} resource
   * @param {Throwable} currentException
   * @return {Throwable}
   */
  static safeClose(resource, currentException) {
    if (resource == null) {
      return currentException;
    }
    try {
      resource.m_close__();
    } catch (e) {
      e = Exceptions.toJava(e);
      if (currentException == null) {
        return e;
      }
      currentException.m_addSuppressed__java_lang_Throwable(e);
    }
    return currentException;
  }

  /**
   * @param {*} e
   * @return {Throwable}
   */
  static toJava(e) {
    return Throwable.of(e);
  }

  /**
   * @param {Throwable} t
   * @return {*}
   */
  static toJs(t) {
    return /** @type {Error} */ (t.backingJsObject);
  }
}


/**
 * Exported class.
 */
exports = Exceptions;
