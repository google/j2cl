load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "npecatch",
    srcs = glob(["*.java"]),
    # TODO(b/183769034): Enable once the wasm traps vs exceptions is solved.
    enable_wasm = False,
)
