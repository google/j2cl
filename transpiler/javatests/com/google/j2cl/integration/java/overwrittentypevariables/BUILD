load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "overwrittentypevariables",
    srcs = glob(["*.java"]),
    # TODO(b/202427872): Fix once Generic bounds supports nullability.
    enable_kt = False,
)
