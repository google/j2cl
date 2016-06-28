package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "staticjsmembers",
    srcs = glob(["*.java"]),
    js_deps = [":native_sources"],
    deps = ["//jre/java:gwt-jsinterop-annotations"],
)

js_library(
    name = "native_sources",
    srcs = glob(["native_sources/*.js"]),
    deps = ["//javascript/closure:base"],
)
