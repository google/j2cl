load("//testing/build_defs:junit_test_suites.bzl", "junit_test_suites")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(
    licenses = ["notice"],  # Apache 2.0
)

kt_jvm_library(
    name = "tests",
    testonly = 1,
    srcs = glob([
        "*.kt",
        "*.java",
    ]),
    deps = [
        "//third_party/java/junit",
        "//third_party/kotlin/kotlin:kotlin_test",
        "//transpiler/java/com/google/j2cl/common",
        "//transpiler/java/com/google/j2cl/transpiler/ast",
        "//transpiler/java/com/google/j2cl/transpiler/backend/common",
        "//transpiler/java/com/google/j2cl/transpiler/backend/kotlin",
    ],
)

junit_test_suites(
    name = "junit_tests",
    sizes = ["small"],
    deps = [":tests"],
)
