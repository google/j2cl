/**
 * This file provides the @defines for JRE configuration options.
 * See InternalPreconditions.java for details.
 */

// TODO(goktug): Remove these after all references are removed
/** @define {string} */
goog.define('ARRAY_CHECK_TYPES_', 'auto');
/** @define {string} */
goog.define('CAST_CHECKS_ENABLED_', 'auto');
/** @define {string} */
goog.define('ARITHMETIC_EXCEPTION_CHECKS_ENABLED_', 'auto');


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
goog.define(
    'jre.checks.numeric', ARITHMETIC_EXCEPTION_CHECKS_ENABLED_ == 'auto' ?
        'AUTO' :
        ARITHMETIC_EXCEPTION_CHECKS_ENABLED_ == true ? 'ENABLED' : 'DISABLED');
/** @define {string} */
goog.define(
    'jre.checks.type',
    (ARRAY_CHECK_TYPES_ == 'auto' && CAST_CHECKS_ENABLED_ == 'auto') ?
        'AUTO' :
        (ARRAY_CHECK_TYPES_ == true || CAST_CHECKS_ENABLED_ == true) ?
        'ENABLED' :
        'DISABLED');
/** @define {string} */
goog.define('jre.checks.critical', 'AUTO');


// TODO(goktug): rename to jre.logging
goog.provide('gwt.logging');

/** @define {string} */
goog.define('gwt.logging.simpleConsoleHandler', "ENABLED");
/** @define {string} */
goog.define(
    'gwt.logging.enabled', jre.debugMode == 'ENABLED' ? 'TRUE' : 'SEVERE');
