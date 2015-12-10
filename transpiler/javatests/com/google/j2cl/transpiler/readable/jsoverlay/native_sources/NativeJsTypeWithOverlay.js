goog.module('test.foo.Main$NativeJsTypeWithOverlay');


class Main$NativeJsTypeWithOverlay {
  m() { return 1; }

  static n() {}
};

/**
 * @public {number}
 */
Main$NativeJsTypeWithOverlay.nonJsOverlayField = 1;


exports = Main$NativeJsTypeWithOverlay;
