load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)
load("//build_defs:rules.bzl", "j2wasm_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

# Provide a custom j2wasm_library so no .js files passed to target.
j2wasm_library(
    name = "wasm-j2wasm",
    srcs = glob(["*.java"]),
    javacopts = ["-XepDisableAllChecks"],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:gwt-jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

# Creates targets
# blaze build :optimized_js
# blaze test :compiled_test
# blaze test :uncompiled_test
integration_test(
    name = "wasm",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    enable_kt = False,
    deps = [
        "//jre/java:javaemul_internal_annotations-j2cl",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
