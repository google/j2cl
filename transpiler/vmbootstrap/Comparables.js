/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Comparables');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Comparable = goog.require('gen.java.lang.Comparable');
let _Booleans = goog.require('vmbootstrap.Booleans');
let _Numbers = goog.require('vmbootstrap.Numbers');
let _Strings = goog.require('vmbootstrap.Strings');


// Re-exports the implementation.
let Comparables = goog.require('vmbootstrap.Comparables$impl');
exports = Comparables;
