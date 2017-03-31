package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

filegroup(
    name = "foo_externs",
    srcs = ["foo_externs.js"],
)

js_library(
    name = "_externs",
    srcs = [
        "//javascript/externs:common",
        ":foo_externs",
    ],
    deps_mgmt = "legacy",
)

# Creates verification target
readable_example(
    name = "externs",
    srcs = glob(["*.java"]),
    native_srcs = ["Main.native.js"],
    native_srcs_pkg = "CONVENTION",
    test_externs_list = [
        ":_externs",
    ],
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
    ],
)
