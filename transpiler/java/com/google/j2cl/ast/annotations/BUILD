package(default_visibility = ["//:__subpackages__"])

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
