load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library")

j2cl_library(
    name = "A",
    srcs = [
        "A.java",
        "S.java",
    ],
)

j2cl_library(
    name = "B",
    srcs = [
        "B.java",
    ],
    deps = [
        ":A",
    ],
)

j2cl_library(
    name = "C",
    srcs = [
        "C.java",
    ],
    deps = [
        ":B",
    ],
)

j2cl_library(
    name = "example",
    srcs = [
        "Main.java",
    ],
    deps = [
        ":C",
    ],
)

closure_js_library(
    name = "ajd",
    deps_mgmt = "closure",
    exports = [
        ":example",
    ],
)
