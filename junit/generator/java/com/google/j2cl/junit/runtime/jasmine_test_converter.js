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


goog.module('com.google.j2cl.junit.jasmineTestConverter');
goog.setTestOnly();


/** @record */
class TestCaseWrapper {
  setUpPage() {}

  /** @return {?Thenable|void} */
  setUp() {}

  /** @return {?Thenable|void} */
  tearDown() {}

  tearDownPage() {}

  /** @return {?string} */
  toString() {}
}

/**
 * Converts and executes a Closure test suite as an equivalent Jasmine test.
 *
 * This function acts as an adapter, taking a `jsUnitAdapter` object that
 * represents a Closure test suite and orchestrating its execution within the
 * Jasmine testing framework.
 *
 * @param {!TestCaseWrapper} jsUnitAdapter
 */
function jasmineTestConverter(jsUnitAdapter) {
  goog.global['describe'](jsUnitAdapter.toString(), () => {
    if (jsUnitAdapter.setUpPage) {
      goog.global['beforeAll'](() => jsUnitAdapter.setUpPage());
    }

    goog.global['beforeEach'](() => jsUnitAdapter.setUp());

    // Discover test methods directly.
    const testMethods = getAllTestProperties(jsUnitAdapter);

    for (const methodName of testMethods) {
      const methodOrTestCase =
          /** @type {!Object<!Function>} */ (jsUnitAdapter)[methodName];
      if (typeof methodOrTestCase === 'function') {
        goog.global['it'](methodName, methodOrTestCase.bind(jsUnitAdapter));
      } else {
        jasmineTestConverter(
            /** @type {!TestCaseWrapper} */ (methodOrTestCase));
      }
    }

    goog.global['afterEach'](() => jsUnitAdapter.tearDown());

    if (jsUnitAdapter.tearDownPage) {
      goog.global['afterAll'](() => jsUnitAdapter.tearDownPage());
    }
  });
}

/**
 * Iterates through the object's own properties and then
 * traverses its prototype chain to collect all properties that match the
 * 'test' prefix.
 *
 * @param {?} obj The object from which to retrieve properties.
 * @return {!Array<string>} An array of strings, where each string is the name
 *     of a property that starts with 'test'.
 */
function getAllTestProperties(obj) {
  const props = new Set();
  do {
    for (const prop of Object.getOwnPropertyNames(obj)) {
      if (prop.startsWith('test')) {
        props.add(prop);
      }
    }
  } while (obj = Object.getPrototypeOf(obj));
  return [...props];
}

exports = {jasmineTestConverter};
