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
 * @fileoverview Header hand rolled.
 *
 * @suppress {lateProvide}
 */
goog.module('vmbootstrap.Comparables');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Boolean = goog.require('java.lang.Boolean');
let _Class = goog.require('java.lang.Class');
let _Comparable = goog.require('java.lang.Comparable');
let _Double = goog.require('java.lang.Double');
let _String = goog.require('java.lang.String');
let _$boolean = goog.require('vmbootstrap.primitives.$boolean');
let _$double = goog.require('vmbootstrap.primitives.$double');

// Re-exports the implementation.
let Comparables = goog.require('vmbootstrap.Comparables$impl');
exports = Comparables;
