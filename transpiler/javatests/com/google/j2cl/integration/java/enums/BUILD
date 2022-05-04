load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "enums",
    srcs = glob(["*.java"]),
    # TODO(b/227554489): java.util.function.* is still missing
    enable_kt_native = False,
)
