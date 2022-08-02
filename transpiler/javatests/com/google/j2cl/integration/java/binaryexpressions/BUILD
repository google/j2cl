load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "binaryexpressions",
    srcs = glob(["*.java"]),
    # TODO(b/240658614): NPE when running locally. Needs debugging.
    enable_kt_native = False,
)
