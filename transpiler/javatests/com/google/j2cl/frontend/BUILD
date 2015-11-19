# Google owns the copyright
licenses(["unencumbered"])

package(
    default_testonly = 1,
)

load("/java/com/google/testing/builddefs/GenTestRules", "GenTestRules")

java_library(
    name = "frontend",
    srcs = glob(["*.java"]),
    deps = [
        "//java/com/google/common/base",
        "//java/com/google/common/collect",
        "//third_party/java/eclipse:eclipse-jdt-core",
        "//third_party/java/junit",
        "//transpiler/java/com/google/j2cl/frontend",
    ],
)

GenTestRules(
    name = "GeneratedTestRules",
    test_files = glob(["*.java"]),
    deps = [":frontend"],
)
