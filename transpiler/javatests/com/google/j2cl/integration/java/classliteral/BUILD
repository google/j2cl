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
    name = "classliteral-j2wasm",
    srcs = glob(
        ["*.java"],
        exclude = ["TypeExtendsNativeClass.java"],
    ) + glob(["super-wasm/*.java"]),
    deps = [
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
        "//third_party:jspecify_annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

integration_test(
    name = "classliteral",
    srcs = glob(["*.java"]),
    enable_nullability = True,
    deps = ["//third_party:jspecify_annotations"],
)
