/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.primitives.Primitives');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let Long = goog.require('nativebootstrap.Long');
let LongUtils = goog.require('nativebootstrap.LongUtils');
let _$int = goog.require('vmbootstrap.primitives.$int');


// Re-exports the implementation.
let Primitives = goog.require('vmbootstrap.primitives.Primitives$impl');
exports = Primitives;
