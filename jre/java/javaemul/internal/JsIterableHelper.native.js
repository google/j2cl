/**
 * @param {?T} value
 * @param {boolean} done
 * @return {!IIterableResult<T>}
 * @template T
 */
JsIterableHelper.makeResult = function(value, done) {
  return {value: value, done: done};
};

/**
 * @param {!Array<T>} array
 * @return {!Iterator<T>}
 * @template T
 */
JsIterableHelper.asJsIterator = function(array) {
  return array[Symbol.iterator]();
};
