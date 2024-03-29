load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")
load("//testing/build_defs:junit_test_suites.bzl", "junit_test_suites")

package(
    default_applicable_licenses = ["//:j2cl_license"],
    licenses = ["notice"],
)

kt_jvm_library(
    name = "lib",
    testonly = 1,
    srcs = [
        "SourceTest.kt",
    ],
    deps = [
        "//third_party:junit",
        "//transpiler/java/com/google/j2cl/transpiler/backend/kotlin/source",
        "//transpiler/java/com/google/j2cl/transpiler/backend/kotlin/source/testing",
    ],
)

junit_test_suites(
    name = "junit",
    sizes = ["small"],
    deps = [":lib"],
)
