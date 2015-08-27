# Description:
#   Jsni to native js file converter tool code.

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

# Libraries
# =========================================================

java_library(
    name = "jsniConverter",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//java/com/google/common/flags",
        "//java/com/google/common/io",
        "//third_party/java/eclipse:eclipse-jdt-core-4.4",
    ],
)
