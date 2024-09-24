# Description:
#   Utilites for tests.

load("//build_defs:rules.bzl", "j2cl_library", "j2kt_jvm_library", "j2kt_native_library", "j2wasm_library")
load("@rules_java//java:defs.bzl", "java_library")

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
        "super-wasm/Platform.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
    ],
)

j2kt_jvm_library(
    name = "testing-j2kt-jvm",
    srcs = [
        "Asserts.java",
        "AssertsBase.java",
        "TestUtils.java",
        "super-j2kt-jvm/Platform.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-jvm",
        "//third_party:jsinterop-annotations-j2kt-jvm",
    ],
)

j2kt_native_library(
    name = "testing-j2kt-native",
    srcs = [
        "Asserts.java",
        "AssertsBase.java",
        "TestUtils.java",
        "super-j2kt-native/Platform.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2kt-native",
        "//third_party:jsinterop-annotations-j2kt-native",
    ],
)

j2cl_library(
    name = "testing-j2cl",
    srcs = [
        "Asserts.java",
        "AssertsBase.java",
        "Platform.java",
        "TestUtils.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)

java_library(
    name = "testing",
    srcs = [
        "Asserts.java",
        "AssertsBase.java",
        "Platform.java",
        "TestUtils.java",
    ],
    deps = [
        "//jre/java:javaemul_internal_annotations",
        "//third_party:jsinterop-annotations",
    ],
)
