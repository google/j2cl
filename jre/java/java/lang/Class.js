/**
 * Header super source for java.lang.Class.
 */
goog.module('gen.java.lang.Class');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _$Util = goog.require('nativebootstrap.Util');
let _Object = goog.require('gen.java.lang.Object');


// Re-exports the implementation.
let Class = goog.require('gen.java.lang.Class$impl');
exports = Class;
