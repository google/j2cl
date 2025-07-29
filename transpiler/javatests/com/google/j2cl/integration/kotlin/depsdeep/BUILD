load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_test(
    name = "depsdeep",
    srcs = ["Main.kt"],
    deps = [
        "//transpiler/javatests/com/google/j2cl/integration/kotlin/depsdeep/bar",
        "//transpiler/javatests/com/google/j2cl/integration/kotlin/depsdeep/foo",
    ],
)
