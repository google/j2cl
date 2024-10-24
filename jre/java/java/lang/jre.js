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

goog.provide('jre');

/** @private @const {!Object<string, ?string>} */
jre.systemProperties_ = Object.create(null);

/**
 * Adds a system property connected to goog.define.
 * Properties being registered must have a name that matches the goog.define
 * name and the value must be the result of the goog.define call. These values
 * will then be accessible via System.getProperty in Java.
 * @param {string} googDefineName
 * @param {?string} googDefineValue
 */
jre.addSystemPropertyFromGoogDefine = function(
    googDefineName, googDefineValue) {
  // Values from goog.define are well-known to the compiler and will be
  // statically substituted. Therefore this method can be a no-op when compiled.
  if (!COMPILED) {
    if (googDefineName in jre.systemProperties_) {
      throw new Error(
          `Attempting to redeclare system property '${googDefineName}'.`);
    }
    jre.systemProperties_[googDefineName] = googDefineValue;
  }
};

/**
 * Returns the value of a system property.
 * @param {string} name
 * @param {?string=} defaultValue
 * @return {?string}
 */
jre.getSystemProperty = function(name, defaultValue = null) {
  // For backwards compatibility, fallback to goog.getObjectByName if it's not
  // in the registry.
  const rv = jre.systemProperties_[name] ?? goog.getObjectByName(name);
  return rv == null ? defaultValue : String(rv);
};

// Add core defines
jre.addSystemPropertyFromGoogDefine('COMPILED', String(COMPILED));
jre.addSystemPropertyFromGoogDefine('goog.DEBUG', String(goog.DEBUG));
jre.addSystemPropertyFromGoogDefine('goog.LOCALE', goog.LOCALE);
jre.addSystemPropertyFromGoogDefine(
    'goog.TRUSTED_SITE', String(goog.TRUSTED_SITE));
jre.addSystemPropertyFromGoogDefine(
    'goog.FEATURESET_YEAR', String(goog.FEATURESET_YEAR));

/** @define {string} */
jre.classMetadata = goog.define('jre.classMetadata', 'SIMPLE');
jre.addSystemPropertyFromGoogDefine('jre.classMetadata', jre.classMetadata);

/** @define {string} */
jre.checkedMode =
    goog.define('jre.checkedMode', goog.DEBUG ? 'ENABLED' : 'DISABLED');
jre.addSystemPropertyFromGoogDefine('jre.checkedMode', jre.checkedMode);


goog.provide('jre.checks');

/** @define {string} */
jre.checks.checkLevel = goog.define('jre.checks.checkLevel', 'NORMAL');
jre.addSystemPropertyFromGoogDefine(
    'jre.checks.checkLevel', jre.checks.checkLevel);

/** @define {string} */
jre.checks.bounds = goog.define('jre.checks.bounds', 'AUTO');
jre.addSystemPropertyFromGoogDefine('jre.checks.bounds', jre.checks.bounds);


/** @define {string} */
jre.checks.api = goog.define('jre.checks.api', 'AUTO');
jre.addSystemPropertyFromGoogDefine('jre.checks.api', jre.checks.api);

/** @define {string} */
jre.checks.numeric = goog.define('jre.checks.numeric', 'AUTO');
jre.addSystemPropertyFromGoogDefine('jre.checks.numeric', jre.checks.numeric);

/** @define {string} */
jre.checks.type = goog.define('jre.checks.type', 'AUTO');
jre.addSystemPropertyFromGoogDefine('jre.checks.type', jre.checks.type);

/** @define {string} */
jre.checks.critical = goog.define('jre.checks.critical', 'AUTO');
jre.addSystemPropertyFromGoogDefine('jre.checks.critical', jre.checks.critical);


goog.provide('jre.logging');

/** @define {string} */
jre.logging.logLevel =
    goog.define('jre.logging.logLevel', goog.DEBUG ? 'ALL' : 'SEVERE');
jre.addSystemPropertyFromGoogDefine(
    'jre.logging.logLevel', jre.logging.logLevel);

/** @define {string} */
jre.logging.simpleConsoleHandler =
    goog.define('jre.logging.simpleConsoleHandler', 'ENABLED');
jre.addSystemPropertyFromGoogDefine(
    'jre.logging.simpleConsoleHandler', jre.logging.simpleConsoleHandler);

// Provide a stub for field preserver to make things work without full
// compilation.
if (!COMPILED || goog.DEBUG) {
  globalThis.$J2CL_PRESERVE$ = function(/** ...* */ e) {};
}
