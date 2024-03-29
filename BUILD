load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")
load("@rules_license//rules:license.bzl", "license")
load("//build_defs:rules.bzl", "j2cl_alias")

# Description:
#  Public targets available externally. Also see build_defs/rules.bzl for the provided rules.
load("@bazel_skylib//rules:common_settings.bzl", "bool_flag", "string_flag")

package(
    default_applicable_licenses = [":j2cl_license"],
    default_visibility = ["//visibility:public"],
    licenses = ["notice"],
)

license(
    name = "j2cl_license",
    package_name = "j2cl",
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
    actual = "//third_party:jsinterop-annotations",
)

j2cl_alias(
    name = "jsinterop-annotations-j2cl",
    actual = "//third_party:jsinterop-annotations-j2cl",
)

# JUnit library emulation
j2cl_alias(
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

# TODO(b/135123615): Temporary support for selecting frontend. Once javac becomes the frontend for
# J2CL this will be removed.
string_flag(
    name = "experimental_java_frontend",
    build_setting_default = "jdt",
    values = [
        "jdt",
        "javac",
    ],
)

# Flag to enable j2kt-web experiment. Please talk to j2cl-team@ before using it.
bool_flag(
    name = "experimental_enable_j2kt_web",
    build_setting_default = False,
)
