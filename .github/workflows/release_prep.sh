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
## Using bzlmod with Bazel 6 or later:

1. [Bazel 6] Add \`common --enable_bzlmod\` to \`.bazelrc\`.

2. Add to your \`MODULE.bazel\` file:

\`\`\`starlark
bazel_dep(
    name = "j2cl",
    repo_name = "com_google_j2cl",
    version = "${tag}")
\`\`\`

## Using WORKSPACE:

\`\`\`starlark

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "j2cl",
    sha256 = "${sha256}",
    strip_prefix = "${directory}",
    url = "https://github.com/bazelbuild/j2cl/releases/download/${tag}/${archive}",
)

\`\`\`
EOF