package(default_visibility = [
    "//third_party/java/j2cl:__pkg__",
    "//:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "rerunning_transpiler",
    srcs = ["RerunningJ2clTranspiler.java"],
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/hash",
        "//java/com/google/common/io",
        "//transpiler/java/com/google/j2cl/errors",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/transpiler",
    ],
)
