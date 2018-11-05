#!/bin/bash
set -e

chmod +x ${KOKORO_GFILE_DIR}/use_bazel.sh
${KOKORO_GFILE_DIR}/use_bazel.sh latest
bazel version

# the repo is cloned under git/j2cl
cd git/j2cl

bazel build {transpiler,tools,jre,samples/helloworld}/java/...
