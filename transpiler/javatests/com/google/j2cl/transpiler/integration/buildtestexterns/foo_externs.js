/**
 * @fileoverview Example externs definition for type Foo.
 */

/**
 * @constructor
 * @param {string} value
 * @return {Foo}
 */
function Foo(value) {}

/**
 * @this {Foo}
 * @return {string}
 */
Foo.prototype.bar = function() {};

// Properties.

/**
 * @type {string}
 */
Foo.prototype.foo;

/**
 * @type {Foo}
 */
Foo.A;

/**
 * @type {Foo}
 */
Foo.B;

/**
 * @type {Foo}
 */
Foo.C;
