load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "jsinteroptests",
    srcs = glob([
        "**/*.java",
        "**/*.js",
    ]),
    enable_kt = False,
    # JsInterop features are not supported in WASM.
    enable_wasm = False,
    j2cl_library_tags = ["manual"],
    deps = [
        "//third_party:closure_reflect",
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)
