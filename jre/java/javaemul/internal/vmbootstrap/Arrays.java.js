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
 * @suppress {extraRequire, lateProvide, unusedLocalVariables, lintChecks}
 */
goog.module('vmbootstrap.Arrays');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
const _Class = goog.require('java.lang.Class');
const _Constructor = goog.require('javaemul.internal.Constructor');
const _Integer = goog.require('java.lang.Integer');
const _InternalPreconditions = goog.require('javaemul.internal.InternalPreconditions');
const _Object = goog.require('java.lang.Object');
const _Objects = goog.require('vmbootstrap.Objects');
const _Util = goog.require('nativebootstrap.Util');
// Re-exports the implementation.
const Arrays = goog.require('vmbootstrap.Arrays$impl');
exports = Arrays;
