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
    name = "lib",
    srcs = ["EnumWithOverriddenMethods.java"],
)

integration_test(
    name = "enums",
    srcs = glob(
        ["*.java"],
        exclude = ["EnumWithOverriddenMethods.java"],
    ),
    deps = [":lib"],
)
