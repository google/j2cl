/**
 * Header hand rolled.
 */
goog.module('vmbootstrap.Arrays');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Serializable = goog.require('gen.java.io.Serializable');
let _ArrayIndexOutOfBoundsException = goog.require('gen.java.lang.ArrayIndexOutOfBoundsException');
let _ArrayStoreException = goog.require('gen.java.lang.ArrayStoreException');
let _Cloneable = goog.require('gen.java.lang.Cloneable');
let _Class = goog.require('gen.java.lang.Class');
let _Object = goog.require('gen.java.lang.Object');
let _Integer = goog.require('gen.java.lang.Integer');
let _NullPointerException = goog.require('gen.java.lang.NullPointerException');
let _Hashing = goog.require('nativebootstrap.Hashing');
let _Casts = goog.require('vmbootstrap.Casts');
let _Exceptions = goog.require('vmbootstrap.Exceptions');



// Re-exports the implementation.
let Arrays = goog.require('vmbootstrap.Arrays$impl');
exports = Arrays;
