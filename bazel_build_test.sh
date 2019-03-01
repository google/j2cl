#!/bin/bash
# Script that can be used by CI server for testing j2cl builds.
set -e

bazel build :all tools/java/...

# build hello world sample in its own workspace
cd samples/helloworld

bazel build src/main/...

