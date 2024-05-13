load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "fakes-j2cl",
    srcs = glob(["fakes/**/*.java"]),
    tags = ["no_grok"],
    visibility = [
        "//transpiler/javatests/com/google/j2cl/integration/kotlin/xplatforeach:__pkg__",
    ],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

integration_test(
    name = "xplatforeach",
    srcs = glob([
        "*.java",
        "*.native.js",
    ]),
    # Test JS specific logic.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    deps = [
        ":fakes",
    ],
)
