load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
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
    # JsInterop features are not supported in WASM.
    enable_wasm = False,
    deps = [
        ":superinject",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
