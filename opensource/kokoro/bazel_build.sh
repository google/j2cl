#!/bin/bash
set -e

# the repo is cloned under git/j2cl
cd git/j2cl

bazel build transpiler/java/...
bazel build tools/java/...