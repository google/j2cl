#!/bin/bash
# This file is used to prepare the githubrelease.
# It is not intended to be run directly but called from the github release
# workflow: bazel-contrib/.github/workflows/release_ruleset.yaml\
set -euo pipefail

tag="$1"
# GITHUB_REPOSITORY looks like user/repository_name, let strip the user part.
repository=${GITHUB_REPOSITORY#*/}
# The prefix is used to determine the directory structure of the archive. We
# strip the 'v'prefix from the version number.
directory="${repository}-${tag#v}"
archive="${repository}-${tag}.tar.gz"

git archive --format=tar --prefix=${directory}/ -o "${archive}" ${tag}

# Replace hyphens with underscores in the repository name to match our Bazel
# module naming conventions.
bazel_module_name="${repository//-/_}"
# The stdout of this program will be used as the top of the release notes for
# this release.
cat << EOF
## Using Bazel 8 or later, add to your \`MODULE.bazel\` file:

\`\`\`starlark
bazel_dep(name = "${bazel_module_name}", version = "${tag}")
\`\`\`
EOF