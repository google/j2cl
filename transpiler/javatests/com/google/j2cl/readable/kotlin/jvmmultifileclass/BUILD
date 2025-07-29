load("//build_defs:rules.bzl", "j2cl_library")
load(
    "//transpiler/javatests/com/google/j2cl/readable:readable_example.bzl",
    "readable_example",
)

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

OTHER_LIBRARY_SRCS = [
    "otherLibraryX.kt",
    "otherLibraryY.kt",
]

j2cl_library(
    name = "otherLibrary",
    srcs = OTHER_LIBRARY_SRCS,
)

readable_example(
    srcs = glob(
        ["*.kt"],
        exclude = OTHER_LIBRARY_SRCS,
    ),
    deps = [":otherLibrary"],
)
