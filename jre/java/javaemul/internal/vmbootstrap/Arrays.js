/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Arrays');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Serializable = goog.require('java.io.Serializable');
let _ArrayIndexOutOfBoundsException = goog.require('java.lang.ArrayIndexOutOfBoundsException');
let _ArrayStoreException = goog.require('java.lang.ArrayStoreException');
let _Cloneable = goog.require('java.lang.Cloneable');
let _Class = goog.require('java.lang.Class');
let _Object = goog.require('java.lang.Object');
let _Integer = goog.require('java.lang.Integer');
let _NullPointerException = goog.require('java.lang.NullPointerException');
let _Hashing = goog.require('nativebootstrap.Hashing');
let _Casts = goog.require('vmbootstrap.Casts');
let _Exceptions = goog.require('vmbootstrap.Exceptions');



// Re-exports the implementation.
let Arrays = goog.require('vmbootstrap.Arrays$impl');
exports = Arrays;
