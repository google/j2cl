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
# Runs the transpiler in debug mode.
#
# Launch the script directly, not through Blaze.

java_dir="third_party/java_src/j2cl/transpiler/javatests"
examples_dir="$java_dir/com/google/j2cl/transpiler/readable/"
transpiler_bin="blaze-bin/third_party/java_src/j2cl/j2cl"
jre_jar="blaze-bin/third_party/java_src/j2cl/jre/java/libJavaJre.jar"
example_name=$1

# Show commands as they occur
set -x

# Fail on any error
set -e

# Build  JRE
blaze build third_party/java_src/j2cl/jre/java:JavaJre &> /dev/null

# Build the transpiler
blaze build third_party/java_src/j2cl:j2cl &> /dev/null

# Figure out where the referenced example lives
example_dir=$examples_dir$example_name

# Accumulate java files in this example and subdirs
example_files=""
for java_file in $(find $example_dir -name '*.java') ; do
  example_files="$example_files$java_file "
done

# Transpile and debug it
$transpiler_bin --debug -d /tmp/gen/$example_name -cp $jre_jar $example_files
