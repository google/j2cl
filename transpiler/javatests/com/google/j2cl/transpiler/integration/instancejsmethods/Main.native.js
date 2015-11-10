/**
 * @param {*} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 * @nocollapse
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
 * @nocollapse
 */
Main.callParentBar = function(p, a, b) {
  Main.$clinit();
  return p.bar(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @return {number}
 * @nocollapse
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
 * @nocollapse
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
 * @nocollapse
 */
Main.callChildBar = function(c, a, b) {
  Main.$clinit();
  return c.bar(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 * @nocollapse
 */
Main.callChildFoo = function(c, a) {
  Main.$clinit();
  return c.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 * @nocollapse
 */
Main.callChildIntfFoo = function(c, a) {
  Main.$clinit();
  return c.intfFoo(a);
}
