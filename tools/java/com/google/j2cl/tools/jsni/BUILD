# Description:
#   Jsni to native js file converter tool code.

package(default_visibility = [
    "//third_party/java/j2cl:__subpackages__",
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
        "//third_party:args4j",
        "//third_party:guava",
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/ast",
        "//transpiler/java/com/google/j2cl/frontend",
        "//transpiler/java/com/google/j2cl/problems",
    ],
)
