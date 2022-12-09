load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "depsshallow",
    srcs = ["Main.java"],
    deps = ["//transpiler/javatests/com/google/j2cl/integration/java/depsshallow/foo"],
)
