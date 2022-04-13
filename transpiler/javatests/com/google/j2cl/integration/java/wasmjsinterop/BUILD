load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    licenses = ["notice"],  # Apache 2.0
)

integration_test(
    name = "wasmjsinterop",
    srcs = glob(["*.java"]),
    enable_kt = False,
)
