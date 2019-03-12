"""Bazel rule for loading external repository deps for J2CL."""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

_IO_BAZEL_RULES_CLOSURE_VERSION = "master"
_BAZEL_SKYLIB_VERSION = "0.7.0"

def load_j2cl_repo_deps():
  http_archive(
    name = "io_bazel_rules_closure",
    strip_prefix = "rules_closure-%s" % _IO_BAZEL_RULES_CLOSURE_VERSION,
    url = "https://github.com/bazelbuild/rules_closure/archive/%s.zip"% _IO_BAZEL_RULES_CLOSURE_VERSION,
  )

  http_archive(
    name = "bazel_skylib",
    strip_prefix = "bazel-skylib-%s" % _BAZEL_SKYLIB_VERSION,
    url = "https://github.com/bazelbuild/bazel-skylib/archive/%s.zip"% _BAZEL_SKYLIB_VERSION,
  )

