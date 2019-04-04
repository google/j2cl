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
goog.module('vmbootstrap.Numbers$impl');

const $Long = goog.require('nativebootstrap.Long$impl');

let Double = goog.forwardDeclare('java.lang.Double$impl');
let Number = goog.forwardDeclare('java.lang.Number$impl');


/**
 * Provides devirtualized method implementations for Numbers.
 */
class Numbers {
  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_byteValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_byteValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_byteValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_doubleValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_doubleValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_doubleValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_floatValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_floatValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_floatValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_intValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_intValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_intValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {!$Long}
   * @public
   */
  static m_longValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_longValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_longValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_shortValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_shortValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_shortValue__();
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Numbers.$clinit = function() {};
    Double = goog.module.get('java.lang.Double$impl');
    Number = goog.module.get('java.lang.Number$impl');
  }
}



/**
 * Exported class.
 */
exports = Numbers;
