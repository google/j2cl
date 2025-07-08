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

# The script creates a tag to mark an individual point in the repository history
# of J2CL and includes a version number for J2CL release.
set -euo pipefail

source "$(dirname "$0")/deploy.sh"

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

parse_arguments() {
  lib_version=""

  while [[ $# -gt 0 ]]; do
    case $1 in
      --version )
        shift
        lib_version=$1
        ;;
      --help )
        usage
        exit 0
        ;;
      * )
        common::error "unexpected option $1"
        ;;
    esac
    shift
  done
}

check_prerequisites() {
  common::check_bazel
  common::check_version_set
}

main() {
  parse_arguments "$@"
  check_prerequisites
  common::create_and_push_git_tag "${lib_version}"
}

main "$@"
