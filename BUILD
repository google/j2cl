# Description:
#  Public targets available externally. Also see build_defs/rules.bzl for the provided rules.

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load("@bazel_skylib//rules:common_settings.bzl", "bool_flag")

package(
    default_visibility = ["//visibility:public"],
    licenses = ["notice"],  # Apache 2.0
)

exports_files(["LICENSE"])

# JRE emulation
# This is not an alias since it is intended only for js targets not j2cl targets
closure_js_library(
    name = "jre",
    exports = ["//jre/java:jre"],
)

# Note that JsInterop targets may disappear after jsinterop-annotations get its own repo.

alias(
    name = "jsinterop-annotations",
    actual = "//third_party:gwt-jsinterop-annotations",
)

alias(
    name = "jsinterop-annotations-j2cl",
    actual = "//third_party:gwt-jsinterop-annotations-j2cl",
)

# JUnit library emulation
alias(
    name = "junit",
    actual = "//junit/emul/java:junit_emul",
)

# JUnit async runner (EXPERIMENTAL)

alias(
    name = "junit_async_runner",
    actual = "//junit/generator/java/com/google/j2cl/junit/async",
)

alias(
    name = "junit_async_runner-j2cl",
    actual = "//junit/generator/java/com/google/j2cl/junit/async:async-j2cl",
)

# Optional minifier library for development servers
alias(
    name = "minifier",
    actual = "//tools/java/com/google/j2cl/tools/minifier",
)

sh_binary(
    name = "deploy",
    srcs = ["deploy.sh"],
    visibility = ["//visibility:private"],
)

bool_flag(
    name = "enable_experimental_tree_artifact_mode",
    build_setting_default = False,
)
