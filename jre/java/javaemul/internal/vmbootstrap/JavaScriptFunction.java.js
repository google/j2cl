/**
 * @fileoverview Header hand rolled.
 *
 * @suppress {lateProvide}
 */
goog.module('vmbootstrap.JavaScriptFunction');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _$Util = goog.require('nativebootstrap.Util');


// Re-exports the implementation.
let JavaScriptObject = goog.require('vmbootstrap.JavaScriptFunction$impl');
exports = JavaScriptObject;
