# Description:
#   This package contains frontend code that uses OpenJDK/javac to parse, typecheck and resolve
#   Java code.

package(default_visibility = [
    "//transpiler/java/com/google/j2cl/frontend:__subpackages__",
])

licenses(["notice"])  # Apache 2.0

java_library(
    name = "javac",
    srcs = glob(["*.java"]),
    deps = [
        "//third_party:javac",
    ],
)
