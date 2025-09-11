load(
    "//build_defs:rules.bzl",
    "j2cl_library",
)
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

j2cl_library(
    name = "third_party_lib-j2cl",
    srcs = ["DefaultParamInterface.kt"],
)

readable_example(
    srcs = glob(
        ["*.kt"],
        exclude = ["DefaultParamInterface.kt"],
    ),
    deps = [":third_party_lib-j2cl"],
)
