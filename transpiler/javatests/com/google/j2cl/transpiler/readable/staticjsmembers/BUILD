# Apache2
licenses(["notice"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    srcs = glob(["*.java"]),
    js_deps = [":native_sources"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)

js_library(
    name = "native_sources",
    srcs = glob(["native_sources/*.js"]),
    deps_mgmt = "legacy",
    deps = ["//javascript/closure:base"],
)
