/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Numbers');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Character = goog.require('gen.java.lang.Character');
let _Class = goog.require('gen.java.lang.Class');
let _Double = goog.require('gen.java.lang.Double');
let _Number = goog.require('gen.java.lang.Number');
let _$Long = goog.require('nativebootstrap.Long');
let _$Casts = goog.require('vmbootstrap.Casts');
let _$double = goog.require('vmbootstrap.primitives.$double');
let _Primitives = goog.require('vmbootstrap.primitives.Primitives');


// Re-exports the implementation.
let Numbers = goog.require('vmbootstrap.Numbers$impl');
exports = Numbers;
