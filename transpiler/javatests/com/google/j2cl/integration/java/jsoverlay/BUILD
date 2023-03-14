load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "jsoverlay",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    # TODO(b/264466634): WASM can be enabled after generating imports.
    enable_wasm = False,
)
