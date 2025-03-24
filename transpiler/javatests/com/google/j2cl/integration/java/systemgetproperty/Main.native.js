/** @suppress {extraRequire} */
goog.require('systemgetproperty.Zoo');

/** @define {string} */
const localBazz = goog.define('bazz', '42');

/** Registers 'bazz' as a sysem property on-demand. */
Main.registerBazz = function() {
  jre.addSystemPropertyFromGoogDefine('bazz', localBazz);
};
