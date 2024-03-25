load("//build_defs:rules.bzl", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2wasm_library(
    name = "staticjsmethods-j2wasm",
    srcs = glob(
        [
            "*.java",
            "*.js",
        ],
        exclude = [
            # .native.js not supported in Wasm.
            "Main.native.js",
        ],
    ),
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "staticjsmethods",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
)
