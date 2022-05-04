load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "arithmeticexception",
    srcs = glob(["*.java"]),
    closure_defines = {"jre.checks.numeric": "'ENABLED'"},
    enable_kt = True,
)
