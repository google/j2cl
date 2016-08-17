/**
 * @fileoverview Header hand rolled.
 *
 * @suppress {lateProvide}
 */
goog.module('vmbootstrap.Objects');


// Imports headers for both eager and lazy dependencies to ensure that
// all files are included in the dependency tree.
let _Boolean = goog.require('java.lang.Boolean');
let _Class = goog.require('java.lang.Class');
let _Double = goog.require('java.lang.Double');
let _Object = goog.require('java.lang.Object');
let _String = goog.require('java.lang.String');
let _Arrays = goog.require('vmbootstrap.Arrays');
let _JavaScriptFunction = goog.require('vmbootstrap.JavaScriptFunction');
let _JavaScriptObject = goog.require('vmbootstrap.JavaScriptObject');
let _$Hashing = goog.require('nativebootstrap.Hashing');


// Re-exports the implementation.
let Objects = goog.require('vmbootstrap.Objects$impl');
exports = Objects;
