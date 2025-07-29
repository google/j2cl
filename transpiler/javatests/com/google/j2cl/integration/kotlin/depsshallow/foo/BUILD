load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_library(
    name = "foo",
    srcs = ["Foo.kt"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/kotlin/depsshallow:__subpackages__"],
)
