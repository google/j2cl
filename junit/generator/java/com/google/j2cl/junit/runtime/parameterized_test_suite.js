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

/** @record */
class TestCaseWrapper {
  setUpPage() {}

  beforeParam() {}

  /** @return {?Thenable|void} */
  setUp() {}

  /** @return {?Thenable|void} */
  tearDown() {}

  afterParam() {}

  tearDownPage() {}

  /** @return {?Array<*>} */
  getData() {}

  /**
   * @param {number} index the index of parameter values to updated before each
   *     test case execution
   */
  setCurrentParameterValueIndex(index) {}

  /** @return {?string} */
  getParameterizedNameTemplate() {}

  /** @return {?string} */
  toString() {}

  /**
   * @param {number} index
   * @param {number} testCaseIndex
   * @return {?Object}
   */
  getParam(index, testCaseIndex) {}
}


/**
 * Expands the javaWrapper for a parameterized test into a nested test object
 * that can be consumed as a test suite.
 * @param {!TestCaseWrapper} javaWrapper
 * @return {!Object} the nested testing object
 */
function expandParameterized(javaWrapper) {
  let currTestNumber = 0;
  // Using quotes on method names here since export_test_functions doesn't
  // handle function declaration in object literals.
  const generatedSuite = {
    'setUpPage': function() {
      javaWrapper.setUpPage();
    },

    'tearDownPage': function() {
      javaWrapper.tearDownPage();
    },

    'setUp': function() {
      if (currTestNumber++ == 0) {
        javaWrapper.beforeParam();
      }
    },

    'tearDown': function() {
      // AfterParam will be executed after all tests are evaluted with
      // particular parameters
      if (currTestNumber == methods.length) {
        javaWrapper.afterParam();
        currTestNumber = 0;
      }
    },
    'toString': function() {
      return javaWrapper.toString();
    },
  };

  const jsUnitAdapter = /** @type {!Object<!Function>} */ (javaWrapper);
  const methods =
      Object.getOwnPropertyNames(assert(Object.getPrototypeOf(jsUnitAdapter)))
          .filter(eachProperty => eachProperty.startsWith('test'));
  const data = assert(javaWrapper.getData());
  for (let i = 0; i < data.length; i++) {
    const testCases = createTestCases(i, javaWrapper, methods);
    generatedSuite[testCases.toString()] = testCases;
  }

  return generatedSuite;
}

function /** !Object */ createTestCases(
    /** number */ currentIndex,
    /** !TestCaseWrapper */ javaWrapper,
    /** !Array<string>*/ methods) {
  const parameterizedTests = {
    setUp() {
      // configure the actual test instance
      javaWrapper.setCurrentParameterValueIndex(currentIndex);
      return javaWrapper.setUp();
    },
    tearDown() {
      return javaWrapper.tearDown();
    },
    toString() {
      return `testGroup${currentIndex}`;
    },
  };

  methods.forEach(method => {
    const testName = getName(method, currentIndex, javaWrapper);
    parameterizedTests[testName] = () =>
        /** @type {!Object<!Function>} */ (javaWrapper)[method]();
  });

  return parameterizedTests;
}

function /** string */ getName(
    /** string */ method, /** number */ index,
    /** !TestCaseWrapper */ javaWrapper) {
  const nameTemplate = javaWrapper.getParameterizedNameTemplate();
  if (nameTemplate === '') {
    return `test${method}[${index}]`;
  }
  let convertedName = nameTemplate.replace(/{index}/g, `${index}`);
  convertedName = convertedName.replace(
      /{([0-9]+)}/g,
      (match, paramIndex) =>
          javaWrapper.getParam(paramIndex, index) === undefined ?
          match :
          javaWrapper.getParam(paramIndex, index));
  return `test${method}[${convertedName}]`;
}

/**
 * @template T
 * @param {T} condition The condition to check.
 * @return {T} The value of the condition.
 * @closurePrimitive {asserts.truthy}
 */
function assert(condition) {
  if (!condition) {
    throw new Error("Assertion failed");
  }
  return condition;
}

exports = {expandParameterized};
