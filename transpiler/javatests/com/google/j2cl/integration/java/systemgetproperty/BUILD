load("@rules_closure//closure:defs.bzl", "closure_js_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

closure_js_library(
    name = "zoo",
    srcs = ["Zoo.js"],
    deps = ["//:jre"],
)

alias(
    name = "zoo-j2cl",
    actual = ":zoo",
)

alias(
    name = "zoo-j2wasm",
    actual = ":zoo",
)

integration_test(
    name = "systemgetproperty",
    srcs = glob(
        [
            "*.java",
            "*.js",
        ],
        exclude = ["Zoo.js"],
    ),
    closure_defines = {
        "zoo": "buzz",
    },
    # This test is specific to J2CL's implementation of System.getProperty().
    enable_jvm_test = False,
    enable_kt = False,
    deps = [":zoo"],
)
