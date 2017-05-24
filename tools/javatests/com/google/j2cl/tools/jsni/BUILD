# Description:
#   Jsni to js file converter tests.
#

package(default_visibility = ["//:__subpackages__"])

# Google owns the copyright
licenses(["unencumbered"])

java_test(
    name = "JsniConverterTestSuite",
    data = glob(["testdata/*.java"]),
    runtime_deps = [":jsniConverterTestLib"],
)

java_library(
    name = "jsniConverterTestLib",
    testonly = 1,
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/devtools/build/runtime:runfiles",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "//third_party:args4j",
        "//tools/java/com/google/j2cl/tools/jsni",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)
