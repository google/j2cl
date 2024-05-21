load("//build_defs:rules.bzl", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

# Provide a custom j2wasm_library so no .js files passed to target.
j2wasm_library(
    name = "wasm-j2wasm",
    srcs = glob(["super-wasm/*.java"]),
    javacopts = ["-XepDisableAllChecks"],
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "wasm",
    srcs = glob(["*.java"]),
    # Contains features which are only applicable for Wasm.
    enable_jvm_test = False,
    enable_kt = False,
    # TODO(b/325056024): jsenums are not yet supported in the modular pipeline
    use_modular_pipeline = False,
)
