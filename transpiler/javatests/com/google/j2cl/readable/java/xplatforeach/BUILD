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
    srcs = glob([
        "fakes/**/*.java",
        "fakes/**/*.js",
    ]),
    tags = ["no_grok"],
    visibility = [
        "//transpiler/javatests/com/google/j2cl/readable/kotlin/xplatforeach:__pkg__",
    ],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

readable_example(
    srcs = glob(["*.java"]),
    generate_kt_readables = False,
    deps = [":fakes"],
)
