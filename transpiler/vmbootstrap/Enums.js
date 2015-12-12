/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Enums');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _IllegalArgumentException = goog.require('gen.java.lang.IllegalArgumentException');
let _Exceptions = goog.require('vmbootstrap.Exceptions');

// Re-exports the implementation.
let Enums = goog.require('vmbootstrap.Enums$impl');
exports = Enums;
