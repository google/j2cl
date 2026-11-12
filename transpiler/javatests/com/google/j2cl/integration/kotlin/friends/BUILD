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
    name = "friend",
    srcs = ["Friend.kt"],
)

integration_test(
    srcs = ["Main.kt"],
    deps = [
        ":friend",
        "//transpiler/javatests/com/google/j2cl/integration/kotlin/friends/library",
    ],
)
