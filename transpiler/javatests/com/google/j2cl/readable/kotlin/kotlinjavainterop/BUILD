load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "java",
    srcs = glob(
        ["*.java"],
        exclude = ["JavaIntSetExtendingIntermediateKotlinIntSet.java"],
    ),
)

j2cl_library(
    name = "intermediate_kotlin",
    srcs = [
        "IntermediateKotlinIntSet.kt",
    ],
    deps = [":java"],
)

j2cl_library(
    name = "intermediate_java",
    srcs = [
        "JavaIntSetExtendingIntermediateKotlinIntSet.java",
    ],
    deps = [":intermediate_kotlin"],
)

readable_example(
    srcs = glob(
        ["*.kt"],
        exclude = ["IntermediateKotlinIntSet.kt"],
    ),
    deps = [
        ":intermediate_java",
        ":java",
    ],
)
