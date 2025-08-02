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
)

# Make sure we back off for the old XplatLogger implementation until refactoring is complete.
# TODO(435512074): Remove after XplatLogger refactoring is complete.
readable_example(
    srcs = glob(["*.java"]),
    generate_kt_readables = False,
    deps = [":fakes"],
)
