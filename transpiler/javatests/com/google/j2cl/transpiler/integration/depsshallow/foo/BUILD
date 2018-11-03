# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library")

j2cl_library(
    name = "foo",
    srcs = ["Foo.java"],
    visibility = ["//transpiler/javatests/com/google/j2cl/transpiler/integration/depsshallow:__subpackages__"],
)
