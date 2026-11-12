load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

integration_library(
    name = "nonfriend",
    srcs = ["NonFriend.kt"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/kotlin/friends:__subpackages__"],
)
