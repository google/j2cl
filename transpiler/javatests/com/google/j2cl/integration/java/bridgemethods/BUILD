load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "bridgemethods",
    srcs = glob(["*.java"]),
    enable_kt = False,
    suppress = ["JSC_TYPE_MISMATCH"],
)
