package(default_visibility = [
    "//tools:__subpackages__",
    "//transpiler:__subpackages__",
])

licenses(["notice"])  # Apache 2.0

java_library(
    name = "jdt",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:gwt-jsinterop-annotations",
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
    ],
)
