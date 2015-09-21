/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Strings');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Class = goog.require('gen.java.lang.Class');
let _String = goog.require('gen.java.lang.String');
let _$Casts = goog.require('vmbootstrap.Casts');


// Re-exports the implementation.
let Strings = goog.require('vmbootstrap.Strings$impl');
exports = Strings;
