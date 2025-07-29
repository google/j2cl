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
    name = "thirdpartyjavalib",
    srcs = glob(["thirdpartyjavalib/*.java"]),
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

j2cl_library(
    name = "thirdpartylib",
    srcs = [
        "thirdpartylib/OtherKotlinLibrary.kt",
        "thirdpartylib/package-info.java",
    ],
    deps = ["//third_party:jsinterop-annotations-j2cl"],
)

readable_example(
    srcs = [
        "Outer.kt",
        "package-info.java",
        "subpackage/Foo.kt",
    ],
    deps = [
        ":thirdpartyjavalib",
        ":thirdpartylib",
        "//third_party:jsinterop-annotations-j2cl",
    ],
)
