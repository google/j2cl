load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

integration_test(
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    # Contains JsInterop features which are only applicable for JS and Wasm JsInterop output.
    enable_jvm_test = False,
    enable_kt = False,
    # Calls exported non-native @JsMethod members from JS, which requires Wasm JsInterop.
    enable_wasm = False,
    enable_wasm_custom_descriptors_jsinterop = True,
)
