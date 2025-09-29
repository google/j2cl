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
    name = "promise-j2cl",
    srcs = ["Promise.java"],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

integration_test(
    srcs = glob(
        ["*.java"],
        exclude = ["Promise.java"],
    ),
    # Contains JsInterop features which are only applicable for JS output.
    enable_jvm_test = False,
    enable_kt_native = False,
    enable_wasm = False,
    deps = [":promise"],
)
