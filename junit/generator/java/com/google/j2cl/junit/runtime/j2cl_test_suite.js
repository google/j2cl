// Copyright 2025 Google Inc.
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
goog.module('com.google.j2cl.junit.j2clTestSuite');

goog.setTestOnly();

const TestCase = goog.require('goog.testing.TestCase');
const testSuite = goog.require('goog.testing.testSuite');
const {expandParameterized} = goog.require('com.google.j2cl.junit.parameterizedTestSuite');
const {jasmineTestConverter} = goog.require('com.google.j2cl.junit.jasmineTestConverter');

/**
 * Registers a test suite with the appropriate testing framework.
 * Either converts it for Jasmine or defines it as a closure test suite
 * with natural ordering based on the presence of `jasmine`.
 *
 * @param {?} suite The test suite object to be registered.
 * @param {boolean} isParameterized Whether the test suite is parameterized.
 * @suppress {reportUnknownTypes} Unblock stricter JsCompiler options.
 */
function j2clTestSuite(suite, isParameterized) {
  if (isParameterized) {
    suite = expandParameterized(suite);
  }
  goog.global['jasmine'] ? jasmineTestConverter(suite) : testSuite(suite, {order: TestCase.Order.NATURAL});
}

exports = {j2clTestSuite};
