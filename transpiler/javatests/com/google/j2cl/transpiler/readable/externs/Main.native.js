// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
