package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "sourcemap",
    srcs = glob(["*.java"]),
    deps = [
        "//transpiler/java/com/google/j2cl/ast/processors:visitor-codegen",
        "//third_party/java_src/jscomp/java/com/google/debugging/sourcemap:shared",
    ],
)
