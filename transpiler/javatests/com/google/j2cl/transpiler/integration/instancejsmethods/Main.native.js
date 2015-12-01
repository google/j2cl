/**
 * @param {*} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
Main.callParentFun = function(p, a, b) {
  Main.$clinit();
  return p.sum(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
Main.callParentBar = function(p, a, b) {
  Main.$clinit();
  return p.bar(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @return {number}
 */
Main.callParentFoo = function(p, a) {
  Main.$clinit();
  return p.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
Main.callChildFun = function(c, a, b) {
  Main.$clinit();
  return c.sum(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
Main.callChildBar = function(c, a, b) {
  Main.$clinit();
  return c.bar(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 */
Main.callChildFoo = function(c, a) {
  Main.$clinit();
  return c.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 */
Main.callChildIntfFoo = function(c, a) {
  Main.$clinit();
  return c.intfFoo(a);
}
