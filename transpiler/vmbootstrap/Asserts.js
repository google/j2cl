/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Asserts');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _AssertionError = goog.require('gen.java.lang.AssertionError');


// Re-exports the implementation.
let Asserts = goog.require('vmbootstrap.Asserts$impl');
exports = Asserts;
