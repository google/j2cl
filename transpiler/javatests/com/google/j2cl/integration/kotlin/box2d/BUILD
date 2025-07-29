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
    srcs = ["Box2d.kt"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    deps = ["//samples/box2d/src/main/kotlin/auto_converted:box2d_library"],
)

integration_library(
    name = "box2d_idiomatic",
    srcs = ["Box2dIdiomatic.kt"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    deps = ["//samples/box2d/src/main/kotlin/idiomatic:box2d_library"],
)
