/**
 * @fileoverview Header hand rolled.
 *
 * @suppress {lateProvide}
 */
goog.module('vmbootstrap.CharSequences');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.

let _Numbers = goog.require('vmbootstrap.Numbers');
let _Comparable = goog.require('java.lang.CharSequence');
let _String = goog.require('java.lang.String');

// Re-exports the implementation.
let CharSequences = goog.require('vmbootstrap.CharSequences$impl');
exports = CharSequences;
