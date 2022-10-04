load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(licenses = ["notice"])

integration_test(
    name = "depsdeep",
    srcs = ["Main.java"],
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/java/depsdeep/bar:bar-j2cl",
        "//transpiler/javatests/com/google/j2cl/integration/java/depsdeep/foo:foo-j2cl",
    ],
)
