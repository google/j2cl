load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "loggingstripped",
    srcs = glob(["*.java"]),
    # TODO(b/227424033): Reenable once the JRE logging APIs are available.
    enable_kt_native = False,
)
