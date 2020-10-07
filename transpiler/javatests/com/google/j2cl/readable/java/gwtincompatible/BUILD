load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)
load("//build_defs:rules.bzl", "j2cl_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_library(
    name = "annotation",
    srcs = ["GwtIncompatible.java"],
)

j2cl_library(
    name = "interface",
    srcs = [
        "Interface.java",
    ],
    deps = [":annotation"],
)

# Creates verification target
readable_example(
    srcs = [
        "Implementor.java",
    ],
    deps = [
        ":annotation",
        ":interface",
    ],
)
