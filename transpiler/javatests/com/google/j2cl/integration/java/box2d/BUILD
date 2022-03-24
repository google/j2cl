load("//build_defs:rules.bzl", "j2cl_library")

package(
    default_visibility = ["//transpiler/javatests/com/google/j2cl/integration:__subpackages__"],
    licenses = ["notice"],  # Apache 2.0
)

j2cl_library(
    name = "box2d_j2cl",
    srcs = glob(["*.java"]),
    tags = ["manual"],
    deps = ["//third_party:jbox2d-j2cl"],
)
