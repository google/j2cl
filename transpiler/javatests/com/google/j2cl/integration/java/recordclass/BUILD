load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
    "integration_test",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

integration_library(
    name = "record-in-other-library",
    srcs = ["RecordInOtherLibrary.java"],
)

integration_test(
    srcs = ["Main.java"],
    deps = [":record-in-other-library"],
)
