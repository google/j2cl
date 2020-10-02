# Description:
#   This package contains frontend code that uses OpenJDK/javac to parse, typecheck and resolve
#   Java code.

package(default_visibility = [
    "//transpiler/java/com/google/j2cl/transpiler/frontend:__subpackages__",
])

licenses(["notice"])

java_library(
    name = "javac",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:gwt-jsinterop-annotations",
        "//third_party:javac",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
    ],
)
