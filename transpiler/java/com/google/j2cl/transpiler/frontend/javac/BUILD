load("@rules_java//java:defs.bzl", "java_library")

# Description:
#   This package contains frontend code that uses OpenJDK/javac to parse, typecheck and resolve
#   Java code.

package(
    default_applicable_licenses = ["//:j2cl_license"],
    default_visibility = [
        "//transpiler/java/com/google/j2cl/transpiler/frontend:__subpackages__",
    ],
)

licenses(["notice"])

java_library(
    name = "javac",
    srcs = glob(["*.java"]),
    add_exports = [
        "jdk.compiler/com.sun.source.tree",
        "jdk.compiler/com.sun.tools.javac.api",
        "jdk.compiler/com.sun.tools.javac.code",
        "jdk.compiler/com.sun.tools.javac.file",
        "jdk.compiler/com.sun.tools.javac.model",
        "jdk.compiler/com.sun.tools.javac.tree",
        "jdk.compiler/com.sun.tools.javac.util",
    ],
    javacopts = ["-Xep:BetaApi:OFF"],
    deps = [
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsinterop-annotations",
        "//third_party:jsr305_annotations",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/frontend/common",
    ],
)
