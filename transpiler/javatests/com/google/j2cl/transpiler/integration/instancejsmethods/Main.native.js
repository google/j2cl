/**
 * @param {*} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
__class.callParentFun = function(p, a, b) {
  __class.$clinit();
  return p.sum(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
__class.callParentBar = function(p, a, b) {
  __class.$clinit();
  return p.bar(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @return {number}
 */
__class.callParentFoo = function(p, a) {
  __class.$clinit();
  return p.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
__class.callChildFun = function(c, a, b) {
  __class.$clinit();
  return c.sum(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
__class.callChildBar = function(c, a, b) {
  __class.$clinit();
  return c.bar(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 */
__class.callChildFoo = function(c, a) {
  __class.$clinit();
  return c.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 */
__class.callChildIntfFoo = function(c, a) {
  __class.$clinit();
  return c.intfFoo(a);
}
