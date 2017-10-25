package(default_visibility = ["//transpiler:__subpackages__"])

# Apache2
licenses(["notice"])

java_library(
    name = "visitors",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
    ],
)
