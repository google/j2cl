package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "transpiler",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/io",
        "//third_party/java/eclipse:eclipse-jdt-core-4.4",
        "//third_party/java/jakarta_velocity",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/errors",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/generator",
    ],
)
