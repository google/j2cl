load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)
load("//build_defs:rules.bzl", "j2wasm_library")

package(
    default_visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    licenses = ["notice"],
)

j2wasm_library(
    name = "autovalue-j2wasm",
    srcs = glob(
        ["*.java"],
        exclude = ["JsArray.java"],
    ) + glob(["super-wasm/*.java"]),
    deps = [
        "//third_party:auto_value-j2wasm",
        "//transpiler/javatests/com/google/j2cl/integration/testing:testing-j2wasm",
    ],
)

java_library(
    name = "autovalue",
    srcs = glob(["*.java"]),
    tags = ["manual"],
    deps = [
        "//third_party:auto_value",
        "//third_party:gwt-jsinterop-annotations",
        "//transpiler/javatests/com/google/j2cl/integration/testing",
    ],
)

integration_test(
    name = "autovalue-j2cl",
    srcs = glob(["*.java"]),
    closure_defines = {"jre.classMetadata": "'STRIPPED'"},
    enable_kt = False,
    optimize_autovalue = True,
    deps = [
        "//third_party:auto_value-j2cl",
    ],
)
