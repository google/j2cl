load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "nestednativetype",
    srcs = glob(["*.java"]),
    enable_wasm = False,  # TODO(b/264466634) Enable when imports are generated.
)
