goog.module('test.foo.NativeJsTypeWithOverlay');


class Main$NativeJsTypeWithOverlay {
  m() { return 1; }

  static n() {}
};

/**
 * @public {number}
 */
Main$NativeJsTypeWithOverlay.nonJsOverlayField = 1;


exports = Main$NativeJsTypeWithOverlay;
