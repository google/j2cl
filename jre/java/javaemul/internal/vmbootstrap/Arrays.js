/**
 * @fileoverview Header hand rolled.
 *
 * @suppress {extraRequire, lateProvide}
 */
goog.module('vmbootstrap.Arrays');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
const _Serializable = goog.require('java.io.Serializable');
const _ArrayIndexOutOfBoundsException = goog.require('java.lang.ArrayIndexOutOfBoundsException');
const _Cloneable = goog.require('java.lang.Cloneable');
const _Class = goog.require('java.lang.Class');
const _Object = goog.require('java.lang.Object');
const _Integer = goog.require('java.lang.Integer');
const _NullPointerException = goog.require('java.lang.NullPointerException');
const _Hashing = goog.require('nativebootstrap.Hashing');
const _Exceptions = goog.require('vmbootstrap.Exceptions');
const _InternalPreconditions = goog.require('javaemul.internal.InternalPreconditions');



// Re-exports the implementation.
const Arrays = goog.require('vmbootstrap.Arrays$impl');
exports = Arrays;
