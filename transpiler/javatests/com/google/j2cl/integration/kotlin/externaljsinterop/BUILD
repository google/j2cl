load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_library(
    name = "external_library",
    srcs = glob(
        ["*.kt"],
        exclude = ["Main.kt"],
    ),
)

integration_test(
    name = "externaljsinterop",
    srcs = ["Main.kt"],
    deps = [":external_library"],
)
