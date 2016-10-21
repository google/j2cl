package(default_visibility = [
    "//:__subpackages__",
    "//third_party/java/j2cl:__pkg__",
])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "transpiler_lib",
    srcs = ["J2clTranspiler.java"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//third_party/java/eclipse:eclipse-jdt-core-4.5",
        "//third_party/java/jakarta_velocity",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/ast/visitors",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/errors",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/generator",
    ],
)
