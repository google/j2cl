/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Casts');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _ClassCastException = goog.require('gen.java.lang.ClassCastException');


// Re-exports the implementation.
let Casts = goog.require('vmbootstrap.Casts$impl');
exports = Casts;
