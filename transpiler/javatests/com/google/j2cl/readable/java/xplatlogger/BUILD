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
    name = "fakes",
    srcs = glob(["fakes/**/*.java"]),
    tags = ["no_grok"],
    visibility = [
        "//transpiler/javatests/com/google/j2cl/readable/kotlin/xplatlogger:__pkg__",
    ],
)

readable_example(
    srcs = glob(["*.java"]),
    generate_kt_readables = False,
    deps = [":fakes"],
)
