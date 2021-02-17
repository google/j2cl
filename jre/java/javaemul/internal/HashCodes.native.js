/**
 * @param {*} obj
 * @return {number}
 * @public
 */
HashCodes.getObjectIdentityHashCode = function(obj) {
  const o = /** @type {!Object} */ (obj);
  return o.$systemHashCode ||
      (Object.defineProperties(o, {
           $systemHashCode: {value: HashCodes.getNextHash(), enumerable: false}
         }),
       o.$systemHashCode);
};
