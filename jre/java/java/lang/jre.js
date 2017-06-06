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
 * This file provides the @defines for JRE configuration options.
 * See InternalPreconditions.java for details.
 */

goog.provide('jre');

/** @define {string} */
goog.define('jre.classMetadata', 'SIMPLE');
/** @define {string} */
goog.define('jre.checkedMode', goog.DEBUG ? 'ENABLED' : 'DISABLED');


goog.provide('jre.checks');

/** @define {string} */
goog.define('jre.checks.checkLevel', 'NORMAL');
/** @define {string} */
goog.define('jre.checks.bounds', 'AUTO');
/** @define {string} */
goog.define('jre.checks.api', 'AUTO');
/** @define {string} */
goog.define('jre.checks.numeric', 'AUTO');
/** @define {string} */
goog.define('jre.checks.type', 'AUTO');
/** @define {string} */
goog.define('jre.checks.critical', 'AUTO');

// TODO(b/33100017): Remove once bug is fixed.
/** @define {boolean} */
goog.define('jre.checks.temporaryEarlyBailOutFromCastCheck', false);

goog.provide('jre.logging');

/** @define {string} */
goog.define('jre.logging.logLevel', goog.DEBUG ? 'ALL' : 'SEVERE');
/** @define {string} */
goog.define('jre.logging.simpleConsoleHandler', "ENABLED");
