package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

load(
    "//:transpiler/javatests/com/google/j2cl/transpiler/readable/readable_example.bzl",
    "readable_example",
)

# Creates verification target
readable_example(
    name = "jsmemberinnativetype",
    srcs = glob(["*.java"]),
    js_deps = [":native_types"],
    deps = ["//third_party/java/gwt:gwt-jsinterop-annotations-j2cl"],
)

js_library(
    name = "native_types",
    srcs = glob(["native_sources/*.js"]),
    deps_mgmt = "legacy",
    deps = ["//javascript/closure:base"],
)
