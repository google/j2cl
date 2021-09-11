# Description:
#   Utilites for tests.

load("//build_defs:rules.bzl", "j2cl_library", "j2kt_library", "j2wasm_library")

package(
    licenses = ["notice"],
)

j2kt_library(
    name = "testing-j2kt",
    srcs = [
        "super-kt/Asserts.java",
    ],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    #TODO(jparachoniak): Get j2kt to a point where it will transpile the assert classes.
)

j2wasm_library(
    name = "testing-j2wasm",
    srcs = [
        "Asserts.java",
        "super-wasm/AssertsBase.java",
    ],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    deps = [
        "//third_party:gwt-javaemul-internal-annotations-j2wasm",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
    ],
)

j2cl_library(
    name = "testing",
    srcs = [
        "Asserts.java",
        "AssertsBase.java",
    ],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    deps = [
        "//third_party:gwt-javaemul-internal-annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
