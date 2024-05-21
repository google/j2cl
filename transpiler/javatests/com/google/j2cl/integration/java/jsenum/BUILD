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
    name = "jsenum-j2wasm",
    srcs = glob(
        [
            "*.java",
            "super-wasm/*.java",
        ],
        exclude = [
            "NativeEnums.java",
        ],
    ),
    javacopts = ["-XepDisableAllChecks"],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "jsenum",
    srcs = glob([
        "*.java",
        "*.js",
    ]),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt = False,
    # TODO(b/325056024): jsenums are not yet supported in the modular pipeline
    use_modular_pipeline = False,
)
