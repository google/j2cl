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

# Script that can be used by CI server for testing j2cl builds.
set -e

bazel build :all {jre,transpiler,tools}/java/...

# Do a quick smoke check of integration test
bazel test transpiler/javatests/com/google/j2cl/integration/emptyclass/...

# Run CI test if requested
if [[ $1 == "CI" ]]; then
  bazel test transpiler/javatests/com/google/j2cl/integration/...

  # Build Hello World sample in its own workspace
  cd samples/helloworld && bazel build src/main/...

  # Build Guave sample in its own workspace
  cd ../guava && bazel build src/main/...
fi
