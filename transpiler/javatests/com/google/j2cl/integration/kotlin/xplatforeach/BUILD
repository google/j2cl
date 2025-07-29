load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "xplatforeach",
    srcs = glob([
        "*.kt",
        "*.native.js",
    ]),
    # Test JS specific logic.
    enable_jvm_test = False,
    enable_kt = False,
    enable_wasm = False,
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/java/xplatforeach:fakes",
    ],
)
