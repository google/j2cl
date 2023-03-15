# Description:
#   Utilites for tests.

load("//build_defs:rules.bzl", "j2cl_library", "j2wasm_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    licenses = ["notice"],
)

j2wasm_library(
    name = "testing-j2wasm",
    srcs = [
        "Asserts.java",
        "TestUtils.java",
        "super-wasm/AssertsBase.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
    ],
)

j2cl_library(
    name = "testing-j2cl",
    srcs = [
        "Asserts.java",
        "AssertsBase.java",
        "TestUtils.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

java_library(
    name = "testing",
    srcs = [
        "Asserts.java",
        "AssertsBase.java",
        "TestUtils.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations",
        "//third_party:gwt-jsinterop-annotations",
    ],
)
