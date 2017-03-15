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

// Trust that Arrays.impl.js has already been imported.

/**
 * @template M_T
 * @param {*} array
 * @param {Array<M_T>} referenceType
 * @return {Array<M_T>}
 * @public
 */
ArrayStamper.m_stampJavaTypeInfo__java_lang_Object__arrayOf_java_lang_Object =
    function(array, referenceType) {
  ArrayStamper.$clinit();
  var cast_array = /** @type {Array<*>} */ (array);
  $Arrays.$copyType(cast_array, referenceType);
  return cast_array;
};
