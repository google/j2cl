package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "generator",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/annotations",
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/io",
        "//third_party/java/jakarta_commons_lang/v2_6",
        "//third_party/java/jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/ast/sourcemap",
        "//transpiler/java/com/google/j2cl/errors",
        "//transpiler/java/com/google/j2cl/generator/visitors",
        "//third_party/java_src/jscomp/java/com/google/debugging/sourcemap:common",
        "//third_party/java_src/jscomp/java/com/google/debugging/sourcemap:generation",
        "//third_party/java_src/jscomp/java/com/google/debugging/sourcemap:shared",
    ],
)
