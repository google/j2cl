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

/** @define {string} */
jre.classMetadata = goog.define('jre.classMetadata', 'SIMPLE');
/** @define {string} */
jre.checkedMode =
    goog.define('jre.checkedMode', goog.DEBUG ? 'ENABLED' : 'DISABLED');


goog.provide('jre.checks');

/** @define {string} */
jre.checks.checkLevel = goog.define('jre.checks.checkLevel', 'NORMAL');
/** @define {string} */
jre.checks.bounds = goog.define('jre.checks.bounds', 'AUTO');
/** @define {string} */
jre.checks.api = goog.define('jre.checks.api', 'AUTO');
/** @define {string} */
jre.checks.numeric = goog.define('jre.checks.numeric', 'AUTO');
/** @define {string} */
jre.checks.type = goog.define('jre.checks.type', 'AUTO');
/** @define {string} */
jre.checks.critical = goog.define('jre.checks.critical', 'AUTO');


goog.provide('jre.logging');

/** @define {string} */
jre.logging.logLevel =
    goog.define('jre.logging.logLevel', goog.DEBUG ? 'ALL' : 'SEVERE');
/** @define {string} */
jre.logging.simpleConsoleHandler =
    goog.define('jre.logging.simpleConsoleHandler', 'ENABLED');

// Provide a stub for field preserver to make things work without full
// compilation.
if (!COMPILED || goog.DEBUG) {
  globalThis.$J2CL_PRESERVE$ = function(/** ...* */ e) {};
}
