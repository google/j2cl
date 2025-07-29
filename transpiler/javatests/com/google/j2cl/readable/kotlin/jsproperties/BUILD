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
    name = "javafoo",
    srcs = ["JavaFoo.java"],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

j2cl_library(
    name = "otherlib",
    srcs = ["Other.kt"],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

readable_example(
    srcs = glob(
        [
            "*.kt",
        ],
        exclude = ["Other.kt"],
    ),
    deps = [
        ":javafoo",
        ":otherlib",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
