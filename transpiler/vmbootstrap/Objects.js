/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Objects');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Class = goog.require('gen.java.lang.Class');
let _Arrays = goog.require('vmbootstrap.Arrays');
let _Booleans = goog.require('vmbootstrap.Booleans');
let _Numbers = goog.require('vmbootstrap.Numbers');
let _Strings = goog.require('vmbootstrap.Strings');


// Re-exports the implementation.
let Objects = goog.require('vmbootstrap.Objects$impl');
exports = Objects;
