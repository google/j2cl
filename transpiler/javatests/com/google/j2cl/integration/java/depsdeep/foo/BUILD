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
    srcs = ["Foo.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep:__subpackages__"],
    deps = ["//transpiler/javatests/com/google/j2cl/integration/java/depsdeep/bar"],
)
