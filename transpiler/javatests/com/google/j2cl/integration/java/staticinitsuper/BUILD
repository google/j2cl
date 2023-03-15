load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "staticinitsuper",
    srcs = glob(["*.java"]),
    # TODO(b/256681746): Improve TestUtils to detect Kotlin and fix the test.
    enable_kt_native = False,
    tags = ["manual"],
)
