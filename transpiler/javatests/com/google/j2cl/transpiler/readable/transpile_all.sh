#!/bin/bash
#
# Copyright 2015 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License. You may
# obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied. See the License for the specific language governing
# permissions and limitations under the License.
#
# Regenerates and diffs all readable JS examples.
#
# Launch the script directly, not through Blaze.

java_dir="third_party/java_src/j2cl/transpiler/javatests"
examples_dir="$java_dir/com/google/j2cl/transpiler/readable/"
transpiler_bin="blaze-bin/third_party/java_src/j2cl/j2cl"

# Show commands as they occur
set -x

# Build the transpiler
blaze build third_party/java_src/j2cl:j2cl

# Transpile each example
for example_dir in $examples_dir*/ ; do
  # Accumulate java files in this example and subdirs
  example_files=""
  for java_file in $(find $example_dir -name '*.java') ; do
    example_files="$example_files$java_file "
  done
  # Transpile them
  $transpiler_bin -d $java_dir $example_files
done

# Format all JS and rename to .js.txt to avoid the linter
for js_file in $(find $examples_dir -name '*.js') ; do
  clang-format -style=Google -i $js_file > /dev/null
  mv $js_file $js_file.txt
done

# Diff modified JS
git difftool -y -- "$examples_dir**/*.js.txt"
