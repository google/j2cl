load(
    "//transpiler/javatests/com/google/j2cl/integration:integration_test.bzl",
    "integration_library",
)

package(licenses = ["notice"])

integration_library(
    name = "box2d",
    srcs = glob(["*.java"]),
    visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    deps = ["//third_party:jbox2d"],
)
