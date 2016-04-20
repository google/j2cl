
/**
 * @return {string}
 * @public
 */
Object.prototype.toString = function() {
  return window.String(this.$javaToString());
};
