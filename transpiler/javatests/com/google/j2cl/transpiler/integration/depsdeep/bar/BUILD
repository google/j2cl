package(
    licenses = ["notice"],  # Apache 2.0
)

load("//build_defs:rules.bzl", "j2cl_library")

j2cl_library(
    name = "bar",
    srcs = ["Bar.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/depsdeep:__subpackages__"],
)
