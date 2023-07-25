#!/bin/bash -i
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

# The script creates a tag to mark an individual point in the repository history
# of J2CL and includes a version number for J2CL release.

set -e

usage() {
    echo ""
    echo "$(basename $0): Tag script for J2CL."
    echo ""
    echo "$(basename $0) --version <version>"
    echo "    --help"
    echo "        Print this help output and exit."
    echo "    --version <version>"
    echo "        Release version of the J2CL."
    echo ""
}

while [[ "$1" != "" ]]; do
  case $1 in
    --version )    if [ -z $2 ]; then
                     echo "Error: Incorrect version value."
                     usage
                     exit 1
                   fi
                   shift
                   lib_version=$1
                   ;;
    --help )       usage
                   exit 1
                   ;;
    * )            echo "Error: unexpected option $1"
                   usage
                   exit 1
                   ;;
  esac
  shift
done

if [[ -z "$lib_version" ]]; then
  echo "Error: --version flag is missing"
  usage
  exit 1
fi

if [ ! -f "WORKSPACE" ]; then
  echo "Error: should be run from the root of the Bazel repository"
  exit 1
fi

git tag -a ${lib_version} -m "${lib_version} release"
git push origin ${lib_version}

