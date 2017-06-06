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
 * @public
 */
Throwable.prototype.m_captureStackTrace___$p_java_lang_Throwable = function() {
  // Only supporting modern browsers so generating stack by traversing callees
  // is not necessary.
};

/**
 * @return {Array<StackTraceElement>}
 * @public
 */
Throwable.prototype.m_constructJavaStackTrace___$p_java_lang_Throwable =
    function() {
  var stackTraceElements = $Arrays.$create([0], StackTraceElement);
  var e = this.backingJsObject;
  var splitStack = (e && e.stack) ? e.stack.split(/\n/) : [];
  for (var i = 0; i < splitStack.length; i++) {
    var createSte = StackTraceElement.
        $create__java_lang_String__java_lang_String__java_lang_String__int;
    stackTraceElements[i] = createSte('', splitStack[i], null, -1);
  }
  return stackTraceElements;
};

/**
 * @param {*} e
 * @return {*}
 * @public
 */
Throwable.m_fixIE__java_lang_Object_$p_java_lang_Throwable = function(e) {
  if (!('stack' in e)) {
    try { throw e; } catch (ignored) {}
  }
  return e;
};
