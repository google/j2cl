load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "emptyclass",
    srcs = glob(["*.java"]),
    enable_bundle_size_report = True,
    enable_kt_native = True,
)
