const googReflect = goog.require('goog.reflect');

let dummy = Object.create(null);

/**
 * @param {string} s
 * @return {string}
 */
Platform.forceCopy = function(s) {
  Platform.$clinit();
  // Using string as a dictionary key forces V8 to internalize the string which
  // reallocates "sliced" String and drop the reference to the original large
  // string and get it GC'd.
  // TODO(goktug): Remove this when V8 starts doing the same for Map keys.
  // https://chromium-review.googlesource.com/c/v8/v8/+/6965654
  googReflect.sinkValue(dummy[s]);
  return s;
};
