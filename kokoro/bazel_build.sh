#!/bin/bash
set -e

chmod +x ${KOKORO_GFILE_DIR}/use_bazel.sh
${KOKORO_GFILE_DIR}/use_bazel.sh 0.19.0
bazel version

# the repo is cloned under git/j2cl
cd git/j2cl

bazel build :all

# build hello world sample in its own workspace
cd samples/helloworld

bazel build src/main/...
