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
 * Implementation hand rolled.
 */
goog.module('vmbootstrap.CharSequences$impl');

let CharSequence = goog.forwardDeclare('java.lang.CharSequence$impl');
let String = goog.forwardDeclare('java.lang.String$impl');


/**
 * Provides devirtualized method implementations for CharSequence.
 */
class CharSequences {
  /**
   * Redirect the string calls to use the devirtualized version.
   *
   * @param {CharSequence|string} obj
   * @return {number}
   * @public
   */
  static m_length__java_lang_CharSequence(obj) {
    CharSequences.$clinit();
    var type = typeof obj;
    if (type == 'string') {
      obj = /**@type {string}*/ (obj);
      return String.m_length__java_lang_String(obj);
    }
    return obj.m_length__();
  }

  /**
   * Redirect the string calls to use the devirtualized version.
   *
   * @param {CharSequence|string} obj
   * @param {number} index
   * @return {number}
   */
  static m_charAt__java_lang_CharSequence__int(obj, index) {
    CharSequences.$clinit();
    var type = typeof obj;
    if (type == 'string') {
      obj = /**@type {string}*/ (obj);
      return String.m_charAt__java_lang_String__int(obj, index);
    }
    return obj.m_charAt__int(index);
  }

  /**
   * Redirect the string calls to use the devirtualized version.
   *
   * @param {CharSequence|string} obj
   * @param {number} start
   * @param {number} end
   * @return {CharSequence|string}
   */
  static m_subSequence__java_lang_CharSequence__int__int(obj, start, end) {
    CharSequences.$clinit();
    var type = typeof obj;
    if (type == 'string') {
      obj = /**@type {string}*/ (obj);
      return String.m_subSequence__java_lang_String__int__int(obj, start, end);
    }
    return obj.m_subSequence__int__int(start, end);
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    CharSequence = goog.module.get('java.lang.CharSequence$impl');
    String = goog.module.get('java.lang.String$impl');
  }
}


/**
 * Exported class.
 */
exports = CharSequences;
