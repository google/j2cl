load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)
load("//build_defs:rules.bzl", "j2cl_library")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
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

readable_example(
    srcs = [
        "Implementor.java",
    ],
    deps = [
        ":annotation",
        ":interface",
    ],
)
