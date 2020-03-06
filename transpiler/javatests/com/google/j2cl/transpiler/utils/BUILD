# Description:
#   Utilites for tests.

load("//build_defs:rules.bzl", "j2cl_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_library(
    name = "asserts",
    srcs = ["Asserts.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler:__subpackages__"],
    deps = [
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
