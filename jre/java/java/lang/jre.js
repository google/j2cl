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
 * This file provides the @defines for JRE configuration options. See
 * https://github.com/google/j2cl/blob/master/docs/best-practices.md#jre-configuration
 * for documentation.
 */

goog.module('jre');

/** @private @const {!Object<string, *>} */
const systemProperties = Object.create(null);

/**
 * Adds a system property connected to goog.define.
 * Properties being registered must have a name that matches the goog.define
 * name and the value must be the result of the goog.define call. These values
 * will then be accessible via System.getProperty in Java.
 * @param {string} googDefineName
 * @param {?string} googDefineValue
 */
function addSystemPropertyFromGoogDefine(googDefineName, googDefineValue) {
  // Values from goog.define are well-known to the compiler and will be
  // statically substituted. Therefore this method can be a no-op when compiled.
  if (!COMPILED) {
    const existingValue = systemProperties[googDefineName];
    if (existingValue instanceof Error) {
      throw existingValue;
    } else if (existingValue !== undefined) {
      throw new Error(
          `Attempting to redeclare system property '${googDefineName}'.`);
    }
    systemProperties[googDefineName] = googDefineValue;
  }
}

/**
 * Returns the value of a system property.
 * @param {string} name
 * @param {?string=} defaultValue
 * @return {?string}
 */
function getSystemProperty(name, defaultValue = null) {
  let rv = systemProperties[name];
  if (!COMPILED && (rv === undefined || rv instanceof Error)) {
    if (rv === undefined) {
      // Stash an error that will later be thrown if anyone tries to register
      // the property. We intentionally only stash the first occurrence as this
      // is the earliest point that the registration call needs to move before.
      systemProperties[name] = new Error(`System property "${
          name}" read before registration via jre.addSystemPropertyFromGoogDefine.`);
    }
    // Make sure fallback doesn't see a recorded Error instance.
    rv = undefined;
  }
  // For backwards compatibility, fallback to goog.getObjectByName if it's not
  // in the registry.
  rv = rv ?? goog.getObjectByName(name);
  return rv == null ? defaultValue : String(rv);
}

// Add core defines
addSystemPropertyFromGoogDefine('COMPILED', String(COMPILED));
addSystemPropertyFromGoogDefine('goog.DEBUG', String(goog.DEBUG));
addSystemPropertyFromGoogDefine('goog.LOCALE', goog.LOCALE);
addSystemPropertyFromGoogDefine(
    'goog.FEATURESET_YEAR', String(goog.FEATURESET_YEAR));

/** @define {string} */
const classMetadata = goog.define('jre.classMetadata', 'SIMPLE');
addSystemPropertyFromGoogDefine('jre.classMetadata', classMetadata);

/** @define {string} */
const checkedMode =
    goog.define('jre.checkedMode', goog.DEBUG ? 'ENABLED' : 'DISABLED');
addSystemPropertyFromGoogDefine('jre.checkedMode', checkedMode);

/** @const */
const checks = {};

/** @define {string} */
checks.checkLevel = goog.define('jre.checks.checkLevel', 'NORMAL');
addSystemPropertyFromGoogDefine('jre.checks.checkLevel', checks.checkLevel);

/** @define {string} */
checks.bounds = goog.define('jre.checks.bounds', 'AUTO');
addSystemPropertyFromGoogDefine('jre.checks.bounds', checks.bounds);


/** @define {string} */
checks.api = goog.define('jre.checks.api', 'AUTO');
addSystemPropertyFromGoogDefine('jre.checks.api', checks.api);

/** @define {string} */
checks.numeric = goog.define('jre.checks.numeric', 'AUTO');
addSystemPropertyFromGoogDefine('jre.checks.numeric', checks.numeric);

/** @define {string} */
checks.type = goog.define('jre.checks.type', 'AUTO');
addSystemPropertyFromGoogDefine('jre.checks.type', checks.type);

/** @define {string} */
checks.critical = goog.define('jre.checks.critical', 'AUTO');
addSystemPropertyFromGoogDefine('jre.checks.critical', checks.critical);

/** @const */
const logging = {};

/** @define {string} */
logging.logLevel =
    goog.define('jre.logging.logLevel', goog.DEBUG ? 'ALL' : 'SEVERE');
addSystemPropertyFromGoogDefine('jre.logging.logLevel', logging.logLevel);

/** @define {string} */
logging.simpleConsoleHandler =
    goog.define('jre.logging.simpleConsoleHandler', 'ENABLED');
addSystemPropertyFromGoogDefine(
    'jre.logging.simpleConsoleHandler', logging.simpleConsoleHandler);

// Provide a stub for field preserver to make things work without full
// compilation.
if (!COMPILED || goog.DEBUG) {
  globalThis.$J2CL_PRESERVE$ = function(/** ...* */ e) {};
}

exports = {
  addSystemPropertyFromGoogDefine,
  getSystemProperty,
  classMetadata,
  checkedMode,
  checks,
  logging,
};
