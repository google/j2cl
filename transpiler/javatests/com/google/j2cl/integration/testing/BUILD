# Description:
#   Utilites for tests.

load("//build_defs:rules.bzl", "j2cl_library")

package(
    licenses = ["notice"],
)

j2cl_library(
    name = "testing",
    srcs = ["Asserts.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    deps = [
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
