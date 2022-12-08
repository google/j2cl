load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "compiletimeconstant",
    srcs = glob(["*.java"]),
    # TODO(b/256681746): Improve TestUtils to detect Kotlin and fix the test.
    enable_kt_native = False,
)
