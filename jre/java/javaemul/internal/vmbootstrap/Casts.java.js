/**
 * @fileoverview Header hand rolled.
 *
 * @suppress {lateProvide}
 */
goog.module('vmbootstrap.Casts');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _InternalPreconditions = goog.require('javaemul.internal.InternalPreconditions');

// Re-exports the implementation.
let Casts = goog.require('vmbootstrap.Casts$impl');
exports = Casts;
