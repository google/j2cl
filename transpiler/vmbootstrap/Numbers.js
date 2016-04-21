/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Numbers');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Character = goog.require('java.lang.Character');
let _Class = goog.require('java.lang.Class');
let _Double = goog.require('java.lang.Double');
let _Number = goog.require('java.lang.Number');
let _$Long = goog.require('nativebootstrap.Long');
let _$double = goog.require('vmbootstrap.primitives.$double');


// Re-exports the implementation.
let Numbers = goog.require('vmbootstrap.Numbers$impl');
exports = Numbers;
