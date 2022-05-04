load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "staticfields",
    srcs = glob(["*.java"]),
    # TODO(b/227349235): Getting InvalidMutabilityException with old memory model
    enable_kt_native = False,
)
