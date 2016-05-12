/**
 * @fileoverview Header hand rolled.
 *
 * @suppress {lateProvide}
 */
goog.module('vmbootstrap.LongUtils');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Long = goog.require('nativebootstrap.Long$impl');
let _Primitives = goog.require('vmbootstrap.primitives.Primitives');

// Re-exports the implementation.
let LongUtils = goog.require('vmbootstrap.LongUtils$impl');
exports = LongUtils;
