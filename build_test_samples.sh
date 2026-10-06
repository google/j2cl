#!/bin/bash
# Copyright 2019 Google Inc. All Rights Reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS-IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -ex

j2cl_path="$(pwd)"

# Use the BAZELRC environment variable instead once we've migrated to Bazel 9.
bazel_rc="--bazelrc=$(pwd)/ci.bazelrc"


function bazel_workspace() {
  # Test in its own workspace with local (head) j2cl version.
  (cd $2 &&  bazel "${bazel_rc}" $1 "--override_module=j2cl=${j2cl_path}" ... && bazel shutdown)
}

# Build and test Hello World sample in its own workspace
bazel_workspace test "samples/helloworld"

bazel_workspace test "samples/wasm"

if [[ $1 == "CI" ]]; then
  bazel_workspace build "samples/guava"
fi
