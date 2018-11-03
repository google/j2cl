load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library")

closure_js_library(
    name = "nativejs",
    srcs = [
        "native1.js",
        "native2.js",
        "native3.js",
        "native4.js",
        "native5.js",
    ],
    deps = [
        "//javascript/closure:base",
    ],
)

j2cl_library(
    name = "native",
    srcs = [
        "NativeType1.java",
        "NativeType2.java",
        "NativeType3.java",
        "NativeType4.java",
        "NativeType5.java",
    ],
    _js_deps = [
        ":nativejs",
    ],
    js_deps_mgmt = "closure",
    deps = [
        "//third_party:gwt-jsinterop-annotations-j2cl",
    ],
)

j2cl_library(
    name = "A",
    srcs = [
        "A1.java",
        "A2.java",
        "A3.java",
        "A4.java",
        "A5.java",
        "Container.java",
        "Marker.java",
    ],
    js_deps_mgmt = "closure",
    deps = [
        ":native",
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

closure_js_library(
    name = "ajd",
    deps_mgmt = "closure",
    exports = [
        ":B",
    ],
)
