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

const Reflect = goog.require('goog.reflect');

/**
 * // TODO(b/79389970): change classConstructor to Function.
 * @param {Object} classConstructor
 * @param {number=} opt_dimensionCount
 * @return {Class}
 * @public
 */
Class.$get = function(classConstructor, opt_dimensionCount) {
  let dimensionCount = opt_dimensionCount || 0;
  return Reflect.cache(
      classConstructor.prototype, '$$class/' + dimensionCount, function() {
        return new Class(
            /** @type {Function} */ (classConstructor), dimensionCount);
      });
};
