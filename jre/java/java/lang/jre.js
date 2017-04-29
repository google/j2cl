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
