"""Bazel rule for loading external repository deps for J2CL."""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def load_j2cl_repo_deps():
    # TODO(b/211017789):Use old proto version until maven issue is fixed.
    http_archive(
        name = "com_google_protobuf",
        strip_prefix = "protobuf-3.15.3",
        sha256 = "b10bf4e2d1a7586f54e64a5d9e7837e5188fc75ae69e36f215eb01def4f9721b",
        urls = [
            "https://github.com/protocolbuffers/protobuf/archive/v3.15.3.tar.gz",
        ],
    )

    _github_repo(
        name = "io_bazel_rules_closure",
        repo = "bazelbuild/rules_closure",
        tag = "5abddd3891ad9433267b25f40d938b8f66f08a48",
        sha256 = "cc784777922476a21ddef6d654e9b8e29a5ff9a9d8081bfc9b157e3d059d0927",
    )

    # TODO(goktug): Consider moving to setup_j2cl_workspace after licences migration
    # is completed.
    _github_repo(
        name = "rules_license",
        repo = "bazelbuild/rules_license",
        sha256 = "792aad709d8abfbf9e1b523e4c82b6f7cb6035222241f51901e80a7b64a58f94",
        tag = "0.0.4",
    )

    _github_repo(
        name = "bazel_skylib",
        repo = "bazelbuild/bazel-skylib",
        tag = "1.0.2",
        sha256 = "64ad2728ccdd2044216e4cec7815918b7bb3bb28c95b7e9d951f9d4eccb07625",
    )

    _github_repo(
        name = "io_bazel_rules_kotlin",
        repo = "bazelbuild/rules_kotlin",
        tag = "legacy-1.3.0-rc3",
        sha256 = "54678552125753d9fc0a37736d140f1d2e69778d3e52cf454df41a913b964ede",
    )

def _github_repo(name, repo, tag, sha256 = None):
    if native.existing_rule(name):
        return

    _, project_name = repo.split("/")
    http_archive(
        name = name,
        strip_prefix = "%s-%s" % (project_name, tag),
        url = "https://github.com/%s/archive/%s.zip" % (repo, tag),
        sha256 = sha256,
    )
