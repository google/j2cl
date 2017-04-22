/**
 * @param {number} a
 * @param {number} b
 * @return {*}
 */
JsTypeObjectMethodsTest.createWithEqualsAndHashCode = function(a, b) {
  return {
    a: a,
    b: b,
    hashCode: function() { return this.b; },
    equals: function(other) { return this.a == other.a; }
  };
};

/**
 * @param {number} a
 * @param {number} b
 * @return {*}
 */
JsTypeObjectMethodsTest.createWithoutEqualsAndHashCode = function(a, b) {
  return {a: a, b: b};
};

/**
 * @param {*} a
 * @return {*}
 */
JsTypeObjectMethodsTest.callHashCode = function(a) {
  return a.hashCode();
};

/**
 * @return {void}
 */
JsTypeObjectMethodsTest.patchErrorWithJavaLangObjectMethods = function() {
  Error.prototype.equals = function(o) { return this.myValue == o.myValue; };
};
