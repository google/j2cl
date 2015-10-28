/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.CharSequences');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.

let _Numbers = goog.require('vmbootstrap.Numbers');
let _Class = goog.require('gen.java.lang.Class');
let _Comparable = goog.require('gen.java.lang.CharSequence');
let _String = goog.require('gen.java.lang.String');

// Re-exports the implementation.
let CharSequences = goog.require('vmbootstrap.CharSequences$impl');
exports = CharSequences;
