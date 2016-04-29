package(default_visibility = [
    "//third_party/java/j2cl:__pkg__",
    "//:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "transpiler",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//third_party/java/eclipse:eclipse-jdt-core",
        "//third_party/java/jakarta_velocity",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/ast/visitors",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/errors",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/generator",
        "//transpiler/java/com/google/j2cl/generator/visitors",
    ],
)
