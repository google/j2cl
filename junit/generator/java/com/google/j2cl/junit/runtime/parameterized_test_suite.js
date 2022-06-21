// Copyright 2022 Google Inc.
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

goog.module('com.google.j2cl.junit.parameterizedTestSuite');
goog.setTestOnly();

const TestCase = goog.require('goog.testing.TestCase');
const testSuite = goog.require('goog.testing.testSuite');
const {assert} = goog.require('goog.asserts');

/** @record */
class TestCaseWrapper {
  setUpPage() {}

  /** @return {?Thenable|void} */
  setUp() {}

  /** @return {?Thenable|void} */
  tearDown() {}

  tearDownPage() {}

  /** @return {?Array<*>} */
  getData() {}

  /**
   * @param {number} index the index of parameter values to updated before each
   *     test case execution
   */
  setCurrentParameterValueIndex(index) {}

  /** @return {?string} */
  toString() {}
}

/**
 * A drop-in replacement for goog.testing.testSuite that supports parameterized
 * tests
 * @param {!TestCaseWrapper} javaWrapper
 * @param {{order: (!TestCase.Order|undefined)}=} options
 */
function parameterizedTestSuite(javaWrapper, options) {
  testSuite(parameterizedTestHelper(javaWrapper), options);
}

/**
 * Takes a javaWrapper object to create a nested testing object.
 * @param {!TestCaseWrapper} javaWrapper
 * @return {!Object} the nested testing object
 */
function parameterizedTestHelper(javaWrapper) {
  // Using quotes on method names here since export_test_functions doesn't
  // handle function declaration in object literals.
  const generatedSuite = {
    'setUpPage': function() {
      javaWrapper.setUpPage();
    },

    'tearDownPage': function() {
      javaWrapper.tearDownPage();
    },
  };

  const jsUnitAdapter = /** @type {!Object<!Function>} */ (javaWrapper);
  const methods =
      Object.getOwnPropertyNames(assert(Object.getPrototypeOf(jsUnitAdapter)))
          .filter(eachProperty => eachProperty.startsWith('test'));
  for (let i = 0; i < javaWrapper.getData().length; i++) {
    generatedSuite[`testGroup${i}`] = createTestCases(i, javaWrapper, methods);
  }

  return generatedSuite;
}

function /** !Object */ createTestCases(
    /** number */ currentIndex,
    /** !TestCaseWrapper */ javaWrapper, /** !Array<string>*/ methods) {
  const parameterizedTests = {
    setUp() {
      // configure the actual test instance
      javaWrapper.setCurrentParameterValueIndex(currentIndex);
      return javaWrapper.setUp();
    },
    tearDown() {
      return javaWrapper.tearDown();
    },
  };
  methods.forEach(method => {
    parameterizedTests[getName(method, currentIndex)] = () =>
        /** @type {!Object<!Function>} */ (javaWrapper)[method]();
  });

  return parameterizedTests;
}

function /** string */ getName(/** string */ method, /** number */ index) {
  // TODO(b/232465613): add support for customized name
  return `test${method}[${index}]`;
}

exports = {parameterizedTestSuite};
