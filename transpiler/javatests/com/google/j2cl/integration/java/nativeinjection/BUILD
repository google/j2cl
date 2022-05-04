load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

j2cl_library(
    name = "superinject",
    srcs = glob([
        "super/**/*.java",
        "super/**/*.native.js",
    ]),
    generate_j2kt_jvm_library = False,
    generate_j2kt_native_library = False,
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

integration_test(
    name = "nativeinjection",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    enable_kt = False,
    # JsInterop features are not supported in WASM.
    enable_wasm = False,
    deps = [
        ":superinject",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
