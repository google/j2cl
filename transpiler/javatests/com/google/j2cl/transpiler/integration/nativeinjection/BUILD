package(
    licenses = ["notice"],  # Apache 2.0
)

load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/transpiler/integration:integration_test.bzl",
    "integration_test",
)

j2cl_library(
    name = "superinject",
    srcs = glob([
        "super/**/*.java",
        "super/**/*.native.js",
    ]),
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "nativeinjection",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    deps = [
        ":superinject",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
