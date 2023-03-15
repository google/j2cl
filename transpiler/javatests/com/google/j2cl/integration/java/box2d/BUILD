load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

integration_library(
    name = "box2d",
    srcs = glob(["*.java"]),
    visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    deps = ["//samples/box2d/src/main/java:box2d_library"],
)
