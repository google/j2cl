load(
    "//transpiler/javatests/com/google/j2cl/transpiler/readable:readable_example.bzl",
    "readable_example",
)
load("//build_defs:rules.bzl", "j2cl_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

j2cl_library(
    name = "interface",
    srcs = [
        "Interface.java",
    ],
    deps = [
        "//third_party:guava-j2cl",
    ],
)

# Creates verification target
readable_example(
    srcs = [
        "Implementor.java",
    ],
    deps = [
        ":interface",
        "//third_party:guava-j2cl",
    ],
)
