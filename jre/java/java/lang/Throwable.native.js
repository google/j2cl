/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
let $Arrays = goog.require('vmbootstrap.Arrays$impl');

/**
 * @return {Throwable}
 * @public
 */
Throwable.prototype.m_fillInStackTrace = function() {
  // TODO(dankurka): Implement properly
  this.f_stackTrace__java_lang_Throwable_ =
      $Arrays.$create(0, StackTraceElement);
  return this;
};


/**
 * @param {Throwable} t
 * @return {Array<StackTraceElement>}
 * @private
 */
Throwable
    .m_constructJavaStackTrace__java_lang_Throwable_$p_java_lang_Throwable =
    function(t) {
  // TODO(dankurka): Implement properly
  return $Arrays.$create(0, StackTraceElement);
};
