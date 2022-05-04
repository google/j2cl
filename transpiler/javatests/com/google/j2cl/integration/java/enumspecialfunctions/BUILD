load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "enumspecialfunctions",
    srcs = glob(["*.java"]),
    # TODO(b/219913874): java.lang.Enum to kotlin.Enum translation
    enable_kt = False,
)
