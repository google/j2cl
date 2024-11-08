"""Bazel rule for loading external repository deps for J2CL."""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def load_j2cl_repo_deps():
    # TODO(b/211017789):Use old proto version until maven issue is fixed.
    http_archive(
        name = "com_google_protobuf",
        strip_prefix = "protobuf-3.19.0",
        sha256 = "4a045294ec76cb6eae990a21adb5d8b3c78be486f1507faa601541d1ccefbd6b",
        urls = [
            "https://github.com/protocolbuffers/protobuf/archive/v3.19.0.tar.gz",
        ],
    )

    _github_repo(
        name = "io_bazel_rules_closure",
        repo = "bazelbuild/rules_closure",
        tag = "7dc462656229f37f0ab90bba3c82c9f6b8171ebf",
        sha256 = "09965ff4236d678c81bcfb1a42502ba7e8feafa92be02ccf0d8d4eccc51379be",
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

    _load_binaryen()

def _load_binaryen():

    PY_VERSION = "0.23.1"

    http_archive(
        name = "rules_python",
        strip_prefix = "rules_python-{}".format(PY_VERSION),
        url = "https://github.com/bazelbuild/rules_python/releases/download/{}/rules_python-{}.tar.gz".format(PY_VERSION, PY_VERSION),
        sha256 = "84aec9e21cc56fbc7f1335035a71c850d1b9b5cc6ff497306f84cced9a769841",
    )

    _github_repo(
        name="com_google_binaryen",
        repo = "WebAssembly/binaryen",
        tag = "6645f0c05b8e9268f35742bb6b0a67e0a9c40795",
        sha256 = "a3b0c6b4884cd147913088817f6e238e700394834a7cbd8610acd6b23bdd4860",
        patch_args = ["-p1"],
        build_file = "@com_google_j2cl//build_defs/internal_do_not_use/binaryen:BUILD.binaryen",
        patches = ["@com_google_j2cl//build_defs/internal_do_not_use/binaryen:generate_intrinsics.patch"],
    )

def _github_repo(name, repo, tag, **kwargs):
    if native.existing_rule(name):
        return

    _, project_name = repo.split("/")
    http_archive(
        name = name,
        strip_prefix = "%s-%s" % (project_name, tag),
        url = "https://github.com/%s/archive/%s.zip" % (repo, tag),
        **kwargs
    )
