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
let JsException = goog.require("java.lang.JsException");

/**
 * @param {*} wrapped
 * @return {Exception}
 * @public
 */
JsExceptionViolator.m_createJsException__java_lang_Object = function(wrapped) {
  JsExceptionViolator.$clinit();
  return JsException.$create__java_lang_Object(wrapped);
};

/**
 * @param {Throwable} throwable
 * @return {*}
 * @public
 */
JsExceptionViolator.m_getBackingJsObject__java_lang_Throwable = function(throwable) {
  JsExceptionViolator.$clinit();
  return throwable.backingJsObject;
};
