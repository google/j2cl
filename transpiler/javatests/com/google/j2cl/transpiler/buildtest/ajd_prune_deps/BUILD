# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library")

js_library(
    name = "A-native",
    srcs = [
        "a-native.js",
    ],
    deps = [
        "//javascript/closure:base",
    ],
)

j2cl_library(
    name = "A",
    srcs = glob([
        "A*.java",
    ]),
    _js_deps = [
        ":A-native",
    ],
    js_deps_mgmt = "closure",
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

j2cl_library(
    name = "B",
    srcs = [
        "B.java",
    ],
    js_deps_mgmt = "closure",
    deps = [
        ":A",
    ],
)

j2cl_library(
    name = "C",
    srcs = [
        "C.java",
    ],
    js_deps_mgmt = "closure",
    deps = [
        ":B",
    ],
)

js_library(
    name = "ajd",
    deps_mgmt = "closure",
    exports = [
        ":C",
    ],
)
