load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "bridgejsmethod",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    enable_kt = False,
    # Disabled because it is testing bridging needed for @JsMethod.
    enable_wasm = False,
    suppress = ["JSC_TYPE_MISMATCH"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
