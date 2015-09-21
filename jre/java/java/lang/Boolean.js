/**
 * Header super source for java.lang.Boolean.
 */
goog.module('gen.java.lang.Boolean');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _$Util = goog.require('nativebootstrap.Util');
let _Class = goog.require('gen.java.lang.Class');
let _Object = goog.require('gen.java.lang.Object');


// Re-exports the implementation.
let Boolean = goog.require('gen.java.lang.Boolean$impl');
exports = Boolean;
