/**
 * @fileoverview Header hand rolled.
 *
 * @suppress {lateProvide}
 */
goog.module('vmbootstrap.primitives.Primitives');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let Long = goog.require('nativebootstrap.Long');
let _LongUtils = goog.require('vmbootstrap.LongUtils');
let _$int = goog.require('vmbootstrap.primitives.$int');
let _InternalPreconditions = goog.require('javaemul.internal.InternalPreconditions');


// Re-exports the implementation.
let Primitives = goog.require('vmbootstrap.primitives.Primitives$impl');
exports = Primitives;
