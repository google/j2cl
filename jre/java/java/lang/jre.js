/**
 * This file provides the @defines for JRE configuration options.
 * See InternalPreconditions.java for details.
 */

goog.provide('jre');

/** @define {string} */
goog.define('jre.debugMode', goog.DEBUG ? 'ENABLED' : 'DISABLED');
/** @define {string} */
goog.define('jre.checkedMode', jre.debugMode);


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


// TODO(goktug): rename to jre.logging
goog.provide('gwt.logging');

/** @define {string} */
goog.define('gwt.logging.simpleConsoleHandler', "ENABLED");
/** @define {string} */
goog.define(
    'gwt.logging.enabled', jre.debugMode == 'ENABLED' ? 'TRUE' : 'SEVERE');
