package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "generator",
    srcs = glob(["*.java"]),
    resources = ["jsmodule.vm"],
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//third_party/java/jakarta_velocity",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/errors",
    ],
)
