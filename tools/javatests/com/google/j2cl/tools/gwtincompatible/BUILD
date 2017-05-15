package(
    default_testonly = 1,
)

# Google owns the copyright
licenses(["unencumbered"])

load("//java/com/google/testing/builddefs:GenTestRules.bzl", "GenTestRules")

java_library(
    name = "frontend",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/eclipse:eclipse-jdt-core-4.5",
        "//third_party/java/junit",
        "//tools/java/com/google/j2cl/tools/gwtincompatible",
    ],
)

GenTestRules(
    name = "GeneratedTestRules",
    test_files = glob(["*.java"]),
    deps = [":frontend"],
)
