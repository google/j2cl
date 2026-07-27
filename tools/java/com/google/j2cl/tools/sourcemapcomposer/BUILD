# Description:
#  Implementation of source map composer for J2CL.

load("@rules_java//java:defs.bzl", "java_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

java_library(
    name = "sourcemapcomposer_lib",
    srcs = glob(["*.java"]),
    visibility = ["//tools/javatests/com/google/j2cl/tools/sourcemapcomposer:__pkg__"],
    deps = [
        "//third_party:jscomp",
        "//third_party:jspecify_annotations",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
    ],
)
