/**
 * @param {number} a
 * @param {number} b
 * @return {*}
 */
__class.createWithEqualsAndHashCode = function(a, b) {
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
__class.createWithoutEqualsAndHashCode = function(a, b) {
  return {a: a, b: b};
};

/**
 * @param {*} a
 * @return {*}
 */
__class.callHashCode = function(a) {
  return a.hashCode();
};

/**
 * @return {void}
 */
__class.patchErrorWithJavaLangObjectMethods = function() {
  Error.prototype.equals = function(o) { return this.myValue == o.myValue; };
};
