load("//build_defs:rules.bzl", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    licenses = ["notice"],
)

integration_library(
    name = "dependency",
    srcs = ["AutoValueDependency.java"],
    deps = [
        "//third_party:auto_value",
    ],
)

j2wasm_library(
    name = "autovalue-j2wasm",
    srcs = glob(
        ["*.java"],
        exclude = [
            "AutoValueDependency.java",
            "JsArray.java",
        ],
    ) + glob(["super-wasm/*.java"]),
    deps = [
        ":dependency-j2wasm",
        "//jre/java:javaemul_internal_annotations-j2wasm",
        "//third_party:auto_value-j2wasm",
        "//third_party:jsinterop-annotations-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

# TODO(b/264934554): Port this test to Kotlin when AutoValue is supported.
integration_test(
    srcs = glob(
        ["*.java"],
        exclude = ["AutoValueDependency.java"],
    ),
    closure_defines = {"jre.classMetadata": "'STRIPPED'"},
    deps = [
        ":dependency",
        "//third_party:auto_value",
    ],
)
