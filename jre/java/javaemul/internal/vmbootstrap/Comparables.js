/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Comparables');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Boolean = goog.require('java.lang.Boolean');
let _Class = goog.require('java.lang.Class');
let _Comparable = goog.require('java.lang.Comparable');
let _Double = goog.require('java.lang.Double');
let _String = goog.require('java.lang.String');
let _$boolean = goog.require('vmbootstrap.primitives.$boolean');
let _$double = goog.require('vmbootstrap.primitives.$double');

// Re-exports the implementation.
let Comparables = goog.require('vmbootstrap.Comparables$impl');
exports = Comparables;
