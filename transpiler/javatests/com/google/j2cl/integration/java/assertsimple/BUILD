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
    # TODO(b/256681746): Improve TestUtils to detect Kotlin and fix the test.
    enable_kt_native = False,
    # TODO(b/203692803): Enable once the bug is fixed.
    enable_wasm = False,
    tags = ["manual"],
)
