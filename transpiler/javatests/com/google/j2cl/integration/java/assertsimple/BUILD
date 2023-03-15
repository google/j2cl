load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "assertsimple",
    srcs = glob(["*.java"]),
    defs = [
        # Turn on asserts since it is what this test is testing.
        "--remove_j2cl_asserts=false",
    ],
    # TODO(b/219913874): Translation of toString() from Java to Kotlin
    enable_kt = False,
    # TODO(b/203692803): Enable once the bug is fixed.
    enable_wasm = False,
    tags = ["manual"],
)
