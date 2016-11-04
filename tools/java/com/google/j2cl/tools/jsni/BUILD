# Description:
#   Jsni to native js file converter tool code.

package(default_visibility = [
    "//third_party/java/j2cl:__pkg__",
    "//:__subpackages__",
])

# Google owns the copyright
licenses(["unencumbered"])

# Libraries
# =========================================================

java_library(
    name = "jsni",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/flags",
        "//java/com/google/common/io",
        "//:jdt-core-stable",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/generator",
        "//transpiler/java/com/google/j2cl/problems",
    ],
)
