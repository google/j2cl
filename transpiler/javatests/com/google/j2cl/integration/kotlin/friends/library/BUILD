load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["reciprocal"],
)

integration_library(
    name = "friend",
    srcs = ["LibraryFriend.kt"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/kotlin/friends:__subpackages__"],
)

integration_library(
    name = "library",
    srcs = ["Library.kt"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/kotlin/friends:__subpackages__"],
    deps = [
        ":friend",
        "//transpiler/javatests/com/google/j2cl/integration/kotlin/friends/nonfriend",
    ],
)
