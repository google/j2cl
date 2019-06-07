package(
    default_visibility = ["//junit/generator/javatests/com/google/j2cl/junit/integration:__subpackages__"],
    licenses = ["notice"],  # Apache 2.0
)

load("//build_defs:rules.bzl", "j2cl_library")

java_library(
    name = "testlogger",
    srcs = glob(["*.java"]),
)

j2cl_library(
    name = "testlogger-j2cl",
    srcs = glob(["super/*.java"]),
    _js_deps = ["//third_party:closure_testcase"],
    deps = ["//third_party:gwt-jsinterop-annotations-j2cl"],
)
