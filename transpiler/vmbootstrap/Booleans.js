/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Booleans');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Boolean = goog.require('gen.java.lang.Boolean');
let _Class = goog.require('gen.java.lang.Class');
let _$boolean = goog.require('vmbootstrap.primitives.$boolean');
let _Casts = goog.require('vmbootstrap.Casts');


// Re-exports the implementation.
let Booleans = goog.require('vmbootstrap.Booleans$impl');
exports = Booleans;
