load("//build_defs:rules.bzl", "j2cl_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_library(
    name = "bar",
    srcs = ["Bar.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/integration/depsdeep:__subpackages__"],
)
