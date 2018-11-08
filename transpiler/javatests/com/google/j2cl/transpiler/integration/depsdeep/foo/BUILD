licenses(["notice"])  # Apache 2.0

load("//build_defs:rules.bzl", "j2cl_library")

j2cl_library(
    name = "foo",
    srcs = ["Foo.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/depsdeep:__subpackages__"],
    deps = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/depsdeep/bar"],
)
