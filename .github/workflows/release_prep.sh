#!/bin/bash
# This file is used to prepare the githubrelease.
# It is not intended to be run directly but called from the github release
# workflow: bazel-contrib/.github/workflows/release_ruleset.yaml\
set -euo pipefail

tag="$1"
# The prefix is used to determine the directory structure of the archive. We strip the 'v'
# prefix from the version number.
directory="j2cl-${tag#v}"
archive="j2cl-${tag}.tar.gz"

git archive --format=tar --prefix=${directory}/ -o "${archive}" ${tag}

sha256=$(shasum -a 256 "${archive}" | awk '{print $1}')

# The stdout of this program will be used as the top of the release notes for this release.
cat << EOF
## Using Bazel 8 or later, add to your \`MODULE.bazel\` file:

\`\`\`starlark
bazel_dep(name = "j2cl", version = "${tag}")
\`\`\`
EOF
