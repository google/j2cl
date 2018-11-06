# Description:
#  Public targets available externally. Also see build_defs/rules.bzl for the provided rules.

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

package(default_visibility = ["//visibility:public"])

# Apache2
licenses(["notice"])

exports_files(["LICENSE"])

load("//build_defs:rules.bzl", "j2cl_library")

# JRE emulation - for js_library targets
closure_js_library(
    name = "jre",
    exports = ["//jre/java:jre"],
)

# JUnit library emulation
j2cl_library(
    name = "junit",
    exports = ["//junit/emul/java:junit_emul"],
)

# Optional minifier library for development servers
java_library(
    name = "minifier",
    exports = ["//tools/java/com/google/j2cl/tools/minifier"],
)
