/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.CharSequences');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.

let _Strings = goog.require('vmbootstrap.Strings');
let _Numbers = goog.require('vmbootstrap.Numbers');
let _Comparable = goog.require('gen.java.lang.CharSequence');

// Re-exports the implementation.
let CharSequences = goog.require('vmbootstrap.CharSequences$impl');
exports = CharSequences;
