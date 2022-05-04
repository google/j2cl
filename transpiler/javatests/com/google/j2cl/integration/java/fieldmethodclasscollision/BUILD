load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "fieldmethodclasscollision",
    srcs = glob(["*.java"]),
    # TODO(b/219914876): Name collision
    enable_kt = False,
)
