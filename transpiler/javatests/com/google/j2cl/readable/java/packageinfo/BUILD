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
    name = "thirdpartylib",
    srcs = glob(["thirdpartylib/*.java"]),
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

readable_example(
    srcs = glob([
        "*.java",
        "subpackage/*.java",
    ]),
    deps = [
        ":thirdpartylib",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
