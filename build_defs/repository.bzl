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
        tag = "248f0e4a2bfd6063e88278be081d55715ecd298c",
        sha256 = "f5318f5c06bc02270852359812555cfda87b34e4002c855ba25a5a81ff551ac9",
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
        name = "com_google_absl_py",
        repo = "abseil/abseil-py",
        tag = "pypi-v0.15.0",
        sha256 = "39baf348f9358346dde9b021b54f7d2b02a416246f147f2329f59515b6b10c70",
    )

    # Six is a dependency of com_google_absl_py
    http_archive(
        name = "six_archive",
        build_file = "@com_google_absl_py//third_party:six.BUILD",
        strip_prefix = "six-1.10.0",
        urls = [
            "http://mirror.bazel.build/pypi.python.org/packages/source/s/six/six-1.10.0.tar.gz",
            "https://pypi.python.org/packages/source/s/six/six-1.10.0.tar.gz",
        ],
        sha256 = "105f8d68616f8248e24bf0e9372ef04d3cc10104f1980f54d57b2ce73a5ad56a",
    )

    _github_repo(
        name="com_google_binaryen",
        repo = "WebAssembly/binaryen",
        tag = "c170fe7893fc05147de455e36d0a36b356b3f3db",
        sha256 = "5af0f5028e869ee3569e49ed2170b7f680a827d43c9c110de868d46db3b275e8",
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
