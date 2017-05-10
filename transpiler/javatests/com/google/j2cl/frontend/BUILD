# Google owns the copyright
licenses(["unencumbered"])

package(
    default_testonly = 1,
)

load("//java/com/google/testing/builddefs:GenTestRules.bzl", "GenTestRules")

java_library(
    name = "frontend",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/junit",
        "//third_party:jdt-core",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)

GenTestRules(
    name = "GeneratedTestRules",
    test_files = glob(["*.java"]),
    deps = [":frontend"],
)
