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
 * @param {TreeMap} map
 * @return {void}
 * @public
 */
TreeMapViolator.m_callAssertCorrectness__java_util_TreeMap = function(map) {
  TreeMapViolator.$clinit();
  map.m_assertCorrectness___$pp_java_util();
};
