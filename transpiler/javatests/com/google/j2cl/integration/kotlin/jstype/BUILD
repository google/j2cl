load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

alias(
    name = "closure_reflect-j2cl",
    actual = "//third_party:closure_reflect",
)

alias(
    name = "closure_reflect-j2wasm",
    actual = "//third_party:closure_reflect",
)

integration_library(
    name = "jstype_from_other_library",
    srcs = ["JsTypeFromOtherLibrary.kt"],
    enable_jvm = False,
    enable_kt = False,
    enable_kt_native = False,
    enable_wasm = False,
)

integration_test(
    srcs = glob(
        [
            "*.kt",
            "*.js",
        ],
        exclude = ["JsTypeFromOtherLibrary.kt"],
    ),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    deps = [
        ":closure_reflect",
        ":jstype_from_other_library",
    ],
)
