package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_library(
    name = "ast",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/auto:auto_value",
        "//third_party/java/eclipse:eclipse-jdt-core",
        "//third_party/java/jsr305_annotations",
        "//transpiler/java/com/google/j2cl/ast/processors:visitor-codegen",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/sourcemaps:tracking_interface",
    ],
)
