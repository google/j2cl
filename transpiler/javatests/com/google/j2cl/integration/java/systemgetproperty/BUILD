load("@rules_closure//closure:defs.bzl", "closure_js_library")
load("//build_defs:rules.bzl", "j2wasm_library")
load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
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

j2wasm_library(
    name = "systemgetproperty-j2wasm",
    srcs = glob(
        [
            "*.java",
            "*.js",
        ],
        exclude = [
            "Zoo.js",
            # .native.js files are not supported in J2Wasm.
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
