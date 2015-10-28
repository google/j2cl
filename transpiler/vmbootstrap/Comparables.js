/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Comparables');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Boolean = goog.require('gen.java.lang.Boolean');
let _Class = goog.require('gen.java.lang.Class');
let _Comparable = goog.require('gen.java.lang.Comparable');
let _Double = goog.require('gen.java.lang.Double');
let _String = goog.require('gen.java.lang.String');
let _$boolean = goog.require('vmbootstrap.primitives.$boolean');
let _$double = goog.require('vmbootstrap.primitives.$double');

// Re-exports the implementation.
let Comparables = goog.require('vmbootstrap.Comparables$impl');
exports = Comparables;
