load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")
load("//testing/build_defs:junit_test_suites.bzl", "junit_test_suites")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "tests",
    testonly = 1,
    srcs = [
        "CharEscaperTest.kt",
        "NamesTest.kt",
    ],
    deps = [
        "//third_party:junit",
        "//third_party:truth",
        "//transpiler/java/com/google/j2cl/transpiler/backend/kotlin",
    ],
)

junit_test_suites(
    name = "junit",
    sizes = ["small"],
    deps = [":tests"],
)
