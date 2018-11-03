package(default_visibility = ["//transpiler/javatests/com/google/j2cl/transpiler/integration:__subpackages__"])

# Apache2
licenses(["notice"])

load("//build_defs:rules.bzl", "j2cl_library")

gwt_module(
    name = "box2d_gwt",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/collect:collect-gwt",
        "//third_party/java/jbox2d:jbox2d_gwt",
    ],
)

j2cl_library(
    name = "box2d_j2cl",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party/java/gwt:gwt-jsinterop-annotations-j2cl",
        "//third_party/java/jbox2d:jbox2d_j2cl",
    ],
)
