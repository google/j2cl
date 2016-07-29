/**
 * @param {Foo} foo
 * @returns {string}
 */
function useFoo(foo) {
  return foo.foo;
}

/**
 * @param {*} foo
 * @return {void}
 * @public
 */
Main.m_useDirectlyAsFoo__java_lang_Object_$p_com_google_j2cl_transpiler_readable_externs_Main =
    function(foo) {
  useFoo(/** @type {Foo} */ (foo));
};