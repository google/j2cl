package(default_visibility = ["//transpiler:__subpackages__"])

# Apache2
licenses(["notice"])

java_library(
    name = "annotations",
    exported_plugins = ["//transpiler/java/com/google/j2cl/ast/processors:visitor-apt-processor"],
    exports = [":annotations-internal"],
)

java_library(
    name = "annotations-internal",
    srcs = glob(["*.java"]),
)

load("//build_defs:rules.bzl", "j2cl_import")

j2cl_import(
    name = "annotations-j2cl",
    jar = ":annotations",
)
