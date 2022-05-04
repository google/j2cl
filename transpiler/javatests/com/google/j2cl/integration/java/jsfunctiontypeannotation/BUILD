load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "jsfunctiontypeannotation",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    enable_kt = False,
    # JsInterop features are not supported in WASM.
    enable_wasm = False,
    suppress = ["JSC_TYPE_MISMATCH"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
