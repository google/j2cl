package(default_visibility = [
    "//:__subpackages__",
    "//third_party/java/j2cl:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "transpiler_lib",
    srcs = ["J2clTranspiler.java"],
    deps = [
        "//third_party:guava",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/ast/visitors",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/generator",
        "//transpiler/java/com/google/j2cl/problems",
    ],
)
