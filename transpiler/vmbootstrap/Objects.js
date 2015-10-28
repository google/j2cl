/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Objects');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Boolean = goog.require('gen.java.lang.Boolean');
let _Class = goog.require('gen.java.lang.Class');
let _Double = goog.require('gen.java.lang.Double');
let _String = goog.require('gen.java.lang.String');
let _Arrays = goog.require('vmbootstrap.Arrays');
let _$boolean = goog.require('vmbootstrap.primitives.$boolean');
let _$double = goog.require('vmbootstrap.primitives.$double');


// Re-exports the implementation.
let Objects = goog.require('vmbootstrap.Objects$impl');
exports = Objects;
