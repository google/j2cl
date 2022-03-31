# Description:
#   Utilites for tests.

load("//build_defs:rules.bzl", "j2cl_library", "j2wasm_library")

package(
    default_visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    licenses = ["notice"],
)

j2wasm_library(
    name = "testing-j2wasm",
    srcs = [
        "Asserts.java",
        "super-wasm/AssertsBase.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
    ],
)

j2cl_library(
    name = "testing",
    srcs = [
        "Asserts.java",
        "AssertsBase.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
