# Description:
#   This package contains frontend that uses Eclispe/JDT to parse, typecheck and resolve Java code.

package(default_visibility = [
    "//tools/java/com/google/j2cl/tools/gwtincompatible:__pkg__",
    "//transpiler/java/com/google/j2cl/frontend:__subpackages__",
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
        "//transpiler/java/com/google/j2cl/frontend/common",
    ],
)
