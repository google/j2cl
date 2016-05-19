/**
 * @fileoverview Header hand rolled.
 * @suppress {extraRequire,lateProvide}
 */
goog.module('vmbootstrap.LongUtils');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
const _Long = goog.require('nativebootstrap.Long$impl');
const _Arrays = goog.require('vmbootstrap.Arrays$impl');
const _Primitives = goog.require('vmbootstrap.primitives.Primitives');

// Re-exports the implementation.
const LongUtils = goog.require('vmbootstrap.LongUtils$impl');
exports = LongUtils;
