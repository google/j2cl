/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Casts');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _ClassCastException = goog.require('java.lang.ClassCastException');
let _Exceptions = goog.require('vmbootstrap.Exceptions');

// Re-exports the implementation.
let Casts = goog.require('vmbootstrap.Casts$impl');
exports = Casts;
