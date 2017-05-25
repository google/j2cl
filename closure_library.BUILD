package(default_visibility = ["//visibility:public"])

licenses(["notice"]) # Apache2

load("@io_bazel_rules_closure//closure:defs.bzl", "closure_js_library")

closure_js_library(
    name = "base",
    srcs = [
        "closure/goog/base.js",
        "closure/goog/deps.js",
    ],
)

closure_js_library(
    name = "math_long",
    srcs = ["closure/goog/math/long.js"],
    deps = [
        ":asserts",
        ":reflect",
        ":base",
    ],
)

closure_js_library(
    name = "reflect",
    srcs = ["closure/goog/reflect/reflect.js"],
    deps = [":base"],
)

closure_js_library(
    name = "asserts",
    srcs = ["closure/goog/asserts/asserts.js"],
    deps = [
        ":debug_error",
        ":dom_nodetype",
        ":string",
        ":base",
    ],
)

closure_js_library(
    name = "debug_error",
    srcs = ["closure/goog/debug/error.js"],
    deps = [":base"],
)

closure_js_library(
    name = "dom_nodetype",
    srcs = ["closure/goog/dom/nodetype.js"],
    deps = [":base"],
)

closure_js_library(
    name = "string",
    srcs = ["closure/goog/string/string.js"],
    deps = [":base"],
)