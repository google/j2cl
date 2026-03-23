/**
 * @param {?T} value
 * @param {boolean} done
 * @return {!IIterableResult<T>}
 * @template T
 */
JsIterableHelper.makeResult = function(value, done) {
  return {value: value, done: done};
};
