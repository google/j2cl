load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

alias(
    name = "closure_reflect-j2cl",
    actual = "//third_party:closure_reflect",
)

integration_test(
    name = "jsinteroptests",
    srcs = glob([
        "**/*.java",
        "**/*.js",
    ]),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    deps = [
        ":closure_reflect",
        "//third_party:jspecify_annotations",
    ],
)
